# 个人技能矩阵自评

> 对应《CampusOverflow 实验部分.pdf》Lab 1：
> - 【环境与前置准备】"完成 L1 课后的『个人技能矩阵』自评"
> - Step 1 团队组建"依据技能矩阵互补原则自由组队"
>
> 指导书只提到这个名称、**没有给模板**，下面这份表是按 Lab 1 的实际用途（选角色、定分工、查互补）自己设计的形式。

---

## 一、等级定义（先统一口径，四个人打分才可比）

| 等级 | 含义 | 判断标准 |
| --- | --- | --- |
| **L0 未接触** | 没学过、没用过 | 看到这个词不知道干什么 |
| **L1 了解** | 听过、看过、跟着别人做过 | 能照着教程一步一步抄下来 |
| **L2 会用** | 能独立完成常规任务 | 给需求能自己写完，卡住会查文档解决 |
| **L3 熟练** | 能独立排查问题、能教别人 | 出问题能定位原因，能给队友讲明白 |
| **L4 精通** | 能设计方案、做技术决策 | 能选型、能定规范、能带人搭架子 |

**打分只说真话。** 这一栏是给组队和分工用的，虚高会直接导致后面 Sprint 里接不住任务。

---

## 二、个人自评（吴超阳 / Dev A / @youngbv）

> 以下等级是按你的自我介绍（Java 基础、Spring Boot、MyBatis、Git、Maven、IDEA、Python、数据库基础）初拟的，**请按自己真实水平改，改完再提交**。

| # | 维度 | 具体技能点 | 自评等级 | 依据（我现在能独立做到什么） | 本学期目标 |
| --- | --- | --- | --- | --- | --- |
| 1 | 编程语言 | Java 基础（OOP、集合、异常、泛型） | L2 | 能写类、接口、集合操作，能读懂现有代码 | L3 |
| 2 | 编程语言 | Python | L1 | 能写脚本处理数据 | L2 |
| 3 | 后端框架 | Spring Boot（依赖注入、REST 接口、配置） | L2 | 能起一个能跑的服务、写 Controller/Service | L3 |
| 4 | 持久层 | MyBatis / JPA 映射与查询 | L2 | 能写 Mapper 和 SQL 映射 | L3 |
| 5 | 数据库 | MySQL 建表、基础 SQL、事务概念 | L2 | 能写建表语句和增删改查、多表连接 | L3 |
| 6 | 构建工具 | Maven 多模块、依赖管理 | L2 | 能读懂父 pom 与模块继承、跑通 build | L3 |
| 7 | 版本控制 | Git 基础（clone/branch/commit/push） | L2 | 能自己开分支、提交、推送 | L3 |
| 8 | 版本控制 | PR / Code Review / 冲突解决 | L1 | 跟着流程走过一次 PR，冲突没独立解过 | L3 |
| 9 | 测试 | JUnit 5 单元测试 | L1 | 能看懂测试代码，自己写还要查资料 | L3 |
| 10 | 测试 | Mockito 打桩、覆盖率报告解读 | L0 | 没写过 | L2 |
| 11 | 架构设计 | DDD 限界上下文、聚合/值对象 | L0 | 只听过名字 | L2 |
| 12 | 架构设计 | 六边形分层（domain/application/infrastructure/interfaces） | L0 | 没搭过 | L2 |
| 13 | 前端 | React / TypeScript | L1 | 能看懂页面代码，写要抄 | L2 |
| 14 | 容器化 | Docker 镜像、docker compose | L0 | 没装过 Docker | L3 |
| 15 | CI/CD | GitHub Actions 工作流 | L0 | 没写过 yml | L3 |
| 16 | 运维监控 | Prometheus / Grafana | L0 | 没接触 | L2 |
| 17 | 文档协作 | Markdown 写文档、写评审意见 | L2 | 能写结构化文档 | L3 |
| 18 | 敏捷流程 | Scrum 角色、看板、Sprint 节奏 | L1 | 知道概念，没实操过 | L2 |

**我的定位结论**：Java 后端方向 + 想补 DevOps（Docker/CI-CD），所以主责定为 **投票评论 → 悬赏 → 搜索推荐 + CI/CD**，其中 CI/CD 部分本学期从 L0 起步，需要边做边学。

---

## 三、团队汇总（四个人填完合并到这一张）

把每个人上面的表压缩成一张 5 列总表，一眼就能看出谁强谁弱、有没有短板没人兜。

| 维度 | PO | SM | Dev A（吴超阳） | Dev B | 团队最高 | 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| Java 基础 |  |  | L2 |  |  |  |
| Spring Boot |  |  | L2 |  |  |  |
| 持久层 / MySQL |  |  | L2 |  |  |  |
| 单元测试 |  |  | L1 |  |  |  |
| DDD / 六边形架构 |  |  | L0 |  |  |  |
| 前端 React/TS |  |  | L1 |  |  |  |
| Docker / compose |  |  | L0 |  |  |  |
| CI/CD |  |  | L0 |  |  |  |
| 文档 / 沟通 |  |  | L2 |  |  |  |

**怎么判断互补**：每个维度至少要有一个人 ≥ L2，否则这个维度就是团队短板，要在 Sprint 计划里明确"谁负责学 + 找谁求助"。

---

## 四、填完之后必须回答的三个问题

这份自评不是交上去就完事，Lab 1 Step 1 要求"依据技能矩阵互补原则组队"，所以填完要落三个结论：

1. **谁是 PO**：通常给需求理解能力强、能跟老师对齐验收标准的人（不一定技术最强）。
2. **谁是 SM**：通常给组织协调强、能盯流程和看板的人。
3. **Dev A / Dev B 怎么分**：按技能互补分。本项目建议：
   - Dev A 偏后端（Java/Spring Boot/持久层强的人）
   - Dev B 偏前端 + 测试兜底（React/TS 或测试强的人）
   - 如果两人后端都强、前端都弱，就约定"Dev B 主前端、Dev A 后端 + 支援前端"，并把"前端学习"写进 Sprint 1 任务。

---

## 五、放哪里、怎么提交

### 位置

放 `campus-overflow-student/docs/team/skill-matrix.md`（和 `charter-v1.0.md`、`hello-*.md` 同一个目录）。

> 若老师另有要求"提交到教学平台"，那就仓库留一份、教学平台传一份，两处都留档。

### 提交

```powershell
cd D:\Java\CampusOverflow
git checkout develop
git pull origin develop
git checkout -b feature/wuchaoyang-skill-matrix

# 文件放好后
git add campus-overflow-student/docs/team/skill-matrix.md
git commit -m "docs(team): 补充 Lab1 个人技能矩阵自评"
git push origin feature/wuchaoyang-skill-matrix
```

然后开 PR 到 `develop`，让队友 review 合并。

### 别忘了配套的那份

Lab 1 Step 1 还有一项 **《角色承诺书》签字提交**。你们仓库里目前是靠 `charter-v1.0.md` 第 7 节"签署区"用 commit 签名代替。建议确认一下：

- 老师认可"commit 签名 = 签署" → 章程那节就够了；
- 老师要求单独一份签字的承诺书 → 另建 `docs/team/role-commitment.md`，四人各签一次。

这件事建议直接在群里问老师，别猜。
