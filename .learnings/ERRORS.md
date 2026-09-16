# Errors

Command failures and integration errors.

---

## [ERR-20260914-001] workspace-path-and-git-check

**Logged**: 2026-09-14T00:00:00+08:00
**Priority**: medium
**Status**: resolved
**Area**: config

### Summary
启动上下文中的工作目录不是实际后端源码目录，且实际项目目录没有 Git 元数据。

### Error
```text
Cannot find path ...\src\main\java\io\github\tissyboxc
fatal/warning: Not a git repository
```

### Context
- 初始目录：D:\service\sqlwork\Hospital Appointment Registration Management System
- 实际源码：D:\service\AAAA-sqlwork\Hospital_Appointment_Registration_Management_System
- 尝试使用 git diff 进行静态变更核对时发现该目录不是 Git 仓库。

### Suggested Fix
执行任务前先定位包含 build.gradle 与 src/main 的真实目录；无 Git 元数据时使用文件清单、rg 和逐文件静态核对。

### Metadata
- Reproducible: yes
- Related Files: build.gradle, src/main

### Resolution
- **Resolved**: 2026-09-14T00:00:00+08:00
- **Notes**: 已切换到实际源码目录，并改用文件级静态核对。

---
