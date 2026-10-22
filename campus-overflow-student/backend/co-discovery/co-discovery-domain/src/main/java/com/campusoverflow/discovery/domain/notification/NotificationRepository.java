package com.campusoverflow.discovery.domain.notification;

import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    boolean exists(String sourceEventId, long recipientId);

    // 分页查询接收人的通知
    PageResult<Notification> findByRecipient(long recipientId, PageRequest page);

    // 统计未读数量
    long countUnread(long recipientId);

    // 批量标记已读：返回更新行数int，和jpa返回值对齐
    int markAllRead(long recipientId);
}