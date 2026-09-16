# AGENTS.md

## Bidirectional Sync — `D:\hims_new_deloitte` (monorepo)

This is the **standalone frontend repo**. The canonical monorepo is `D:\hims_new_deloitte` (backend + frontend under `frontend/`).

### Remotes
| Repo | Remote name | Path |
|------|-------------|------|
| `D:\hims` (this repo) | `hims_new_deloitte` | `D:\hims_new_deloitte` |
| `D:\hims_new_deloitte` | `hims` | `D:\hims` |

### Sync Rules

**When you change frontend code in THIS repo (`hims`):**
```bash
# After committing, push changes to the monorepo
git add -A && git commit -m "feat(frontend): <description>"
# Push to the monorepo
cd D:\hims_new_deloitte && git fetch hims && git checkout main && git merge hims/main --no-edit
```

**When you change frontend code in `D:\hims_new_deloitte`:**
```bash
# After committing in hims_new_deloitte, pull changes into this repo
cd D:\hims && git fetch hims_new_deloitte && git merge hims_new_deloitte/main --no-edit
```

**Quick sync script (from either repo):**
```bash
# Push to both repos
git push origin main && git push hims_new_deloitte main

# Pull from both repos
git pull origin main && git pull hims_new_deloitte main
```

### Conflict Resolution
- If both repos modify the same frontend file, resolve conflicts in `D:\hims_new_deloitte` (source of truth for monorepo)
- Then push the resolved state to `D:\hims`

## Project Structure

- **Frontend**: React+Vite+TypeScript under `src/features/`
- **Monorepo**: `D:\hims_new_deloitte` (backend + frontend)

## Conventions

- Frontend: Zod schemas, TanStack Query, feature-folder pattern matching `features/masters/`
- UI: Chakra UI v3 with the Deloitte theme in `src/theme/himsTheme.ts`; shared primitives in `src/components/ui/` (PageHeader, HimsCard, StatusBadge, FormField, …). Re-run `chakra typegen` after theme changes (`postinstall` does it automatically).
- Never add speculative features; only implement what the gap matrix lists
- Run lint/typecheck/tests after every change
