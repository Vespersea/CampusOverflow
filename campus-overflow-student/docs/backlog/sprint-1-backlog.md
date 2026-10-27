# Sprint 1 待办列表（Sprint 1 Backlog）

> Sprint 目标：打通 Walking Skeleton——一位新注册的学生能够"注册 → 浏览问题列表 → 发布问题 → 提交回答"。
> Sprint 时长：2 周（第 5–6 教学周） ｜ 团队速率预估：15 SP（上一 Sprint 为章程与需求建模，本次按 4 人 × 2 周估算）
> 承诺故事：4 条（12 SP）+ 1 条拉伸（3 SP） ｜ 验收场景文件：`docs/backlog/stories/*.feature`

## 1. Sprint 目标

- **S1-G1**：完成身份（注册）与问答核心（列表/提问/回答）的最小闭环，前端能跑通演示脚本。
- **S1-G2**：建立六边形分层的代码骨架（6 个 Maven 模块、ArchUnit 约束、CI 管线）并让 CI 全绿。

## 2. 承诺的故事

| 故事 | 标题 | SP | 负责人 | 验收标准（Gherkin） |
| --- | --- | --- | --- | --- |
| US-01 | 学生注册 | 3 | wangwu | docs/backlog/stories/US-01.feature |
| US-03 | 浏览问题列表与筛选 | 5 | wangwu | docs/backlog/stories/US-03.feature |
| US-04 | 发布问题 | 5 | zhangsan（PO 兼任开发） | docs/backlog/stories/US-04.feature |
| US-05 | 提交回答 | 5 | wangwu | docs/backlog/stories/US-05.feature |
| US-02（拉伸） | 登录与会话保持 | 3 | wangwu | docs/backlog/stories/US-02.feature |

## 3. 任务拆分

### US-01 学生注册（3 SP，负责人 wangwu，验收见 US-01.feature）

| 任务 | 估算 | 负责人 |
| --- | --- | --- |
| co-identity-domain：User 聚合 + PasswordPolicy/PasswordHasher 值对象与单测 | 0.5 d | wangwu |
| co-identity-application/interfaces：注册命令服务 + `POST /api/v1/auth/register`（返回 201 与 id） | 0.5 d | wangwu |
| 密码哈希与唯一性冲突（ID-2xxx）错误映射、集成测试（Testcontainers） | 0.5 d | zhaoliu |

### US-03 浏览问题列表与筛选（5 SP，负责人 wangwu，验收见 US-03.feature）

| 任务 | 估算 | 负责人 |
| --- | --- | --- |
| co-qa-domain：Question 聚合与 QuestionRepository 仓储接口 | 1 d | wangwu |
| co-qa-infrastructure：JpaQuestionEntity + RepositoryAdapter（含软删除查询，ADR-006） | 1 d | wangwu |
| co-qa-interfaces：`GET /api/v1/questions?courseId=&page=&size=`，分页结构 {items,total,page,size} | 0.5 d | zhaoliu |
| 演示数据写入（profile=demo）与分页边界测试 | 0.5 d | zhaoliu |

### US-04 发布问题（5 SP，负责人 zhangsan，验收见 US-04.feature）

| 任务 | 估算 | 负责人 |
| --- | --- | --- |
| 值对象 Title（10–150 字符）/MarkdownBody（1–30000）/Tags（1–5 个，TagName 规则）与单测 | 1 d | zhangsan |
| `POST /api/v1/questions` 命令服务与 QuestionPostedEvent 发布（发件箱同事务写入） | 1 d | wangwu |
| 富文本安全（Markdown 渲染 + OWASP 消毒）与契约生成（springdoc） | 0.5 d | zhangsan |

### US-05 提交回答（5 SP，负责人 wangwu，验收见 US-05.feature）

| 任务 | 估算 | 负责人 |
| --- | --- | --- |
| Answer 实体挂入 Question 聚合，`POST /api/v1/questions/{id}/answers` | 1 d | wangwu |
| 问题详情查询视图（含回答列表） | 1 d | wangwu |
| 并发计数（回答数由原子 UPDATE 维护，ADR-008）与边界测试 | 0.5 d | zhaoliu |

### US-02 登录（拉伸，3 SP，负责人 wangwu，验收见 US-02.feature）

| 任务 | 估算 | 负责人 |
| --- | --- | --- |
| `POST /api/v1/auth/login`（学号/工号 + 密码，Redis 服务端会话，ADR-003） | 0.5 d | wangwu |
| CSRF 令牌（`GET /api/v1/auth/csrf`）与登录限流（1 分钟锁定） | 0.5 d | wangwu |
| `/api/v1/auth/me` 与前端对接 | 0.5 d | zhangsan |

## 4. Sprint 1 完成定义（DoD 摘要）

- 4 条承诺故事全部通过各自 Gherkin 场景的自动化测试；US-02 若未完成，在评审会上说明并移入 Sprint 2。
- 领域层单测毫秒级通过；ArchUnit 约束测试（co-bootstrap 的 ArchitectureTest）通过。
- Swagger UI（/swagger-ui.html）可看到全部 Sprint 1 接口的契约。
- 前端可用演示脚本完成"注册 → 列表 → 提问 → 回答"闭环。

## 5. 风险与应对

| 风险 | 应对 |
| --- | --- |
| 六边形分层样板代码量大（每聚合约 40 行映射） | 由 wangwu 先做出 Question 端到端模板，其余照抄 |
| 团队对 Markdown 渲染安全不熟 | 使用 commonmark + OWASP sanitizer 并写消毒测试 |
| CSRF/会话调试耗时 | zhaoliu 提前写 Postman 集合（docs/postman/CampusOverflow.postman_collection.json）固化调试脚本 |
