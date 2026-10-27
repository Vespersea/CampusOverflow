# Sprint 3 回顾纪要（Sprint Retrospective）

> 模板：Start / Stop / Continue ｜ 主持：李四（SM） ｜ 记录：赵六（QA） ｜ 日期：第 13 教学周

## 1. Sprint 目标回顾

- **承诺**：完成 Reputation（声誉、账本、徽章、悬赏、排行榜）与 Discovery（搜索投影、相关推荐、通知中心）上下文全部 TODO（71 个方法），核心特性端到端可演示（投票 → 采纳 → 声誉 → 搜索 → 通知）。
- **完成情况**：
  - 领域层：ReputationAccount/Bounty/ReputationRulebook/BadgePolicy/AcademicTerm、SearchQuery/MentionParser/Notification/RelatedQuestionPolicy 全部实现，领域单元测试 20 个全绿；
  - 应用层：声誉事件处理器（注册开户、采纳 +15、投票每日上限、徽章）、BountyService（发起/结算/到期退款）、LeaderboardService、ReconciliationService、NotificationProjector、SearchIndexProjector、DiscoveryQueryService 全部实现；
  - 后端 `mvn test` 全绿（含 ArchUnit 架构约束 7 条规则）；前端 typecheck / lint / build 全绿。

## 2. 数据

| 指标 | 数值 |
| --- | --- |
| 完成 TODO 方法 | 71 / 71（S3） |
| 新增领域单元测试 | 15（reputation）+ 5（discovery） |
| 架构约束检查 | ArchUnit 7 条规则通过 |
| 端到端集成测试 | 3 条（Testcontainers，本地 Docker 环境验证） |

## 3. Start / Stop / Continue 卡片（匿名投票后合并）

| 类别 | 卡片内容 | 得票 |
| --- | --- | --- |
| Start | 每个应用服务配手写 Test Double 单元测试（像 co-qa 的 QaTestFixtures 那样），不依赖 Spring 容器 | 4 |
| Start | 领域事件消费端一律先写幂等去重（账本唯一键 / ProcessedEventStore），再写业务逻辑 | 3 |
| Start | 复杂状态机（悬赏 OPEN→AWARDED/EXPIRED）先画状态图再写代码 | 2 |
| Stop | 直接修改他人上下文的表（违反 FF-2）；一律走 api 包或领域事件 | 4 |
| Stop | 等最后一天才跑 `mvn verify`，集成测试问题挤压到 Sprint 末尾 | 3 |
| Stop | 用 `UnsupportedOperationException` 抛 TODO 后不更新任务看板状态 | 2 |
| Continue | 红→绿→重构：先读测试当需求说明书，再写实现 | 4 |
| Continue | 每个 Task 完成后立即提 PR、队友 review + LGTM 后合并 | 3 |
| Continue | 事务边界内的"先写流水再改余额"，保证余额=流水之和（ADR-004） | 2 |

## 4. 投票选出的改进措施（纳入 Sprint 4）

1. **【覆盖率门禁】将 `domain.coverage.minimum` 提升到 0.70 并保持 `mvn verify` 全绿**——负责人：赵六。杜绝"写了实现不写测试"。
2. **【评审习惯】PR 必须附上"改了什么 / 为什么 / 怎么验证"三段式描述**——负责人：李四。降低评审上下文成本。
3. **【演示准备】每周五固定 30 分钟环境演练（compose 起停、演示数据回放）**——负责人：张三。避免 Sprint Review 现场翻车。

## 5. 下个 Sprint（Sprint 4）行动项

- [ ] 前端增强：悬赏对话框与倒计时、列表骨架屏与错误重试（已完成原型，待体验打磨）
- [ ] SonarQube 质量门禁接入 CI；补 Sonar 扫描出的 Bugs / Smells
- [ ] 应用层测试补齐 reputation / discovery 的 Test Double 用例，覆盖率提到 70%+
- [ ] 压测（提问接口）并观察 p95、发件箱积压等指标；配置 Grafana 告警
- [ ] 撰写《运维手册 v1.0》并做一次全流程部署演练
