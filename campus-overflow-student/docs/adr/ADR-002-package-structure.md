# ADR-002-package-structure：按限界上下文物理分包的多模块 Maven 工程

**状态**：已接受 ｜ **驱动特征**：可维护性、可演进性 ｜ **相关场景**：QS-02、FF-1~FF-6 ｜ **互补决策**：[001](001-模块化单体.md) 模块化单体

## 背景

事件风暴识别出四个限界上下文（Identity / Q&A / Reputation / Discovery）与一个共享内核。若将全部代码放在单一模块中，"依赖方向只能由外向内""跨上下文只能经 api 包交流"等架构约束只能靠评审自觉，无法由构建工具强制执行（FF-2~FF-6 是适应度函数，要求 CI 自动验证）。

## 决策

1. **按限界上下文切分 Maven 模块**：`co-identity`、`co-qa`、`co-reputation`、`co-discovery` 各为一个父模块，加上 `co-shared-kernel`（共享内核）与 `co-bootstrap`（装配与启动），共 6 个模块组、27 个 Maven 模块。
2. **每个上下文模块内部按六边形分层再切子模块**：`-api`（发布语言：查询接口、集成事件契约）、`-domain`（纯 Java 领域模型，零框架依赖）、`-application`（用例编排、事务边界）、`-infrastructure`（JPA/Redis/JDBC 适配器）、`-interfaces`（REST 控制器）。
3. **父 POM 统一依赖版本**（Spring Boot 3.x、JDK 21、JUnit 5、JaCoCo、ArchUnit），子模块只声明自己需要的依赖。
4. **`co-bootstrap` 装配一切**：组件扫描、安全链、发件箱中继、统一异常处理、启动入口；上下文的 implementation 依赖在此汇合，运行时仍是单体进程。

依赖规则（由 Maven 依赖 + ArchUnit 双重保证）：

```
bootstrap ──▶ interfaces ──▶ application ──▶ domain
    │              │              │
    └──────────────┴──────▶ infrastructure（实现 domain/application 端口）
跨上下文：任何 context 只依赖对方的 -api（发布语言），禁止依赖其 domain/application/infrastructure/interfaces
```

## 后果

- 正面：上下文依赖方向由 Maven 模块依赖强制；领域层连 Spring/JPA 的依赖都无法引入（FF-1）；独立部署某个上下文时只需改 bootstrap 的装配。
- 负面：模块数较多，新成员上手需要先读本 ADR 与 [001](001-模块化单体.md)；上下文拆分边界若错误（把跨上下文的用例塞进一个应用服务），重构成本高于单模块。

## 被否决的替代方案

- **按技术层切分模块**（所有 controller 一个模块、所有 service 一个模块）：技术分层无法表达业务边界，跨上下文调用会失控，且与 DDD 限界上下文物理解耦的目标相悖。
- **每个上下文独立微服务**：本课程项目规模与团队（4–6 人）不足以承担分布式运维成本；模块化单体保留将来按模块拆服务的可能性。
