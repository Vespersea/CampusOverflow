# 统一语言术语表 v1.0（Ubiquitous Language）

> Lab 3 产出物。领域专家、PO 与全体开发者共同确认。代码中的类名、方法名、事件名必须与下表术语一致；如引入新术语，先在事件风暴工作坊中讨论并更新本表。

| # | 中文术语 | 英文术语 | 定义 | 所属上下文 |
| --- | --- | --- | --- | --- |
| 1 | 问题 | Question | 用户在平台上提出的、可被回答与投票的内容，含标题、Markdown 正文与标签 | Q&A |
| 2 | 回答 | Answer | 用户对某个问题提交的解答，可被投票、采纳或教师认证 | Q&A |
| 3 | 评论 | Comment | 用户对问题或回答发表的简短补充，可包含 @提及 | Q&A |
| 4 | 投票 | Vote | 用户对问题/回答表达的赞(+1)或踩(−1)，可改投与撤销(0) | Q&A |
| 5 | 采纳 | Accept | 提问者或课程管理者将某个回答标记为最佳答案 | Q&A |
| 6 | 最佳答案 | Accepted Answer | 被采纳的回答；每个问题至多一个，采纳后问题视为已解决 | Q&A |
| 7 | 教师认证 | Endorsement | 课程教师对回答的权威性背书，授予"名师认可"标记 | Q&A |
| 8 | 标签 | Tag | 用于分类与检索的短关键词，每个问题 1–5 个 | Q&A |
| 9 | 标题 | Title | 问题的简要概括，长度 5–100 字符 | Q&A |
| 10 | 正文 | Markdown Body | 问题/回答的内容，以 Markdown 编写，展示前经净化 | Q&A |
| 11 | 课程 | Course | 平台内的教学班级单元，问题是可选关联课程 | Identity |
| 12 | 课程管理者 | Course Manager | 对课程内内容有权管理（采纳、认证、发起悬赏）的教师/助教 | Identity |
| 13 | 学生 | Student | 注册后提问、回答、投票的普通用户 | Identity |
| 14 | 教师 | Teacher | 可管理课程内容、认证回答、发起悬赏的用户，开户发放悬赏积分 | Identity |
| 15 | 助教 | TA | 与教师同权限的课程管理者角色 | Identity |
| 16 | 管理员 | Admin | 平台级管理角色，可查看任意声誉流水与对账结果 | Identity |
| 17 | 角色 | Role | 全局 RBAC 角色：STUDENT / TA / TEACHER / ADMIN | Identity |
| 18 | 声誉 | Reputation | 用户在社区贡献的量化评价，等于其账本流水增量之和 | Reputation |
| 19 | 声誉账本 | Reputation Ledger | 只追加的声誉流水表，每条记录一次加减分（ADR-004） | Reputation |
| 20 | 悬赏积分 | Bounty Points | 与声誉不同的"货币"，教师开户发放，用于发起悬赏 | Reputation |
| 21 | 冻结积分 | Frozen Points | 发起悬赏时从可用积分中冻结的部分，结算转出或到期退回 | Reputation |
| 22 | 悬赏 | Bounty | 课程管理者用积分吸引高质量回答的机制，状态机 OPEN→AWARDED/EXPIRED | Reputation |
| 23 | 徽章 | Badge | 达成条件后授予的成就标识（如 FIRST_ACCEPTED、HELPER_10） | Reputation |
| 24 | 排行榜 | Leaderboard | 按周/月/学期/总榜展示的声誉排名 | Reputation |
| 25 | 学期 | Academic Term | 校历学期（2–7 月春季、8–12 月与次年 1 月秋季），用于排行榜周期键 | Reputation |
| 26 | 对账 | Reconciliation | 定时校验"账户余额 = 流水之和"，不一致数以指标暴露（FF-8） | Reputation |
| 27 | 搜索投影 | Search Projection | 由领域事件折叠出的只读文档（dis_search_doc），供搜索与筛选 | Discovery |
| 28 | 全文检索 | Full-text Search | 基于 MySQL ngram（ngram_token_size=2）的中文关键词检索（ADR-005） | Discovery |
| 29 | 相关推荐 | Related Questions | 按标签集合 Jaccard 相似度推荐的问题列表 | Discovery |
| 30 | 通知 | Notification | 站内事件提醒（新回答、被采纳、被提及、悬赏到账、徽章） | Discovery |
| 31 | 已读 | Read | 通知的阅读状态；只有接收者本人可标记 | Discovery |
| 32 | @提及 | Mention | 评论中以 @昵称 提醒他人的机制，每条评论最多 5 人 | Discovery |
| 33 | 限界上下文 | Bounded Context | 拥有独立领域模型与数据所有权的业务边界 | 全局 |
| 34 | 聚合 | Aggregate | 一致性边界，由根实体与内部实体/值对象组成 | 全局 |
| 35 | 领域事件 | Domain Event | 以过去时命名的业务事实（QuestionPosted、AnswerAccepted…） | 全局 |
| 36 | 集成事件 | Integration Event | 跨上下文通信的发布语言，经事务性发件箱至少一次投递 | 全局 |
| 37 | 发件箱 | Outbox | 与业务数据同事务写入、由中继异步投递事件表（ADR-002） | 全局 |
| 38 | 发布语言 | Published Language | 各上下文 api 包对外暴露的稳定契约（查询接口与事件） | 全局 |
| 39 | 读模型 | Read Model | 面向查询优化、可随时重建的投影（搜索文档、排行榜、热门标签） | 全局 |
| 40 | 软删除 | Soft Delete | 用户内容删除后保留历史、仅对外不可见（ADR-006） | Q&A |
