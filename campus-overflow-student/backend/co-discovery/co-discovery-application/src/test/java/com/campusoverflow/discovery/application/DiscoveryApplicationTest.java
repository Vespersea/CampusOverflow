package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.discovery.domain.notification.Notification;
import com.campusoverflow.discovery.domain.notification.NotificationRepository;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchPort;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.shared.event.ProcessedEventStore;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Discovery 上下文应用层测试：通知投影、搜索投影、通知用例与搜索/相关推荐查询。
 */
class DiscoveryApplicationTest {

    private static final Instant NOW = Instant.parse("2026-10-08T02:00:00Z");

    private InMemoryNotifications notifications;
    private InMemorySearchIndex index;
    private RecordingProcessed processed;
    private StubIdentity identity;
    private StubQa qa;

    private NotificationProjector notificationProjector;
    private SearchIndexProjector searchProjector;
    private NotificationService notificationService;
    private DiscoveryQueryService discovery;

    @BeforeEach
    void setUp() {
        notifications = new InMemoryNotifications();
        index = new InMemorySearchIndex();
        processed = new RecordingProcessed();
        identity = new StubIdentity();
        qa = new StubQa();
        notificationProjector = new NotificationProjector(notifications, identity, qa);
        searchProjector = new SearchIndexProjector(index, processed);
        notificationService = new NotificationService(notifications);
        discovery = new DiscoveryQueryService(new FakeSearchPort(), index, identity);
    }

    @Test
    void 回答事件通知提问者且自我回答不通知() {
        notificationProjector.on(new AnswerSubmittedEvent("e1", NOW, 50, 10, 2, 7, "跳表问题"));
        assertThat(notifications.findByRecipient(7, new PageRequest(1, 20)).items()).hasSize(1);
        Notification n = notifications.findByRecipient(7, new PageRequest(1, 20)).items().get(0);
        assertThat(n.type()).isEqualTo(NotificationType.ANSWER_RECEIVED);
        assertThat(n.link()).isEqualTo("/questions/10");

        // 自我回答不产生通知
        notificationProjector.on(new AnswerSubmittedEvent("e2", NOW, 51, 10, 7, 7, "跳表问题"));
        assertThat(notifications.store).hasSize(1);

        // 重复投递被 (sourceEventId, recipientId) 幂等键拦截
        notificationProjector.on(new AnswerSubmittedEvent("e1", NOW, 50, 10, 2, 7, "跳表问题"));
        assertThat(notifications.store).hasSize(1);
    }

    @Test
    void 评论中的提及生成通知并解析昵称() {
        identity.withUser(3, "小明", Role.STUDENT).withUser(4, "小红", Role.STUDENT);
        notificationProjector.on(new CommentPostedEvent("c1", NOW, 1, 10, 5, "感谢 @小明 和 @小红，另外 @不存在 无妨"));

        assertThat(notifications.findByRecipient(3, new PageRequest(1, 20)).items()).hasSize(1);
        assertThat(notifications.findByRecipient(4, new PageRequest(1, 20)).items()).hasSize(1);
        // 提及作者本人不通知
        notificationProjector.on(new CommentPostedEvent("c2", NOW, 2, 10, 3, "@小明 自说自话"));
        assertThat(notifications.findByRecipient(3, new PageRequest(1, 20)).items()).hasSize(1);
    }

    @Test
    void 采纳与悬赏与徽章事件生成对应通知() {
        notificationProjector.on(new AnswerAcceptedEvent("a1", NOW, 50, 10, 2, 7, 7, "跳表问题"));
        assertThat(notifications.findByRecipient(2, new PageRequest(1, 20)).items())
                .extracting(Notification::type).containsExactly(NotificationType.ANSWER_ACCEPTED);

        notificationProjector.on(new BountyAwardedEvent("b1", NOW, 1, 10, 9, 2, 50));
        assertThat(notifications.findByRecipient(2, new PageRequest(1, 20)).items())
                .extracting(Notification::type).contains(NotificationType.BOUNTY_AWARDED);

        notificationProjector.on(new BadgeAwardedEvent("g1", NOW, 2, "FIRST_ACCEPTED", "初露锋芒"));
        assertThat(notifications.findByRecipient(2, new PageRequest(1, 20)).items())
                .extracting(Notification::type).contains(NotificationType.BADGE_AWARDED);
    }

    @Test
    void 通知用例列表未读与已读() {
        notificationProjector.on(new AnswerSubmittedEvent("e1", NOW, 50, 10, 2, 7, "问题"));
        Actor owner = new Actor(7, "小明", Role.STUDENT);
        assertThat(notificationService.unreadCount(owner)).isEqualTo(1);
        assertThat(notificationService.list(owner, new PageRequest(1, 20)).items()).hasSize(1);

        long id = notifications.findByRecipient(7, new PageRequest(1, 20)).items().get(0).id();
        notificationService.markRead(owner, id);
        assertThat(notificationService.unreadCount(owner)).isZero();

        notificationProjector.on(new AnswerSubmittedEvent("e2", NOW, 51, 10, 2, 7, "问题"));
        assertThat(notificationService.markAllRead(owner)).isEqualTo(1);
        assertThat(notificationService.unreadCount(owner)).isZero();
    }

    @Test
    void 搜索投影按事件类型更新且重复事件被去重() {
        searchProjector.on(new QuestionPostedEvent("p1", NOW, 10, 7, null, "跳表问题", "正文", List.of("数据结构")));
        assertThat(index.docs).hasSize(1);
        assertThat(index.docs.get(0).questionId()).isEqualTo(10);

        searchProjector.on(new QuestionPostedEvent("p1", NOW, 10, 7, null, "跳表问题", "正文", List.of("数据结构")));
        assertThat(index.docs).hasSize(1);

        searchProjector.on(new AnswerSubmittedEvent("a1", NOW, 50, 10, 2, 7, "问题"));
        assertThat(index.answerCountDeltas.get(10L)).isEqualTo(1);

        searchProjector.on(new VoteCastEvent("v1", NOW, "QUESTION", 10, 7, 3, 10, 0, 1));
        assertThat(index.scoreDeltas.get(10L)).isEqualTo(1);

        searchProjector.on(new AnswerAcceptedEvent("c1", NOW, 50, 10, 2, 7, 7, "问题"));
        assertThat(index.accepted).contains(10L);

        searchProjector.on(new BountyOpenedEvent("o1", NOW, 1, 10, 9, 50, NOW));
        assertThat(index.bountyOpen.get(10L)).isTrue();
        searchProjector.on(new BountyClosedEvent("x1", NOW, 1, 10, 9, 50));
        assertThat(index.bountyOpen.get(10L)).isFalse();
    }

    @Test
    void 搜索查询映射作者并按Jaccard排序相关推荐() {
        identity.withUser(7, "小明", Role.STUDENT);
        PageResult<SearchResultView> hits =
                discovery.search(new SearchQuery("跳表", null, null, null, false, false, null));
        assertThat(hits.items()).hasSize(1);
        SearchResultView view = hits.items().get(0);
        assertThat(view.author().displayName()).isEqualTo("小明");
        assertThat(view.accepted()).isTrue();

        index.docs.add(new SearchDocument(20, null, 8, "B", "正文", List.of("x", "y"), NOW));
        index.tags.put(10L, List.of("a", "b", "c"));
        index.candidates.add(new SearchIndex.RelatedCandidate(20, "B", 2, 2, 0, 0, false));
        index.candidates.add(new SearchIndex.RelatedCandidate(21, "C", 2, 3, 5, 1, true));

        List<RelatedQuestionView> related = discovery.related(10, 2);
        assertThat(related).extracting(RelatedQuestionView::id).containsExactly(20L, 21L);
        assertThat(related.get(0).similarity()).isEqualTo(2.0 / 3.0);
    }

    // ---------------- 手写测试替身 ----------------

    static class InMemoryNotifications implements NotificationRepository {
        final Map<Long, Notification> store = new LinkedHashMap<>();

        @Override
        public boolean exists(String sourceEventId, long recipientId) {
            return store.values().stream()
                    .anyMatch(n -> n.sourceEventId().equals(sourceEventId) && n.recipientId() == recipientId);
        }

        @Override
        public Notification save(Notification notification) {
            if (notification.id() == null) {
                notification.assignId(store.size() + 1L);
            }
            store.put(notification.id(), notification);
            return notification;
        }

        @Override
        public Optional<Notification> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public PageResult<Notification> findByRecipient(long recipientId, PageRequest page) {
            List<Notification> mine = store.values().stream().filter(n -> n.recipientId() == recipientId).toList();
            return new PageResult<>(mine, mine.size(), page.page(), page.size());
        }

        @Override
        public long countUnread(long recipientId) {
            return store.values().stream().filter(n -> n.recipientId() == recipientId && !n.read()).count();
        }

        @Override
        public int markAllRead(long recipientId) {
            int updated = 0;
            for (Notification n : store.values()) {
                if (n.recipientId() == recipientId && !n.read()) {
                    n.markRead(recipientId);
                    updated++;
                }
            }
            return updated;
        }
    }

    static class InMemorySearchIndex implements SearchIndex {
        final List<SearchDocument> docs = new ArrayList<>();
        final Map<Long, Integer> answerCountDeltas = new HashMap<>();
        final Map<Long, Integer> scoreDeltas = new HashMap<>();
        final List<Long> accepted = new ArrayList<>();
        final Map<Long, Boolean> bountyOpen = new HashMap<>();
        final List<Long> deleted = new ArrayList<>();
        final Map<Long, List<String>> tags = new HashMap<>();
        final List<RelatedCandidate> candidates = new ArrayList<>();
        final List<String> updatedTexts = new ArrayList<>();

        @Override
        public void insert(SearchDocument document) {
            docs.add(document);
        }

        @Override
        public void updateText(long questionId, String title, String bodyText, List<String> tags, Instant at) {
            updatedTexts.add(title);
        }

        @Override
        public void adjustAnswerCount(long questionId, int delta, Instant at) {
            answerCountDeltas.merge(questionId, delta, Integer::sum);
        }

        @Override
        public void adjustScore(long questionId, int delta) {
            scoreDeltas.merge(questionId, delta, Integer::sum);
        }

        @Override
        public void markAccepted(long questionId, Instant at) {
            accepted.add(questionId);
        }

        @Override
        public void setBountyOpen(long questionId, boolean open) {
            bountyOpen.put(questionId, open);
        }

        @Override
        public void markDeleted(long questionId) {
            deleted.add(questionId);
        }

        @Override
        public List<String> findTags(long questionId) {
            return tags.getOrDefault(questionId, List.of());
        }

        @Override
        public List<RelatedCandidate> findCandidatesSharingTags(long questionId, List<String> tags, int limit) {
            return candidates;
        }
    }

    static class FakeSearchPort implements SearchPort {
        @Override
        public PageResult<SearchHit> search(SearchQuery query) {
            SearchHit hit = new SearchHit(10, "跳表问题", "摘录", List.of("数据结构"), null, 7, 5, 2, true, false, NOW);
            return new PageResult<>(List.of(hit), 1, 1, 20);
        }
    }

    static class RecordingProcessed implements ProcessedEventStore {
        final List<String> seen = new ArrayList<>();

        @Override
        public boolean markProcessed(String eventId, String consumer) {
            if (seen.contains(eventId)) {
                return false;
            }
            seen.add(eventId);
            return true;
        }
    }

    static class StubIdentity implements IdentityApi {
        final Map<Long, UserSummary> users = new HashMap<>();

        StubIdentity withUser(long id, String name, Role role) {
            users.put(id, new UserSummary(id, name, role, true, "计算机工程学院"));
            return this;
        }

        @Override
        public Optional<UserSummary> findUser(long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
            Map<Long, UserSummary> result = new HashMap<>();
            userIds.forEach(id -> findUser(id).ifPresent(u -> result.put(id, u)));
            return result;
        }

        @Override
        public Optional<UserSummary> findByDisplayName(String displayName) {
            return users.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
        }

        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            return Optional.of(new CourseSummary(courseId, "CS2001", "数据结构", "2026-秋", 9L));
        }

        @Override
        public boolean canManageCourse(long userId, Long courseId) {
            return false;
        }
    }

    static class StubQa implements QaQueryApi {
        final Map<Long, QuestionRef> questions = new HashMap<>();

        @Override
        public Optional<QuestionRef> findQuestion(long questionId) {
            return Optional.ofNullable(questions.get(questionId));
        }
    }
}
