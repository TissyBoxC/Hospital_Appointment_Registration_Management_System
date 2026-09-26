# Repository Working Rules

## Working Directory

- This repository's canonical working directory is `D:\service\AAAA-sqlwork\Hospital_Appointment_Registration_Management_System`.
- Make all repository changes directly in that directory.
- Do not create, switch, modify, or synchronize through any Git worktree under `C:\Users\Provias\.codex\worktrees`.
- Treat the Git checkout in the canonical directory as the only source of truth.

## Build Rules

- Use the local Gradle installation at `D:\service\gradle-9.4.1`.
- Prefer the command `D:\service\gradle-9.4.1\bin\gradle.bat --offline --console plain`.
- Do not use the Gradle Wrapper and do not download a Gradle distribution.
- Keep builds offline unless the user explicitly approves network access.
- Use Java 21 for this project.
