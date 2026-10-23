# language: zh-CN
功能: 全文搜索问题
  作为学生
  我想要用中文关键词全文搜索问题（每词至少 2 字符、最多 5 词）
  以便快速检索历史问答

  背景:
    假如 搜索投影中已索引以下问题:
      | 问题      | 标题                         | 标签         |
      | Q1        | Java 集合框架中 List 的区别  | java         |
      | Q2        | 数据库索引失效的场景         | 数据库       |
      | Q3        | 软件工程课程设计怎么做       | 软件工程     |

  场景: 快乐路径——中文关键词命中（MySQL ngram 全文检索）
    当 学生请求 GET /api/v1/search/questions?q=集合 框架
    那么 系统返回 200 成功
    并且 结果按相关度排序（RELEVANCE），Q1 排在最前
    并且 分页结构为 {items,total,page,size}

  场景: 边界路径——多个关键词最多取前 5 个词项
    当 学生请求 GET /api/v1/search/questions?q=java 集合 框架 数据库 索引 失效 场景（7 个词项）
    那么 系统返回 200 成功
    并且 系统只使用前 5 个词项参与检索（多余词项被忽略）

  场景: 边界路径——组合筛选（课程/标签/未回答/悬赏）
    当 学生请求 GET /api/v1/search/questions?q=索引&courseId=C1&tag=数据库&unansweredOnly=true
    那么 系统返回 200 成功
    并且 结果同时满足关键词、课程、标签与未回答条件

  场景: 异常路径——单个关键词不足 2 字符
    当 学生请求 GET /api/v1/search/questions?q=J
    那么 系统返回 422 业务规则错误
    并且 业务错误码为 "DIS-1001"（每个关键词至少 2 个字符，ngram_token_size=2）

  场景: 异常路径——关键词超过 50 字符
    当 学生请求 GET /api/v1/search/questions?q=<60 个字符的连续关键词>
    那么 系统返回 422 业务规则错误
    并且 业务错误码为 "DIS-1002"（关键词不能超过 50 个字符）

  场景: 异常路径——无结果时返回空列表
    当 学生请求 GET /api/v1/search/questions?q=不存在的关键词
    那么 系统返回 200 成功
    并且 "items" 为空列表，"total" 为 0
