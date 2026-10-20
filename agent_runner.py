#!/usr/bin/env python3
"""agent_runner.py: PTY-based harness for OpenCode & Antigravity (agy).

Uses pty + os.setsid for proper TTY emulation, preventing terminal detection
deadlocks in headless environments. Outputs structured JSON for Hermes.

Usage:
    python3 agent_runner.py --agent opencode --prompt "Create a version.json" --model oc/muse-spark-1.2
    python3 agent_runner.py --agent agy --prompt "Audit this repo" --timeout 300
"""

import argparse
import json
import os
import pty
import select
import signal
import subprocess
import sys
import time


def build_command(agent: str, prompt: str, model: str) -> list[str]:
    """Build the correct headless command for each agent."""
    if agent == "opencode":
        cmd = ["opencode", "run", prompt]
        if model:
            cmd.extend(["--model", model])
        return cmd
    elif agent in ("antigravity", "agy"):
        cmd = ["agy", "-p", prompt, "--dangerously-skip-permissions"]
        if model:
            cmd.extend(["--model", model])
        return cmd
    else:
        raise ValueError(f"Unsupported agent: {agent}")


def execute_in_pty(cmd: list[str], timeout: int) -> tuple[int, str]:
    """Execute a command in a pseudo-TTY with timeout and structured output."""
    master, slave = pty.openpty()
    proc = subprocess.Popen(
        cmd,
        stdin=slave,
        stdout=slave,
        stderr=slave,
        close_fds=True,
        preexec_fn=os.setsid,
    )
    os.close(slave)

    output_chunks = []
    start_time = time.time()

    try:
        while True:
            elapsed = time.time() - start_time
            if elapsed > timeout:
                os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
                proc.wait()
                raise TimeoutError(f"Task timed out after {timeout} seconds")

            r_ready, _, _ = select.select([master], [], [], 0.5)
            if master in r_ready:
                try:
                    data = os.read(master, 2048)
                    if not data:
                        break
                    output_chunks.append(data)
                except OSError:
                    break

            if proc.poll() is not None:
                while True:
                    r_flush, _, _ = select.select([master], [], [], 0.1)
                    if master in r_flush:
                        try:
                            data = os.read(master, 2048)
                            if not data:
                                break
                            output_chunks.append(data)
                        except OSError:
                            break
                    else:
                        break
                break
    finally:
        os.close(master)

    proc.wait()
    cleaned_output = b"".join(output_chunks).decode("utf-8", errors="replace")
    return proc.returncode, cleaned_output


def run_agent_with_retry(agent: str, prompt: str, model: str, timeout: int, retries: int) -> dict:
    """Run agent with exponential backoff retry."""
    cmd = build_command(agent, prompt, model)
    attempts = 0
    start_time = time.time()
    exit_code = -1
    output = ""

    while attempts <= retries:
        attempts += 1
        error_reason = "Unknown error"  # Initialize to avoid possibly-unbound lint error
        try:
            exit_code, output = execute_in_pty(cmd, timeout)
            if exit_code == 0:
                return {
                    "status": "success",
                    "agent": agent,
                    "exit_code": exit_code,
                    "attempts": attempts,
                    "duration_seconds": round(time.time() - start_time, 2),
                    "output": output.strip(),
                }
            error_reason = f"Agent exited with non-zero code {exit_code}"
        except TimeoutError as err:
            exit_code = 124
            output = str(err)
            error_reason = "Execution timeout reached"

        if attempts <= retries:
            time.sleep(2 ** attempts)

    return {
        "status": "failed",
        "agent": agent,
        "exit_code": exit_code,
        "attempts": attempts,
        "duration_seconds": round(time.time() - start_time, 2),
        "error": error_reason,
        "output": output.strip(),
    }


def main():
    parser = argparse.ArgumentParser(description="PTY Harness for OpenCode & Antigravity")
    parser.add_argument("--agent", choices=["opencode", "antigravity", "agy"], required=True)
    parser.add_argument("--prompt", required=True, help="Task prompt for the worker agent")
    parser.add_argument("--model", default=None, help="Target model override")
    parser.add_argument("--timeout", type=int, default=180, help="Process timeout in seconds")
    parser.add_argument("--retries", type=int, default=1, help="Max retry attempts on failure")

    args = parser.parse_args()
    result = run_agent_with_retry(
        agent=args.agent,
        prompt=args.prompt,
        model=args.model,
        timeout=args.timeout,
        retries=args.retries,
    )

    print(json.dumps(result, indent=2))
    sys.exit(0 if result["status"] == "success" else 1)


if __name__ == "__main__":
    main()
