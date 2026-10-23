# 团队代码评审清单 v1.0

> 适用范围：CampusOverflow 全部 PR（后端 Java + 前端 TypeScript）。评审流程：作者自查 → 1 名队友 review（5 条以上实质意见）→ LGTM → SM 合并。本清单在 Sprint 3 评审实战中沉淀，每 Sprint 回顾会后可修订（版本 +0.1）。

## 0. 评审流程

1. PR 描述必须含**三段式**：改了什么 / 为什么 / 怎么验证（测试命令与结果）。
2. 评审者不能是作者本人；每条意见必须给出**具体位置与可操作建议**，禁止"这里不好"式空评。
3. 发现以下问题时直接请求修改（Request Changes）并附本清单对应条目编号：
   - 违反架构约束（FF-1~FF-6，见 §1）；
   - 缺少对应测试或删改既有测试断言；
   - 存在安全风险（§4）。

## 1. 正确性（Correctness）

- [ ] 领域不变量是否在聚合根内保护？（Set 私有、通过领域方法变更；禁止贫血模型）
- [ ] 跨上下文协作是否只经对方 api 包或集成事件？（FF-2，禁止直接读写他人上下文的表）
- [ ] 消费端幂等是否落实？（账本唯一键 (eventId,userId,reason) / processed_event 表 / 通知 (sourceEventId,recipientId)）
- [ ] 事务边界是否正确？（余额与流水同一事务；事件经发件箱在事务内写入）
- [ ] 错误码是否符合 `docs/错误码.md` 分段约定（1xxx 业务 422 / 2xxx 冲突 409 / 3xxx 禁止 403 / 4xxx 不存在 404）？
- [ ] 并发与边界条件（悬赏状态机终态不可变、每日上限截断、分页边界 page≥1、size≤50）是否覆盖？

## 2. 可读性（Readability）

- [ ] 类/方法名与统一语言术语表一致？（见 `docs/storming/ubiquitous-language-v1.0.md`）
- [ ] 领域方法是否"说业务语言"（accept、freezePoints），而非 setStatus("AWARDED")？
- [ ] 魔法数字是否提取为常量（如 `DAILY_VOTE_CAP = 200`）？
- [ ] 是否存在注释解释"怎么做"（应删）而非"为什么"（应留）？

## 3. 可测性（Testability）

- [ ] 领域逻辑是否有对应单元测试（先读测试再写实现；测试即规格）？
- [ ] 应用服务是否只依赖端口接口，可用手写 Test Double 在毫秒级测试？（不得要求 Spring 容器）
- [ ] 是否存在"无效断言"（只断言非空、不断言行为）？
- [ ] 副作用调用（事件发布、审计、缓存失效）是否用 ArgumentCaptor / Recording Test Double 验证？
- [ ] 测试是否覆盖快乐路径 + 边界路径 + 异常路径（对应 Gherkin 场景）？

## 4. 安全（Security）

- [ ] 用户输入是否经领域校验（标题/正文/标签/关键词长度与格式）？前端校验不能代替后端校验
- [ ] 权限判断是否在领域层/应用层（Actor 显式传入），而非仅靠接口层？
- [ ] Markdown 输出是否经 commonmark + OWASP Sanitizer 净化（防 XSS）？
- [ ] 是否硬编码密钥/密码？（一律走环境变量与 Secrets；本地值仅限 dev 默认）
- [ ] 日志是否泄露敏感信息（密码、会话 token、完整邮箱需脱敏）？
- [ ] 反序列化事件类型是否白名单校验（OutboxRelay 已做，勿绕过）？

## 5. 性能（Performance）

- [ ] 列表/批量场景是否避免 N+1？（IdentityApi.findUsers / ReputationApi.reputations 批量接口）
- [ ] 高冲突更新是否用差量调整（adjustScore/adjustAnswerCount）而非整表重算？
- [ ] 新索引/SQL 是否考虑 ngram 全文索引与表前缀（rep_/qa_/dis_/id_）约定？
- [ ] 读多写少的数据是否走读模型（搜索投影、Redis 排行榜）而非实时聚合？

## 6. 风格（Style）

- [ ] 后端：四空格缩进、与邻近代码风格一致、无 TODO/调试代码残留、import 无未使用项
- [ ] 前端：`npm run lint` 与 `npm run typecheck` 全绿；无 `any` 逃逸；服务端状态与客户端状态区分使用
- [ ] 表名/实体名使用本上下文前缀（FF-6）；控制器位于 interfaces 层（FF-4）
- [ ] PR 只包含一个任务的改动（小步提交，禁止夹带无关重构）

## 附：常见违规示例（评审实战沉淀）

| 反模式 | 问题 | 正确做法 |
| --- | --- | --- |
| `entity.setStatus("AWARDED")` | 绕过状态机，破坏聚合不变式 | `bounty.award(answerId, winnerId, now)` |
| 在 handler 里直接 `UPDATE rep_account` | 跨上下文直写他人数据 | 订阅领域事件，走本上下文仓储 |
| 事件处理器不做去重 | 至少一次投递下重复加分 | 账本唯一键 / ProcessedEventStore |
| `assertThat(result).isNotNull()` | 无效断言 | 断言具体字段与行为 |
| 把积分当声誉扣减 | 破坏"余额=流水之和" | 冻结积分模型（ADR-009） |
