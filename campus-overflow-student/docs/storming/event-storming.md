# 事件风暴工作坊记录（Event Storming）

> 场次：第 5 教学周周三晚 20:00–22:30（2.5 小时） ｜ 地点：线上白板（Miro） ｜ 参与人：六边形小队全员 4 人
> 记录人：张三（PO） ｜ 复核人：赵六（QA）
> 颜色约定（按 DDD 社区惯例）：橙色 = 领域事件；蓝色 = 命令；黄色 = 角色/外部系统；绿色 = 读模型；紫色 = 策略/规则；红色 = Hot Spot。

## 1. 领域事件清单（橙色，按时间线组织）

> 全部使用过去时命名。事件名与代码中事件类名（如 `QuestionPostedEvent`）一一对应。

| # | 领域事件 | 触发命令 | 触发角色 | 消费方/读模型 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 1 | UserRegistered | RegisterUser | 访客（学生） | Reputation：开户（教师发 1000 悬赏积分）；Identity 用户读模型 | 注册成功即发 |
| 2 | UserLoggedIn | Login | 学生/教师/助教/管理员 | 审计日志 | 会话由 co-bootstrap 维护（ADR-003），本事件仅用于审计 |
| 3 | QuestionPosted | PostQuestion | 学生 | Discovery：搜索投影新增文档；QA 问题列表读模型 | 与问题写入同事务进发件箱 |
| 4 | QuestionEdited | EditQuestion | 提问者 | Discovery：搜索投影更新文档 | 仅作者可编辑 |
| 5 | ContentDeleted | DeleteQuestion/DeleteAnswer | 作者或管理员 | Discovery：搜索投影移除/标记文档 | 软删除（ADR-006） |
| 6 | AnswerSubmitted | SubmitAnswer | 学生 | Discovery：通知"收到新回答"；QA 详情读模型 | 触发 FIRST_ANSWER 徽章检查 |
| 7 | CommentPosted | PostComment | 学生 | Discovery：@提及通知（最多 5 人） | 评论可挂问题或回答 |
| 8 | UserMentioned | PostComment（@解析） | 系统 | Discovery：MENTIONED 通知 | 由 MentionParser 从评论解析 |
| 9 | VoteCast | CastVote/ChangeVote（value ∈ {1,−1,0}） | 学生 | Reputation：声誉记账（+10/−2/增量）；QA 分数读模型 | 改投/撤销按"新效果 − 旧效果"计增量 |
| 10 | AnswerAccepted | AcceptAnswer | 提问者或课程教师/助教 | Reputation：+15（自问自答 0）与悬赏结算；Discovery：通知 + 搜索投影更新 | 一个采纳触发多个下游 |
| 11 | AnswerEndorsed | EndorseAnswer | 课程教师 | Reputation：TEACHER_ENDORSED 徽章；Discovery：通知 | 教师认证回答 |
| 12 | BountyOpened | OpenBounty | 课程管理者（教师/助教/管理员） | Discovery：通知问题作者 | 发起即冻结积分（10–500，1–14 天） |
| 13 | BountyAwarded | AcceptAnswer（含悬赏问题） | 系统（采纳触发） | Reputation：结算积分给胜者（BOUNTY_AWARDED 账本）；Discovery：通知 | 发起人自己的回答被采纳 → 改发 BountyClosed |
| 14 | BountyClosed | CloseBounty（到期任务/自答采纳） | 系统定时任务 | Reputation：退款给发起人 | 到期未采纳自动关闭退款 |
| 15 | ReputationChanged | ChangeReputation（系统内部） | Reputation 上下文 | Discovery：通知（可选）；Reputation 排行榜/档案读模型 | 账本只追加（eventId,userId,reason 幂等） |
| 16 | BadgeAwarded | AwardBadge（徽章策略检查） | 系统（事件驱动） | Discovery：通知"获得徽章" | 6 种徽章见徽章策略 |
| 17 | NotificationCreated | CreateNotification | Discovery 通知服务 | 通知读模型（未读数） | 幂等键 (sourceEventId, recipientId) |
| 18 | NotificationRead | MarkNotificationRead | 通知接收者 | 通知读模型（未读数） | 仅本人可读 |
| 19 | QuestionIndexed | IndexQuestion（投影） | Discovery 搜索投影 | 搜索读模型（MySQL ngram 索引） | 消费 QuestionPosted/Edited/ContentDeleted 后产出 |
| 20 | SearchQueryPerformed | SearchQuestions | 学生 | 搜索结果视图 | 读操作，不产生领域事件（仅用于审计与热度统计） |
| 21 | BountyExpired | 到期扫描任务（发现到期悬赏） | 系统定时任务 | Reputation：转为 BountyClosed | 状态机 OPEN → EXPIRED |
| 22 | ReputationReconciled | 每日对账任务 | 系统定时任务 | 对账指标 co_reputation_mismatches | 余额 = 流水之和，不一致告警 |
| 23 | CourseJoined | JoinCourse | 教师/管理员 | Identity 课程成员读模型 | 决定课程管理权限（教师/助教） |
| 24 | QuestionViewed | ViewQuestion | 访客/学生 | 浏览量计数器 | 由原子 UPDATE 维护，不发事件（计数旁路） |
| 25 | VoteRejected | CastVote（命中每日上限/自投） | 系统 | 审计 | 超出每日上限 200 的增量被截断 |

## 2. 按时间线的事件流（主流程）

```
访客 ──RegisterUser──▶ UserRegistered ──▶ 声誉账户开户（教师 +1000 悬赏积分）
学生 ──Login──▶ UserLoggedIn
学生 ──PostQuestion──▶ QuestionPosted ──▶ QuestionIndexed ──▶ 搜索读模型
学生 ──SubmitAnswer──▶ AnswerSubmitted ──▶ 通知（新回答）＋ FIRST_ANSWER 徽章检查
学生 ──PostComment──▶ CommentPosted ──▶ UserMentioned ──▶ MENTIONED 通知（≤5 人）
学生 ──CastVote──▶ VoteCast ──▶ ReputationChanged（+10/−2，每日上限 200）
提问者 ──AcceptAnswer──▶ AnswerAccepted ──▶ ReputationChanged(+15) ＋ BountyAwarded/BountyClosed ＋ 通知 ＋ 投影更新
教师 ──EndorseAnswer──▶ AnswerEndorsed ──▶ BadgeAwarded(TEACHER_ENDORSED) ──▶ NotificationCreated
管理者 ──OpenBounty──▶ BountyOpened（冻结积分）
定时任务 ──扫描到期──▶ BountyExpired ──▶ BountyClosed（退款）
系统 ──徽章策略──▶ BadgeAwarded ──▶ NotificationCreated
```

## 3. Hot Spot（红色，工作坊遗留问题与决议）

| # | Hot Spot | 讨论决议 |
| --- | --- | --- |
| H-1 | 声誉反作弊：小号互相刷赞怎么办？ | 每人每 UTC 自然日因被投票获得的正向声誉上限 200（ReputationRulebook.DAILY_VOTE_CAP）；超出部分截断并审计（见 ADR-009） |
| H-2 | 事件至少一次投递导致重复消费怎么办？ | 消费端幂等：声誉账本唯一键 (eventId,userId,reason)；通知唯一键 (sourceEventId,recipientId)；投影用 processed_event 表（ADR-002/004） |
| H-3 | 自问自答采纳是否给分？ | 不给（acceptedDelta 返回 0），防止刷分 |
| H-4 | 改投/撤销如何计声誉？ | 按"新效果 − 旧效果"计增量（voteDelta），负向增量不受每日上限约束 |
| H-5 | 悬赏发起后积分在哪？ | 冻结模型：发起即从余额扣减并冻结，采纳结算给胜者，到期/自答采纳退款（ADR-009） |
| H-6 | 每日上限的"一天"如何界定？ | UTC 自然日（与全库 UTC 时区一致），跨日自动重置 |
| H-7 | 中文搜索分词 | MySQL ngram 全文索引（ngram_token_size=2），单关键词至少 2 字符（ADR-005） |
| H-8 | 声誉延迟可见性 | 发件箱异步投递 → 最终一致，数百毫秒延迟，需在演示与文档中说明 |

## 4. 系统边界事件（外部系统，黄色）

| 外部系统 | 事件 | 处理 |
| --- | --- | --- |
| 邮件服务（外部） | 账户验证邮件 / 通知邮件 | 本课程**不实现**，仅预留 NotificationCreated 之后的通知渠道适配点；答辩时以站内通知替代 |
| OAuth 第三方登录（微信/统一身份认证） | OAuthCallback | 本课程**不实现**，学号+密码登录已满足课程要求；Identity 上下文保留扩展点 |
| MySQL | 数据持久化 | 唯一约束兜底（投票、账本、通知幂等键） |
| Redis | 会话（ADR-003）、排行榜、登录限流 | 排行榜可重建（以声誉账户为事实源） |
| Prometheus / Grafana | 指标抓取 | co_outbox_backlog、co_reputation_mismatches 等指标告警 |

## 5. 工作坊产出的聚合候选

见 `docs/storming/aggregates.md`（User/Course、Question、Vote、Comment、ReputationAccount、Bounty、BadgeAward、Notification、SearchDocument）。
