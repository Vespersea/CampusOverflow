package com.campusoverflow.discovery.domain.notification;

import com.campusoverflow.shared.domain.ForbiddenException;
import java.time.Instant;
import java.util.Objects;

/** 站内通知。(sourceEventId, recipientId) 唯一，事件重复投递不会产生重复通知。 */
public class Notification {

    private Long id;
    private final long recipientId;
    private final NotificationType type;
    private final String title;
    private final String link;
    private final String sourceEventId;
    private boolean read;
    private final Instant createdAt;

    private Notification(Long id, long recipientId, NotificationType type, String title, String link,
                         String sourceEventId, boolean read, Instant createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = Objects.requireNonNull(type);
        this.title = title;
        this.link = link;
        this.sourceEventId = Objects.requireNonNull(sourceEventId);
        this.read = read;
        this.createdAt = createdAt;
    }

    public static Notification create(long recipientId, NotificationType type, String title, String link,
                                      String sourceEventId, Instant now) {
        // 校验非空约束
        Objects.requireNonNull(type, "通知类型type不能为null");
        Objects.requireNonNull(sourceEventId, "sourceEventId 事件ID不能为空，用于防重复投递");
        Objects.requireNonNull(now, "创建时间now不能为空");

        // 新建通知默认未读 read = false
        return new Notification(null, recipientId, type, title, link, sourceEventId, false, now);
    }

    public static Notification reconstitute(Long id, long recipientId, NotificationType type, String title,
                                            String link, String sourceEventId, boolean read, Instant createdAt) {
        return new Notification(id, recipientId, type, title, link, sourceEventId, read, createdAt);
    }

    public void markRead(long actorId) {
        // 业务规则：只有通知接收人本人才能标记已读
        if (actorId != this.recipientId) {
            throw new ForbiddenException("FORBIDDEN_NOTIFICATION", "无权标记他人的通知为已读");
        }
        // 如果已经已读，直接返回，避免无效变更
        if (this.read) {
            return;
        }
        this.read = true;
    }

    public void assignId(long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public long recipientId() { return recipientId; }
    public NotificationType type() { return type; }
    public String title() { return title; }
    public String link() { return link; }
    public String sourceEventId() { return sourceEventId; }
    public boolean read() { return read; }
    public Instant createdAt() { return createdAt; }
}
