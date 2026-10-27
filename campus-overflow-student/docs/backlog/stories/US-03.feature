# language: zh-CN
功能: 浏览问题列表与筛选
  作为访客或学生
  我想要按课程与分页浏览问题列表
  以便快速找到自己关心的问题

  背景:
    假如 系统中存在课程 C1 与 C2
    并且 课程 C1 下有 25 个未删除问题，课程 C2 下有 3 个未删除问题

  场景: 快乐路径——默认按最新优先返回第一页
    当 访客请求 GET /api/v1/questions（不带任何参数）
    那么 系统返回 200 成功
    并且 分页结构为 {items,total,page,size}，其中 "page" 为 1、"size" 为 20、"total" 为 28
    并且 "items" 按创建时间倒序排列且只含 20 条

  场景: 边界路径——按课程过滤
    当 访客请求 GET /api/v1/questions?courseId=C2
    那么 系统返回 200 成功
    并且 "total" 为 3
    并且 "items" 中每条问题的课程均为 C2

  场景: 边界路径——翻页到超出范围时返回空列表
    当 访客请求 GET /api/v1/questions?page=99&size=20
    那么 系统返回 200 成功
    并且 "items" 为空列表，而 "total" 仍为 28

  场景: 异常路径——分页参数非法
    当 访客请求 GET /api/v1/questions?page=0
    那么 系统返回 422 业务规则错误
    并且 业务错误码以 "QA-1" 开头（QA-1xxx 业务规则）
