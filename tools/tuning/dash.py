#!/usr/bin/env python3
"""Talk to FTC Dashboard on the robot from a laptop: read and change settings, record
telemetry, and sum up the shots in a recording. For launch tuning sessions
(tools/tuning/README.md, docs/decisions/0052). Practice only: the dashboard is off at events.

Python's standard library only, so it runs on any laptop with Python 3.8 or newer.

    python3 tools/tuning/dash.py status
    python3 tools/tuning/dash.py get Launcher
    python3 tools/tuning/dash.py set Launcher.launcherKF=0.00021 Launcher.launchFarRpm=3300
    python3 tools/tuning/dash.py record 30
    python3 tools/tuning/dash.py shots tools/tuning/runs/<file>.jsonl
    python3 tools/tuning/dash.py summary tools/tuning/runs/<file>.jsonl

The robot's dashboard is at ws://192.168.43.1:8000 with the laptop on the robot's Wi-Fi.
"""

import argparse
import base64
import datetime
import json
import os
import socket
import struct
import sys
import time

DEFAULT_HOST = "192.168.43.1"
DEFAULT_PORT = 8000
RUNS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "runs")

# What a tuning session may change without --force: the launcher and intake groups, and
# only the aim and wiggle settings in Drive. Nothing that changes how the robot drives.
ALLOWED_GROUPS = ("Launcher", "Intake")
ALLOWED_DRIVE_PREFIXES = ("aim", "wiggle")


# ---- WebSocket, the small part of RFC 6455 a client needs --------------------------------

class WebSocket:
    """A plain WebSocket client: text messages out (masked, as clients must), text in."""

    def __init__(self, host, port, timeout=5.0):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        key = base64.b64encode(os.urandom(16)).decode()
        request = (
            "GET / HTTP/1.1\r\n"
            f"Host: {host}:{port}\r\n"
            "Upgrade: websocket\r\n"
            "Connection: Upgrade\r\n"
            f"Sec-WebSocket-Key: {key}\r\n"
            "Sec-WebSocket-Version: 13\r\n\r\n"
        )
        self.sock.sendall(request.encode())
        reply = b""
        while b"\r\n\r\n" not in reply:
            chunk = self.sock.recv(1024)
            if not chunk:
                raise ConnectionError("the dashboard closed the connection during the handshake")
            reply += chunk
        head, self.buffer = reply.split(b"\r\n\r\n", 1)
        if b" 101 " not in head.split(b"\r\n")[0]:
            raise ConnectionError("not a WebSocket: " + head.split(b"\r\n")[0].decode(errors="replace"))

    def send(self, text):
        payload = text.encode()
        header = bytearray([0x81])  # FIN + text
        n = len(payload)
        if n < 126:
            header.append(0x80 | n)
        elif n < 65536:
            header.append(0x80 | 126)
            header += struct.pack(">H", n)
        else:
            header.append(0x80 | 127)
            header += struct.pack(">Q", n)
        mask = os.urandom(4)
        masked = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
        self.sock.sendall(bytes(header) + mask + masked)

    def _read(self, n):
        while len(self.buffer) < n:
            chunk = self.sock.recv(65536)
            if not chunk:
                raise ConnectionError("the dashboard closed the connection")
            self.buffer += chunk
        data, self.buffer = self.buffer[:n], self.buffer[n:]
        return data

    def receive(self):
        """The next text message, or None once the connection is closed."""
        message = b""
        while True:
            b1, b2 = self._read(2)
            fin, opcode = b1 & 0x80, b1 & 0x0F
            n = b2 & 0x7F
            if n == 126:
                n = struct.unpack(">H", self._read(2))[0]
            elif n == 127:
                n = struct.unpack(">Q", self._read(8))[0]
            mask = self._read(4) if b2 & 0x80 else None
            payload = self._read(n)
            if mask:
                payload = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
            if opcode == 0x8:  # close
                return None
            if opcode == 0x9:  # ping: answer with a pong
                self._pong(payload)
                continue
            if opcode == 0xA:  # pong
                continue
            message += payload
            if fin:
                return message.decode(errors="replace")

    def _pong(self, payload):
        mask = os.urandom(4)
        masked = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
        self.sock.sendall(bytes([0x8A, 0x80 | len(payload)]) + mask + masked)

    def close(self):
        try:
            self.sock.sendall(bytes([0x88, 0x80]) + os.urandom(4))
        except OSError:
            pass
        self.sock.close()


# ---- The dashboard's messages ------------------------------------------------------------

def connect(args):
    return WebSocket(args.host, args.port, timeout=args.timeout)


def wait_for(ws, wanted, seconds):
    """Read messages until one of type `wanted` arrives. None if it doesn't in time."""
    end = time.time() + seconds
    while time.time() < end:
        ws.sock.settimeout(max(0.1, end - time.time()))
        try:
            text = ws.receive()
        except socket.timeout:
            return None
        if text is None:
            return None
        msg = json.loads(text)
        if msg.get("type") == wanted:
            return msg
    return None


def get_config(ws):
    ws.send(json.dumps({"type": "GET_CONFIG"}))
    msg = wait_for(ws, "RECEIVE_CONFIG", 5)
    if msg is None:
        raise ConnectionError("the dashboard sent no settings: is an OpMode with live tuning running?")
    return msg["configRoot"]


def flatten(config, prefix=""):
    """{"Launcher.launcherKF": ("double", 0.00025), ...} from the dashboard's nested settings."""
    out = {}
    for name, var in (config.get("__value") or {}).items():
        full = prefix + name
        if var.get("__type") == "custom":
            out.update(flatten(var, full + "."))
        else:
            out[full] = (var.get("__type"), var.get("__value"))
    return out


def allowed(name):
    group, _, field = name.partition(".")
    if group in ALLOWED_GROUPS:
        return True
    return group == "Drive" and field.startswith(ALLOWED_DRIVE_PREFIXES)


def parse_value(kind, text):
    if kind == "boolean":
        if text.lower() not in ("true", "false"):
            raise ValueError(f"{text!r} is not true or false")
        return text.lower() == "true"
    if kind in ("int", "long"):
        return int(text)
    if kind in ("double", "float"):
        return float(text)
    if kind == "string":
        return text
    raise ValueError(f"settings of type {kind} can't be changed from here")


def config_diff(changes):
    """The dashboard's SAVE_CONFIG shape for {"Launcher.launcherKF": ("double", 0.00021)}."""
    root = {"__type": "custom", "__value": {}}
    for name, (kind, value) in changes.items():
        group, field = name.split(".", 1)
        node = root["__value"].setdefault(group, {"__type": "custom", "__value": {}})
        node["__value"][field] = {"__type": kind, "__value": value}
    return root


# ---- Commands ------------------------------------------------------------------------------

def cmd_status(args):
    ws = connect(args)
    ws.send(json.dumps({"type": "GET_ROBOT_STATUS"}))
    msg = wait_for(ws, "RECEIVE_ROBOT_STATUS", 5)
    ws.close()
    if msg is None:
        print("Connected, but the robot sent no status.")
        return 1
    status = msg.get("status", {})
    print(f"Connected to {args.host}:{args.port}")
    print(f"  OpMode:  {status.get('activeOpMode')}  ({status.get('activeOpModeStatus')})")
    print(f"  Battery: {status.get('batteryVoltage')}")
    if status.get("errorMessage"):
        print(f"  ERROR:   {status.get('errorMessage')}")
    if status.get("warningMessage"):
        print(f"  Warning: {status.get('warningMessage')}")
    return 0


def cmd_get(args):
    ws = connect(args)
    values = flatten(get_config(ws))
    ws.close()
    for name in sorted(values):
        if not args.names or any(name == n or name.startswith(n + ".") for n in args.names):
            kind, value = values[name]
            print(f"{name} = {value}   ({kind})")
    return 0


def cmd_set(args):
    ws = connect(args)
    try:
        return set_values(ws, args)
    finally:
        ws.close()


def set_values(ws, args):
    values = flatten(get_config(ws))
    changes = {}
    for item in args.assignments:
        name, sep, text = item.partition("=")
        if not sep:
            print(f"'{item}': write it as Group.name=value")
            return 2
        if name not in values:
            print(f"'{name}' is not a dashboard setting. `get` lists them.")
            return 2
        if not allowed(name) and not args.force:
            print(f"'{name}' is outside what a tuning session changes (Launcher, Intake, Drive.aim*, Drive.wiggle*).")
            return 2
        kind = values[name][0]
        changes[name] = (kind, parse_value(kind, text))
    ws.send(json.dumps({"type": "SAVE_CONFIG", "configDiff": config_diff(changes)}))
    after = flatten(get_config(ws))
    for name, (kind, value) in changes.items():
        old = values[name][1]
        now = after.get(name, (kind, None))[1]
        flag = "" if now == value else "   NOT TAKEN"
        print(f"{name}: {old} -> {now}{flag}")
    log_change(changes, values, after)
    return 0


def log_change(changes, before, after):
    """Every change goes into runs/changes.log, so the session can be retraced."""
    os.makedirs(RUNS, exist_ok=True)
    with open(os.path.join(RUNS, "changes.log"), "a") as f:
        stamp = datetime.datetime.now().isoformat(timespec="seconds")
        for name in changes:
            f.write(f"{stamp}  {name}: {before[name][1]} -> {after.get(name, (None, None))[1]}\n")


def cmd_record(args):
    ws = connect(args)
    os.makedirs(RUNS, exist_ok=True)
    out = args.out or os.path.join(RUNS, datetime.datetime.now().strftime("run_%Y-%m-%d_%H-%M-%S.jsonl"))
    packets = 0
    keys = set()
    end = time.time() + args.seconds
    next_ping = 0.0
    with open(out, "w") as f:
        while time.time() < end:
            if time.time() >= next_ping:
                # The dashboard page asks for the status about once a second; do the same.
                ws.send(json.dumps({"type": "GET_ROBOT_STATUS"}))
                next_ping = time.time() + 1.0
            ws.sock.settimeout(0.5)
            try:
                text = ws.receive()
            except socket.timeout:
                continue
            if text is None:
                break
            msg = json.loads(text)
            if msg.get("type") != "RECEIVE_TELEMETRY":
                continue
            for packet in msg.get("telemetry", []):
                data = packet.get("data", {})
                f.write(json.dumps({"t": packet.get("timestamp"), "data": data}) + "\n")
                packets += 1
                keys.update(data)
    ws.close()
    print(f"{packets} telemetry packets in {out}")
    test_keys = sorted(k for k in keys if k.startswith("T "))
    if not test_keys:
        print("No 'T ...' lines: is Tool: Launch Tuning running (testTelemetry on)?")
    return 0 if packets else 1


def number(data, key):
    try:
        return float(data.get(key, ""))
    except ValueError:
        return float("nan")


def read_shots(path):
    """One dict per shot, from the 'T shot_*' lines: each new 'T shots' count is a shot, and
    its mark is the last 'T shot_mark' seen before the next shot."""
    shots = []
    last_count = 0
    with open(path) as f:
        for line in f:
            data = json.loads(line)["data"]
            count = number(data, "T shots")
            if count != count:  # NaN: not a tuning packet
                continue
            if count > last_count:
                last_count = count
                shots.append({
                    "shot": int(count),
                    "range_in": number(data, "T shot_range_in"),
                    "target_rpm": number(data, "T shot_target_rpm"),
                    "dip_rpm": number(data, "T shot_dip_rpm"),
                    "recovery_s": number(data, "T shot_recovery_s"),
                    "recovered": number(data, "T shot_recovered") == 1,
                    "bearing_deg": number(data, "T hive_bearing_deg"),
                    "side_deg": number(data, "T side_deg"),
                    "heading_deg": number(data, "T heading_deg"),
                    "mark": 0,
                })
            if shots:
                mark = number(data, "T shot_mark")
                if mark == mark and mark != 0:
                    shots[-1]["mark"] = int(mark)
    return shots


MARKS = {1: "made", -1: "MISSED", 0: "?"}


def cmd_shots(args):
    shots = read_shots(args.file)
    if not shots:
        print("No shots in this recording.")
        return 1
    print(f"{'#':>3} {'range':>6} {'target':>7} {'dip':>5} {'back s':>6} {'bear':>6} {'side':>6} {'head':>6}  result")
    for s in shots:
        back = f"{s['recovery_s']:.2f}" if s["recovered"] else "never"
        print(f"{s['shot']:>3} {s['range_in']:>6.0f} {s['target_rpm']:>7.0f} {s['dip_rpm']:>5.0f} {back:>6} "
              f"{s['bearing_deg']:>6.1f} {s['side_deg']:>6.1f} {s['heading_deg']:>6.1f}  {MARKS[s['mark']]}")
    return 0


def cmd_summary(args):
    shots = read_shots(args.file)
    if not shots:
        print("No shots in this recording.")
        return 1
    bands = {}
    for s in shots:
        band = int(s["range_in"] // args.band * args.band) if s["range_in"] == s["range_in"] else -1
        bands.setdefault(band, []).append(s)
    print(f"{'range':>9} {'shots':>5} {'made':>4} {'missed':>6} {'?':>3} {'target':>7} {'dip':>5} {'back s':>6}")
    for band in sorted(bands):
        group = bands[band]
        made = sum(1 for s in group if s["mark"] == 1)
        missed = sum(1 for s in group if s["mark"] == -1)
        unknown = len(group) - made - missed
        target = sum(s["target_rpm"] for s in group) / len(group)
        dip = sum(s["dip_rpm"] for s in group) / len(group)
        back = [s["recovery_s"] for s in group if s["recovered"]]
        back_text = f"{sum(back) / len(back):.2f}" if back else "never"
        label = "no tag" if band < 0 else f"{band}-{band + args.band}"
        print(f"{label:>9} {len(group):>5} {made:>4} {missed:>6} {unknown:>3} {target:>7.0f} {dip:>5.0f} {back_text:>6}")
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--host", default=DEFAULT_HOST)
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--timeout", type=float, default=5.0)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("status", help="is the robot there, and which OpMode runs")
    p = sub.add_parser("get", help="settings on the dashboard (all, or a group like Launcher)")
    p.add_argument("names", nargs="*")
    p = sub.add_parser("set", help="change settings: Group.name=value ...")
    p.add_argument("assignments", nargs="+")
    p.add_argument("--force", action="store_true", help="allow settings outside the tuning groups")
    p = sub.add_parser("record", help="save the telemetry for a number of seconds")
    p.add_argument("seconds", type=float)
    p.add_argument("--out")
    p = sub.add_parser("shots", help="one line per shot in a recording")
    p.add_argument("file")
    p = sub.add_parser("summary", help="shots in a recording, grouped by distance")
    p.add_argument("file")
    p.add_argument("--band", type=int, default=5, help="inches per distance group")
    args = parser.parse_args(argv)
    commands = {"status": cmd_status, "get": cmd_get, "set": cmd_set, "record": cmd_record,
                "shots": cmd_shots, "summary": cmd_summary}
    try:
        return commands[args.command](args)
    except (OSError, ConnectionError) as e:
        print(f"Can't reach the dashboard at {args.host}:{args.port}: {e}")
        print("Is the laptop on the robot's Wi-Fi, the Robot Controller app running, and the dashboard enabled?")
        return 3


if __name__ == "__main__":
    sys.exit(main())
