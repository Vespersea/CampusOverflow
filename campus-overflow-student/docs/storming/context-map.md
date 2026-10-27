# 上下文映射（Context Map）

> 版本：v1.0 ｜ 对应 4 个限界上下文：co-identity、co-qa、co-reputation、co-discovery，外加 co-shared-kernel 共享内核与 co-bootstrap 装配层。

## 1. 上下文映射总图（ASCII）

```
                 ┌──────────────────────────────────────────────────────────┐
                 │                   co-shared-kernel（共享内核）              │
                 │  领域异常 / 分页 PageRequest·PageResult / 事件契约          │
                 │  IntegrationEvent / Actor / Role / UtcTime                 │
                 └───────────────▲──────────▲──────────▲──────────▲───────────┘
                    (共享内核 SK)  │          │          │          │
   ┌──────────────────┐   ┌───────┴───┐  ┌───┴───────┐  ┌─┴────────┐  ┌──────┴─────────┐
   │   co-identity    │   │   co-qa   │  │co-reputation│ │co-discovery│  │  co-bootstrap  │
   │  身份认证上下文    │   │ 问答核心   │  │  声誉上下文  │ │ 发现上下文  │  │ 装配/安全/中继  │
   │                  │   │           │  │            │ │            │  │                │
   │ User/Course 聚合  │   │ Question/ │  │ RepAccount │ │ SearchDoc  │  │ 依赖全部上下文   │
   │                  │   │ Vote/     │  │ /Bounty/   │ │ /Notif     │  │ 的组合根         │
   │                  │   │ Comment   │  │ BadgeAward │ │            │  │                │
   └────────┬─────────┘   └───┬───────┘  └───▲───────┘  └─▲─────────┘  └────────────────┘
            │                 │              │            │
            │ ①客户-供应商      │ ②客户-供应商    │            │
            │ (OHS/发布语言     │ + 集成事件      │            │
            │  co-identity-api) │ (VoteCast等)   │            │
            └────────▶─────────┘              │            │
            │                 └────────────────┘            │
            │                  ③事件投影（ACL）               │
            │  ④事件(UserRegistered)                        │
            │  ⑤事件(BadgeAwarded/ReputationChanged)         │
            └───────────────────────────────────────────────▶│
```

> 图例：实线箭头 = 依赖方向（箭头指向被依赖方）；粗线 = 客户-供应商（发布语言）；虚线 = 集成事件订阅。

## 2. 关系标注

| 关系 | 双方 | 模式 | 说明 |
| --- | --- | --- | --- |
| ① | co-identity → co-qa | 客户-供应商（Customer-Supplier）＋ 发布语言 | co-qa 是客户（下游），**只通过 co-identity-api 模块（发布语言）**查询用户、课程与课程角色；Identity 侧接口以开放主机服务（OHS）风格暴露 `IdentityApi`/`CourseApi`，禁止 Q&A 直连 Identity 的持久化表 |
| ② | co-qa → co-reputation | 客户-供应商 ＋ 集成事件（事件驱动） | Reputation **订阅** Q&A 的 `VoteCastEvent`、`AnswerAcceptedEvent`、`AnswerEndorsedEvent` 等记账与发徽章；当需要问题/回答上下文信息时，仅经 co-qa-api 发布语言查询，不反查 Q&A 数据库 |
| ③ | co-qa → co-discovery | 事件投影 ＋ 防腐层（ACL） | Discovery 是纯下游：订阅 `QuestionPostedEvent`/`QuestionEditedEvent`/`ContentDeletedEvent`/`CommentPostedEvent` 等，在防腐层（SearchIndexProjector/NotificationProjector）内把 Q&A 事件**翻译**为搜索文档（SearchDocument）与通知（Notification），不经 Q&A 的任何查询接口 |
| ④ | co-identity → co-reputation | 集成事件（事件驱动） | Reputation 订阅 `UserRegisteredEvent`：为新用户开户（声誉账户），教师角色开户发放 1000 悬赏积分；双方无同步调用 |
| ⑤ | co-reputation → co-discovery | 集成事件（事件驱动） | Discovery 订阅 `BadgeAwardedEvent`（徽章通知）与 `ReputationChangedEvent`（可选），仅做通知投影 |
| ⑥ | 全部上下文 ↔ co-shared-kernel | 共享内核（Shared Kernel） | 所有上下文共享：领域异常体系（DomainException/BusinessRuleException/ConflictException/ForbiddenException/NotFoundException，对应 1xxx/2xxx/3xxx/4xxx 错误码族）、分页模型（PageRequest/PageResult → `{items,total,page,size}`）、事件契约基类（IntegrationEvent/EventIds）、Actor/Role/UtcTime。共享内核变更必须经全团队评审 |
| ⑦ | co-bootstrap → 全部上下文 | 组合根（不属于上下文映射图，此处标注） | 装配全部模块：Spring 装配、安全过滤链（会话 + CSRF）、OutboxRelay 发件箱中继、定时任务；不包含业务逻辑 |

## 3. 边界规则（映射图强约束，由 ArchUnit 检查）

1. 跨上下文调用**只能**通过对方 `-api` 模块（发布语言）或集成事件；直连对方 domain/infrastructure 类 = 架构违规。
2. Q&A 是事件的**唯一业务源头**（声誉、发现都是下游），下游故障不得影响 Q&A 主流程（发件箱最终一致）。
3. 下游（Reputation/Discovery）**不回调**上游（Q&A）的写操作——采纳触发的悬赏结算是通过订阅 AnswerAcceptedEvent 完成的，而非 Q&A 反向调用 Reputation 服务。
4. 共享内核只放"所有人一致同意共享"的最小集合；新增共享类需全团队评审并在 README 记录。
