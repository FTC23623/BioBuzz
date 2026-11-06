#!/bin/sh
# Start a launch tuning session with Claude Code (tools/tuning/README.md).
#
#   tools/tuning/tune.sh            check the robot, then start the session
#   tools/tuning/tune.sh --offline  start anyway (to read old recordings, or try things out)
#
# The session is interactive: Claude asks the people at the robot to launch and mark shots.
# Only tools/tuning/dash.py runs without asking; anything else asks you first.

cd "$(dirname "$0")/../.." || exit 1

if [ "$1" != "--offline" ]; then
    if ! python3 tools/tuning/dash.py --timeout 3 status; then
        echo
        echo "Start Tool: Launch Tuning on the Driver Station, join the laptop to the robot's"
        echo "Wi-Fi, and check the dashboard is enabled (Robot Controller menu). Or run with --offline."
        exit 1
    fi
fi

mkdir -p docs/tuning-log tools/tuning/runs

FIRST="Start the launch tuning session. Check the robot with dash.py status, read the current Launcher, Intake and Drive aim settings, then tell us what you want to test first and what we should do."

# The opening message goes first: --allowedTools takes a list and would swallow it.
exec claude "$FIRST" \
    --name "Launch tuning $(date +%Y-%m-%d)" \
    --append-system-prompt "$(cat tools/tuning/SESSION.md)" \
    --allowedTools "Bash(python3 tools/tuning/dash.py:*)" "Read"
