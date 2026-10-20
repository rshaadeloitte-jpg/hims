# AGENTS.md

## Legacy Codebase Cross-Reference Rule

When implementing any new feature, ALWAYS:
1. Check the old IHMS codebase at `/Users/adityabanerjee/personal_project/ihms/src` for existing logic covering that feature
2. Port business rules, validation, field names, and stored procedure behavior from the old code
3. Mark the corresponding gap as done in `docs/modernization/GAP-ANALYSIS-AND-PLAN.md`
4. No further clarification is required once the old codebase logic has been reviewed and ported

## Orchestrator Protocol

You are the Lead Technical Project Manager and Orchestrator. Your job is to plan, delegate tasks to worker CLI agents, review outputs, and ensure system integrity.

### Available Subordinate Agents

1. **OpenCode** (Implementation & Refactoring):
   - Command: `opencode run "<task>" --model oc/muse-spark-1.2`
   - Best for: Writing feature code, fixing functions, generating tests.

2. **Antigravity / agy** (Architecture, Audits & Scaffolding):
   - Command: `agy -p "<task>" --dangerously-skip-permissions`
   - Best for: Repository inspection, migrations, cross-cutting analysis.

### Worker Execution & Rollback Protocol

Before delegating any task, follow this verification loop:

1. **Checkpoint**: Run `git status --porcelain`. Ensure working directory is clean.
2. **Invocation**: Execute worker through `agent_runner.py` and capture JSON response:
   ```
   result=$(python3 agent_runner.py --agent <opencode|agy> --prompt "<prompt>" --timeout 180)
   ```
3. **JSON Evaluation**:
   - If `status != "success"`, run: `git reset --hard HEAD && git clean -fd`
   - Report failure to user with error details
4. **Diff & Test Verification** (if success):
   - Run `git diff` to confirm modifications match scope
   - Run validation (e.g., `pytest`, `npm test`, `mvn test`)
   - If tests FAIL: `git reset --hard HEAD && git clean -fd`
   - If tests PASS: `git add . && git commit -m "feat(agent): completed <task>"`

### Automated Pipeline (verify_and_run.py)

For atomic execution with built-in rollback:
```
python3 verify_and_run.py opencode "Add input validation" "pytest tests/"
python3 verify_and_run.py agy "Audit for security issues" "npm test"
```

### Operational Protocol

1. **Task Decomposition**: Break requirements into atomic, verifiable subtasks
2. **Delegation**: Execute via agent_runner.py. Always provide full task context.
3. **Verification**: Inspect repo state (git diff, test results) after each run
4. **Error Recovery**: Capture stderr/test output, dispatch targeted corrective run
5. **Reporting**: High-level progress summaries, completed tickets, verification results

### Critical Invocation Rules

- **OpenCode**: ALWAYS pass prompt as positional arg. NEVER bare `opencode run`.
- **Antigravity**: ALWAYS use `-p` (headless) + `--dangerously-skip-permissions` (auto-approve).
- Both agents use PTY harness (`agent_runner.py`) for proper TTY emulation in headless environments.

## Project Structure

- **Backend**: Java microservices under `services/` (DDD layers: api/application/domain/infrastructure)
- **Frontend**: React+Vite+TypeScript under `frontend/src/features/`
- **Docs**: Modernization docs under `docs/modernization/`
- **Old codebase**: `/Users/adityabanerjee/personal_project/ihms/src` (Java monolith, `com.ohum`)

## Conventions

- Follow existing DDD patterns in each service (outbox, CorrelationIdFilter, GlobalExceptionHandler)
- Frontend: Zod schemas, TanStack Query, feature-folder pattern matching `features/masters/`
- Never add speculative features; only implement what the gap matrix lists
- Run lint/typecheck/tests after every change

## Bidirectional Sync — `D:\hims` (frontend standalone repo)

The frontend code lives in two repos:
- **`D:\hims_new_deloitte`** (this repo) — monorepo with backend + frontend under `frontend/`
- **`D:\hims`** — standalone frontend repo (copy of `frontend/`)

### Remotes
| Repo | Remote name | Path |
|------|-------------|------|
| `D:\hims` | `hims_new_deloitte` | `D:\hims_new_deloitte` |
| `D:\hims_new_deloitte` | `hims` | `D:\hims` |

### Sync Rules

**When you change frontend code in THIS repo (`hims_new_deloitte`):**
```bash
# After committing, push frontend changes to D:\hims
git add frontend/ && git commit -m "feat(frontend): <description>"
# Push to the standalone frontend repo
cd D:\hims && git fetch hims_new_deloitte && git merge hims_new_deloitte/main --no-edit
# Or if branches differ:
cd D:\hims && git fetch hims_new_deloitte && git checkout main && git merge hims_new_deloitte/frontend-sync
```

**When you change frontend code in `D:\hims`:**
```bash
# After committing in D:\hims, pull changes into this repo's frontend/
cd D:\hims_new_deloitte && git fetch hims && git checkout main && git merge hims/main --no-edit
```

**Quick sync script (from either repo):**
```bash
# Push to both repos
git push origin main && git push hims main

# Pull from both repos
git pull origin main && git pull hims main
```

### Conflict Resolution
- If both repos modify the same frontend file, resolve conflicts in `D:\hims_new_deloitte` (source of truth for monorepo)
- Then push the resolved state to `D:\hims`
