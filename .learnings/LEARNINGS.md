# Learnings

Corrections, insights, and knowledge gaps captured during development.

**Categories**: correction | insight | knowledge_gap | best_practice

---

## [LRN-20260902-003] correction

**Logged**: 2026-09-02T19:40:00+08:00
**Priority**: high
**Status**: pending
**Area**: backend

### Summary
当前项目使用 Spring Boot 4.1.1，不能默认采用 MyBatis-Plus 方案。

### Details
用户指出当前版本没有可用的 MyBatis-Plus 依赖/兼容方案。后续应优先评估 Spring Data JDBC、Spring JDBC 或与 Boot 4 明确兼容的持久化组件。

### Suggested Action
设计通用 CRUD 时，以现有 `spring-boot-starter-jdbc` 的 `JdbcTemplate`/`JdbcClient` 或 Spring Data JDBC 为基线；预约、支付、排班等事务流程使用专用 Service 和事务控制。

### Metadata
- Source: user_feedback
- Related Files: build.gradle
- Tags: spring-boot-4, compatibility, persistence

---

## [LRN-20260902-002] correction

**Logged**: 2026-09-02T19:35:00+08:00
**Priority**: critical
**Status**: pending
**Area**: workflow

### Summary
未收到明确的代码修改请求时，只做分析，不修改项目文件。

### Details
用户要求：任何实际代码或配置修改前必须先询问并获得确认；默认响应范围是分析、诊断和建议。该规则优先于通常的自动实现倾向。

### Suggested Action
先给出分析结果和拟修改内容，明确询问是否执行；获得明确同意后才能调用文件编辑工具。

### Metadata
- Source: user_feedback
- Related Files: .learnings/LEARNINGS.md
- Tags: project-preference, ask-before-editing, analysis-only

---

## [LRN-20260902-001] correction

**Logged**: 2026-09-02T19:30:00+08:00
**Priority**: high
**Status**: pending
**Area**: workflow

### Summary
本项目后续任务不主动执行编译、测试或启动应用。

### Details
用户明确要求自行手动编译验证；Codex 只进行代码/配置修改和静态检查，除非用户在当前任务中明确要求运行构建或测试命令。

### Suggested Action
开始任务前遵守该项目协作约定，避免调用 `gradle`、`gradlew`、测试运行器或应用启动命令。

### Metadata
- Source: user_feedback
- Related Files: build.gradle
- Tags: project-preference, no-build

---
