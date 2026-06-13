# AI Agent Entry Point

Project: Graficas Mulberry, Java 21 + JavaFX 21 + SQLite/JDBC + Maven.

Root path:

```text
C:\Users\GipsyDavy\MAVEN\Graficas Mulberry
```

This file is the neutral entry point for AI agents such as Codex, Claude Code,
Gemini Code Assist, and other IDE agents.

## Required Reading

Before modifying this project, read:

- `CLAUDE.md` for project workflow, Multi-IA rules, validation, and coding rules.
- `docs/security/README.md` for the security documentation index.
- `docs/security/SECURITY_AUDIT_2026-06-13.md` for the latest full security audit.
- `docs/security/SECURITY_REMEDIATION_2026-06-13.md` for the current remediation status.
- `docs/security/SECURITY_AUDIT_RUNBOOK.md` for reproducible security audit commands.

For UI and product context, read:

- `interfaz.md`
- `AUDITORIA.md`

## Security Audit Status

The latest documented security audit is:

```text
docs/security/SECURITY_AUDIT_2026-06-13.md
```

The current remediation status is:

```text
docs/security/SECURITY_REMEDIATION_2026-06-13.md
```

Supporting tool outputs are stored under:

```text
docs/security/
```

Do not delete or overwrite previous audit artifacts. For a new audit, create
new dated files and update `docs/security/README.md`.

## Agent Handoff Rules

- Preserve existing user changes unless explicitly instructed otherwise.
- For security, authentication, database, import/export, installers, or file
  system changes, consult the security docs before editing.
- If an external tool is unavailable, record the limitation in the audit notes.
- If using Gemini Code Assist, start from `GEMINI.md`, which points back here.
