#!/usr/bin/env python3
"""Tests for dash.py against a pretend FTC Dashboard on this laptop, so the client can be
checked without a robot. Run:  python3 tools/tuning/test_dash.py
"""

import base64
import hashlib
import io
import json
import os
import socket
import struct
import sys
import tempfile
import threading
import unittest
from contextlib import redirect_stdout

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dash  # noqa: E402


def packet(**values):
    return {"timestamp": 0, "data": {f"T {k}": str(v) for k, v in values.items()}, "log": []}


class FakeDashboard(threading.Thread):
    """A one-connection-at-a-time WebSocket server speaking the dashboard's messages."""

    def __init__(self, telemetry=()):
        super().__init__(daemon=True)
        self.server = socket.socket()
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server.bind(("127.0.0.1", 0))
        self.server.listen(5)
        self.port = self.server.getsockname()[1]
        self.telemetry = list(telemetry)
        self.saved = []
        self.config = {"__type": "custom", "__value": {
            "Launcher": {"__type": "custom", "__value": {
                "launcherKF": {"__type": "double", "__value": 0.0002},
                "steadyRange": {"__type": "boolean", "__value": True}}},
            "Drive": {"__type": "custom", "__value": {
                "driveNormal": {"__type": "double", "__value": 1.0},
                "aimKP": {"__type": "double", "__value": 0.02}}}}}

    def run(self):
        while True:
            try:
                conn, _ = self.server.accept()
            except OSError:
                return
            self.serve(conn)

    def serve(self, conn):
        request = b""
        while b"\r\n\r\n" not in request:
            request += conn.recv(1024)
        key = [l.split(b": ")[1] for l in request.split(b"\r\n") if l.lower().startswith(b"sec-websocket-key")][0]
        accept = base64.b64encode(hashlib.sha1(key + b"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").digest())
        conn.sendall(b"HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                     b"Sec-WebSocket-Accept: " + accept + b"\r\n\r\n")
        sent_telemetry = False
        while True:
            text = self.read_frame(conn)
            if text is None:
                conn.close()
                return
            msg = json.loads(text)
            kind = msg["type"]
            if kind == "GET_CONFIG":
                self.send(conn, {"type": "RECEIVE_CONFIG", "configRoot": self.config})
            elif kind == "SAVE_CONFIG":
                self.saved.append(msg["configDiff"])
                for group, node in msg["configDiff"]["__value"].items():
                    for field, var in node["__value"].items():
                        self.config["__value"][group]["__value"][field]["__value"] = var["__value"]
            elif kind == "GET_ROBOT_STATUS":
                self.send(conn, {"type": "RECEIVE_ROBOT_STATUS", "status": {
                    "activeOpMode": "Tool: Launch Tuning", "activeOpModeStatus": "RUNNING",
                    "batteryVoltage": 12.6, "errorMessage": "", "warningMessage": ""}})
                if not sent_telemetry and self.telemetry:
                    sent_telemetry = True
                    self.send(conn, {"type": "RECEIVE_TELEMETRY", "telemetry": self.telemetry})

    @staticmethod
    def read_frame(conn):
        head = conn.recv(2)
        if len(head) < 2:
            return None
        opcode = head[0] & 0x0F
        n = head[1] & 0x7F
        if n == 126:
            n = struct.unpack(">H", conn.recv(2))[0]
        elif n == 127:
            n = struct.unpack(">Q", conn.recv(8))[0]
        mask = conn.recv(4)
        payload = b""
        while len(payload) < n:
            payload += conn.recv(n - len(payload))
        payload = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
        if opcode == 0x8:
            return None
        return payload.decode()

    @staticmethod
    def send(conn, msg):
        data = json.dumps(msg).encode()
        n = len(data)
        if n < 126:
            head = bytes([0x81, n])
        elif n < 65536:
            head = bytes([0x81, 126]) + struct.pack(">H", n)
        else:
            head = bytes([0x81, 127]) + struct.pack(">Q", n)
        conn.sendall(head + data)


def run(fake, *argv):
    out = io.StringIO()
    with redirect_stdout(out):
        code = dash.main(["--host", "127.0.0.1", "--port", str(fake.port), *argv])
    return code, out.getvalue()


class DashTest(unittest.TestCase):
    def setUp(self):
        self.runs = tempfile.mkdtemp()
        dash.RUNS = self.runs

    def test_status(self):
        fake = FakeDashboard()
        fake.start()
        code, out = run(fake, "status")
        self.assertEqual(0, code)
        self.assertIn("Tool: Launch Tuning", out)

    def test_get_lists_settings(self):
        fake = FakeDashboard()
        fake.start()
        code, out = run(fake, "get", "Launcher")
        self.assertIn("Launcher.launcherKF = 0.0002", out)
        self.assertNotIn("Drive.", out)

    def test_set_sends_the_dashboard_shape_and_logs_it(self):
        fake = FakeDashboard()
        fake.start()
        code, out = run(fake, "set", "Launcher.launcherKF=0.00021", "Launcher.steadyRange=false")
        self.assertEqual(0, code, out)
        self.assertIn("Launcher.launcherKF: 0.0002 -> 0.00021", out)
        diff = fake.saved[0]["__value"]["Launcher"]["__value"]
        self.assertEqual({"__type": "double", "__value": 0.00021}, diff["launcherKF"])
        self.assertEqual({"__type": "boolean", "__value": False}, diff["steadyRange"])
        with open(os.path.join(self.runs, "changes.log")) as f:
            self.assertIn("Launcher.launcherKF: 0.0002 -> 0.00021", f.read())

    def test_set_refuses_drive_settings(self):
        fake = FakeDashboard()
        fake.start()
        code, out = run(fake, "set", "Drive.driveNormal=0.5")
        self.assertEqual(2, code)
        self.assertEqual([], fake.saved)
        code, out = run(fake, "set", "Drive.aimKP=0.025")   # aim settings are allowed
        self.assertEqual(0, code, out)

    def test_set_refuses_unknown_names(self):
        fake = FakeDashboard()
        fake.start()
        code, out = run(fake, "set", "Launcher.launcherKX=1")
        self.assertEqual(2, code)
        self.assertIn("not a dashboard setting", out)

    def test_record_then_shots_and_summary(self):
        telemetry = [
            packet(shots=0, shot_mark=0),
            packet(shots=1, shot_range_in=124, shot_target_rpm=3005, shot_dip_rpm=290,
                   shot_recovery_s=0.21, shot_recovered=1, hive_bearing_deg=1.5, side_deg=-12, heading_deg=88, shot_mark=0),
            packet(shots=1, shot_mark=1),
            packet(shots=2, shot_range_in=126, shot_target_rpm=3005, shot_dip_rpm=410,
                   shot_recovery_s=0.4, shot_recovered=1, hive_bearing_deg=0.2, side_deg=-11, heading_deg=88, shot_mark=0),
            packet(shots=2, shot_mark=-1),
        ]
        fake = FakeDashboard(telemetry)
        fake.start()
        out_file = os.path.join(self.runs, "run.jsonl")
        code, out = run(fake, "record", "1.5", "--out", out_file)
        self.assertEqual(0, code, out)
        self.assertIn("5 telemetry packets", out)
        code, out = run(fake, "shots", out_file)
        lines = out.strip().splitlines()
        self.assertEqual(3, len(lines), out)
        self.assertTrue(lines[1].rstrip().endswith("made"), out)
        self.assertTrue(lines[2].rstrip().endswith("MISSED"), out)
        code, out = run(fake, "summary", out_file)
        self.assertIn("120-125", out)
        self.assertIn("125-130", out)

    def test_no_robot(self):
        code, out = dash_main_quiet(["--host", "127.0.0.1", "--port", "1", "--timeout", "0.5", "status"])
        self.assertEqual(3, code)
        self.assertIn("robot's Wi-Fi", out)


def dash_main_quiet(argv):
    out = io.StringIO()
    with redirect_stdout(out):
        code = dash.main(argv)
    return code, out.getvalue()


if __name__ == "__main__":
    unittest.main()
