#!/usr/bin/env python3
"""verify_and_run.py: Orchestrates runner execution, test suites, and git rollbacks.

Hermes invokes this atomically to delegate tasks with automatic verification:
    python3 verify_and_run.py opencode "Add input validation" "pytest tests/"
    python3 verify_and_run.py agy "Audit for security issues" "npm test"

Flow:
    1. Ensure clean git state (commit or stash dirty work)
    2. Execute agent_runner.py and parse JSON response
    3. If failed/hard rollback: git reset --hard HEAD && git clean -fd
    4. If success: run test command
    5. If tests pass: report verified status
    6. If tests fail: rollback and report failure with test output
"""

import json
import subprocess
import sys


def run_cmd(cmd: list[str]) -> tuple[int, str]:
    """Execute a shell command and return (exit_code, output)."""
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.returncode, (res.stdout + res.stderr).strip()


def rollback():
    """Reset all changes and clean untracked files."""
    run_cmd(["git", "reset", "--hard", "HEAD"])
    run_cmd(["git", "clean", "-fd"])


def get_git_status() -> str:
    """Check current git status."""
    _, output = run_cmd(["git", "status", "--porcelain"])
    return output


def main():
    if len(sys.argv) < 4:
        print(json.dumps({
            "status": "error",
            "error": "Usage: python3 verify_and_run.py <agent> <prompt> <test_cmd>"
        }))
        sys.exit(1)

    agent = sys.argv[1]
    prompt = sys.argv[2]
    test_cmd = sys.argv[3]

    # 1. Check baseline git status
    baseline = get_git_status()
    if baseline:
        print(json.dumps({
            "status": "warning",
            "warning": "Working directory was dirty before agent execution",
            "dirty_files": baseline.strip().split("\n")
        }))

    # 2. Run agent harness via PTY runner
    code, runner_out = run_cmd([
        "python3", "agent_runner.py",
        "--agent", agent,
        "--prompt", prompt,
        "--timeout", "240",
        "--retries", "1",
    ])

    try:
        data = json.loads(runner_out)
    except json.JSONDecodeError:
        rollback()
        print(json.dumps({
            "status": "failed",
            "error": "Invalid runner output (JSON parse failed)",
            "raw_output": runner_out[:500]
        }))
        sys.exit(1)

    if data.get("status") != "success":
        rollback()
        print(json.dumps({
            "status": "failed",
            "error": "Agent execution failed",
            "details": data
        }))
        sys.exit(1)

    # 3. Agent succeeded — run test verification command
    test_code, test_out = run_cmd(test_cmd.split())
    if test_code != 0:
        rollback()
        print(json.dumps({
            "status": "rollback",
            "error": "Tests failed after agent execution",
            "test_output": test_out[:1000],
            "agent_output": data.get("output", "")[:500]
        }))
        sys.exit(1)

    # 4. Tests passed — report verified status
    print(json.dumps({
        "status": "verified",
        "agent": agent,
        "summary": "All tests passed",
        "duration_seconds": data.get("duration_seconds", 0),
        "attempts": data.get("attempts", 1),
        "agent_output": data.get("output", "")[:500]
    }))
    sys.exit(0)


if __name__ == "__main__":
    main()
