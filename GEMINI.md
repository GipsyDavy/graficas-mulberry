# Gemini Code Assist Entry Point

Project: Graficas Mulberry — Java 21 + JavaFX 21 + SQLite/JDBC + Maven.

Treat `AGENTS.md` as the main cross-agent instruction file.

## Required reading

- `AGENTS.md` — cross-agent rules, required reading list, sinceridad técnica.
- `CLAUDE.md` — project workflow, Multi-IA rules, sprint checklist.
- `MACRO-PROMPT-GRAFICAS-MULBERRY.md` — full project context.
- `docs/context/STATE.md` — current HEAD, tests, active sprint.
- `docs/security/README.md` — security documentation index.
- `docs/security/SECURITY_AUDIT_2026-06-13.md` — latest full security audit.
- `docs/security/SECURITY_REMEDIATION_2026-06-13.md` — current remediation status.
- `docs/security/SECURITY_AUDIT_RUNBOOK.md` — reproducible audit commands.

## Path rules

- Project root: `C:\Users\GipsyDavy\MAVEN\Graficas Mulberry`
- Use project-relative paths in all suggestions (e.g. `src/main/java/...`).
- Avoid paths with accents or spaces when suggesting shell commands.
- The database is SQLite only. Do not suggest MySQL, MongoDB, or any remote DB.

## Invocation model

Gemini is invoked exclusively via IDE blocks pasted by the user — never via CLI.
When responding, scope your analysis to the specific question in the block.
Do not modify files directly unless the block explicitly requests it.

## VibeSec

VibeSec is the security review skill for this project.
Invoke it proactively when the conversation touches auth, permissions, file paths,
import/export, or sensitive data. If unavailable, flag the gap explicitly.
