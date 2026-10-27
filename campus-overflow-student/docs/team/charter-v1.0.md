# 六边形小队 团队章程 v1.0

> 本文档是团队协作的"宪法"。任何成员对约定有异议，先在回顾会上提出并达成共识后再修订，修订后版本号 +0.1。

## 1. 团队信息

| 项 | 内容 |
| --- | --- |
| 团队名 | 六边形小队（Hexagon Squad） |
| 项目名 | CampusOverflow 校园问答平台 |
| 团队人数 | 4 人 |
| 章程版本 | v1.0 |
| 生效日期 | 第 5 教学周 |

## 2. 成员与角色

| 昵称 | 姓名 | 角色 | 主要职责 |
| --- | --- | --- | --- |
| zhangsan | 张三 | PO（Product Owner） | 需求梳理、Product Backlog 优先级、Gherkin 验收标准、Sprint Review 验收 |
| lisi | 李四 | SM（Scrum Master） | 站会/计划会/回顾会组织、CI 维护、DoD 检查、消除障碍 |
| wangwu | 王五 | Developer | co-qa 问答核心上下文、co-bootstrap 装配与安全、ArchUnit 约束 |
| zhaoliu | 赵六 | Developer + QA | co-reputation 声誉上下文、co-discovery 发现上下文、Testcontainers 集成测试、覆盖率门禁 |

## 3. 团队价值观

1. **契约优先**：接口先改 OpenAPI 契约再改实现（ADR-007），文档与代码同 PR 提交。
2. **领域纯粹**：领域层是纯 Java，业务规则集中在领域对象里（ADR-008），不允许"Controller 里写 if 业务"。
3. **可验证**：每个用户故事必须有 Gherkin 验收场景，每条业务规则必须有对应测试。
4. **透明**：遇到阻塞当天在群里说，不捂到站会。
5. **互相兜底**：评审不是挑刺，是帮队友避免答辩现场翻车。

## 4. 沟通约定

| 项 | 约定 |
| --- | --- |
| IM 群 | 班级微信群（日常讨论）+ 仓库 Issue（技术决策记录） |
| Daily Standup | 每周一、三、五 20:30，线上语音 15 分钟；每人三句话：昨天做了什么、今天做什么、有什么阻塞 |
| 响应时限 | 群消息 4 小时内回复；PR 评审 24 小时内完成首轮评审 |
| Sprint 节奏 | Sprint 长度 2 周；计划会周一晚，回顾会 + 评审会周五晚（Sprint 最后一天） |
| 决策方式 | 技术分歧先在 Issue 讨论，仍无法达成一致则按 ADR 流程记录决策，由 PO 拍板 |

## 5. 开发规约

### 5.1 分支模型

- `main`：随时可部署的分支，**禁止直推**，只接受 PR 合入。
- `feature/*`：功能分支，从 `main` 拉出，命名如 `feature/qa-vote`、`feature/reputation-bounty`。
- `fix/*`：缺陷修复分支；`docs/*`：文档类变更分支（仅限文档时可直接合入，但同样需要 1 人评审）。

### 5.2 Commit Message 规范

遵循 Conventional Commits 风格，格式：`<type>(<scope>): <中文或英文简述>`，例如：

```
feat(qa): 完成问答核心子域(S2)领域层与应用层实现
fix(reputation): 修正每日投票上限跨日未重置的问题
docs(adr): 新增 ADR-009 投票声誉每日上限
test(discovery): 补充搜索投影事件消费测试
refactor(qa): 抽取 Vote 聚合的改投增量计算
```

type 仅限：`feat` / `fix` / `docs` / `test` / `refactor` / `chore`。

### 5.3 PR 评审规则

- 每个 PR 必须至少 1 名非作者成员 Review 并给出 **LGTM** 后才能合入；PO 与 QA 的 PR 相互评审。
- PR 描述必须写明：改动目的、关联用户故事（如 US-06）、验收截图或测试结果。
- 评审意见必须可操作（指出文件与行为），禁止只写"再改改"。
- 合入方式：Squash Merge，保持 `main` 历史整洁。

## 6. Definition of Done（DoD 初版 v1.0）

一条用户故事只有同时满足以下全部条件才算"完成"：

- [ ] 实现代码已合入 `main`（经评审 + LGTM，PR 中附评审记录）
- [ ] 故事对应的 Gherkin 场景全部有自动化测试覆盖（单元测试 + 必要的集成测试），CI 全绿
- [ ] 领域层行覆盖率不低于门禁（FF-4，当前 0，Sprint 4 前提升到 0.80）
- [ ] 通过 ArchUnit 架构约束检查（分层依赖不越界）
- [ ] 契约变更已同步到 springdoc 生成的 OpenAPI（Swagger UI 可见），前端对接字段已确认
- [ ] 错误码遵循约定（XXX-1xxx 业务规则 422 / XXX-2xxx 冲突 409 / XXX-3xxx 禁止 403 / XXX-4xxx 不存在 404，新代码用 REP- / DIS- 前缀）
- [ ] 无已知未关闭的阻塞性 Bug
- [ ] 相关 ADR / 术语表 / 运维手册如有变化已同步更新

## 7. 签署区

本团队成员通过各自在 `main` 分支上的 commit 签署本章程（每位成员以 `chore(charter): 签署团队章程 v1.0` 提交一次空变更即视为签署）。

| 成员 | 角色 | 签署方式 |
| --- | --- | --- |
| 张三（zhangsan） | PO | commit 签名 |
| 李四（lisi） | SM | commit 签名 |
| 王五（wangwu） | Developer | commit 签名 |
| 赵六（zhaoliu） | Developer + QA | commit 签名 |
