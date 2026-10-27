package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeRepository;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyRepository;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.event.ProcessedEventStore;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
 * 应用层用例测试：手写 Test Double，不依赖 Spring 容器与数据库（毫秒级）。
 * 覆盖 ReputationEventHandlers、BountyService、LeaderboardService、ReconciliationService 等核心用例。
 */
class ReputationApplicationTest {

    private static final Instant NOW = Instant.parse("2026-10-08T02:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Actor TEACHER = new Actor(9, "王老师", Role.TEACHER);

    private InMemoryAccounts accounts;
    private InMemoryLedger ledger;
    private InMemoryBounties bounties;
    private InMemoryBadges badges;
    private RecordingEvents events;
    private RecordingProcessed processed;
    private InMemoryLeaderboard leaderboard;
    private StubIdentity identity;
    private StubQa qa;

    private ReputationLedgerService ledgerService;
    private BadgeService badgeService;
    private BountyService bountyService;
    private ReputationEventHandlers handlers;
    private LeaderboardService leaderboardService;
    private ReconciliationService reconciliation;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        ledger = new InMemoryLedger();
        bounties = new InMemoryBounties();
        badges = new InMemoryBadges();
        events = new RecordingEvents();
        processed = new RecordingProcessed();
        leaderboard = new InMemoryLeaderboard();
        identity = new StubIdentity();
        qa = new StubQa();
        badgeService = new BadgeService(badges, events, CLOCK);
        ledgerService = new ReputationLedgerService(accounts, ledger, badgeService, events, CLOCK);
        bountyService = new BountyService(bounties, accounts, ledgerService, qa, identity, events,
                new RecordingAudit(), CLOCK);
        handlers = new ReputationEventHandlers(accounts, ledger, ledgerService, badgeService, bountyService, CLOCK);
        leaderboardService = new LeaderboardService(leaderboard, accounts, identity, processed, CLOCK);
        reconciliation = new ReconciliationService(accounts, ledger);
    }

    @Test
    void 注册事件开立声誉账户且教师获得悬赏积分并幂等() {
        handlers.on(new UserRegisteredEvent("e1", NOW, 9, "王老师", Role.TEACHER));
        ReputationAccount teacher = accounts.findByUserId(9).orElseThrow();
        assertThat(teacher.reputation()).isZero();
        assertThat(teacher.availablePoints()).isEqualTo(1000);

        handlers.on(new UserRegisteredEvent("e2", NOW, 1, "小明", Role.STUDENT));
        assertThat(accounts.findByUserId(1).orElseThrow().availablePoints()).isZero();

        // 重复投递不重置积分
        teacher.applyReputation(15);
        accounts.save(teacher);
        handlers.on(new UserRegisteredEvent("e1-dup", NOW, 9, "王老师", Role.TEACHER));
        assertThat(accounts.findByUserId(9).orElseThrow().reputation()).isEqualTo(15);
    }

    @Test
    void 采纳事件为回答者加声誉授徽章并结算悬赏() {
        handlers.on(new UserRegisteredEvent("r1", NOW, 9, "王老师", Role.TEACHER));
        handlers.on(new UserRegisteredEvent("r2", NOW, 2, "小红", Role.STUDENT));
        Bounty bounty = Bounty.open(10, true, TEACHER, true, 50, 7, NOW);
        bounties.save(bounty);
        ReputationAccount teacher = accounts.findByUserId(9).orElseThrow();
        teacher.freezePoints(50);
        accounts.save(teacher);

        handlers.on(new AnswerAcceptedEvent("evt-accept", NOW, 50, 10, 2, 7, 7, "跳表问题"));

        assertThat(ledger.entries)
                .filteredOn(e -> e.reason() == ReputationReason.ANSWER_ACCEPTED)
                .extracting(LedgerEntry::delta).containsExactly(15);
        assertThat(badges.has(2, BadgeType.FIRST_ACCEPTED)).isTrue();
        assertThat(events.ofType(BadgeAwardedEvent.class))
                .extracting(e -> e.badgeCode()).contains(BadgeType.FIRST_ACCEPTED.name());
        // 悬赏结算：发起人冻结积分转出，获奖者获得等额声誉
        Bounty settled = bounties.findLatestByQuestion(10).orElseThrow();
        assertThat(settled.status()).isEqualTo(BountyStatus.AWARDED);
        assertThat(settled.winnerId()).isEqualTo(2L);
        assertThat(accounts.findByUserId(9).orElseThrow().frozenPoints()).isZero();
        assertThat(accounts.findByUserId(2).orElseThrow().reputation()).isEqualTo(65);
    }

    @Test
    void 自问自答被采纳不产生声誉也不授徽章() {
        handlers.on(new UserRegisteredEvent("r1", NOW, 7, "小明", Role.STUDENT));
        handlers.on(new AnswerAcceptedEvent("evt-self", NOW, 50, 10, 7, 7, 7, "自问自答"));
        assertThat(ledger.entries).isEmpty();
        assertThat(badges.awards).isEmpty();
    }

    @Test
    void 投票事件按增量记账且正向受每日上限约束() {
        handlers.on(new UserRegisteredEvent("r1", NOW, 7, "小明", Role.STUDENT));

        handlers.on(new VoteCastEvent("v1", NOW, "QUESTION", 10, 7, 3, 10, 0, 1));
        assertThat(accounts.findByUserId(7).orElseThrow().reputation()).isEqualTo(10);

        handlers.on(new VoteCastEvent("v2", NOW, "QUESTION", 10, 7, 4, 10, 1, -1));
        assertThat(accounts.findByUserId(7).orElseThrow().reputation()).isEqualTo(-2);

        // 撤销踩：+2
        handlers.on(new VoteCastEvent("v3", NOW, "QUESTION", 10, 7, 5, 10, -1, 0));
        assertThat(accounts.findByUserId(7).orElseThrow().reputation()).isZero();

        // 已获得 195 分后，再获 +10 只记 +5；负向不受限
        ledger.positiveVoteGainToday = 195;
        handlers.on(new VoteCastEvent("v4", NOW, "ANSWER", 51, 7, 6, 10, 0, 1));
        assertThat(accounts.findByUserId(7).orElseThrow().reputation()).isEqualTo(5);
        handlers.on(new VoteCastEvent("v5", NOW, "ANSWER", 51, 7, 6, 10, 0, -1));
        assertThat(accounts.findByUserId(7).orElseThrow().reputation()).isEqualTo(3);
    }

    @Test
    void 提交回答与教师认证分别授予徽章() {
        handlers.on(new UserRegisteredEvent("r1", NOW, 7, "小明", Role.STUDENT));
        handlers.on(new AnswerSubmittedEvent("s1", NOW, 50, 10, 7, 7, "问题"));
        assertThat(badges.has(7, BadgeType.FIRST_ANSWER)).isTrue();

        handlers.on(new AnswerEndorsedEvent("e1", NOW, 50, 10, 7, 9, "问题"));
        assertThat(badges.has(7, BadgeType.TEACHER_ENDORSED)).isTrue();
        assertThat(events.ofType(BadgeAwardedEvent.class)).hasSize(2);
    }

    @Test
    void 发起悬赏冻结积分并发布事件() {
        handlers.on(new UserRegisteredEvent("r1", NOW, 9, "王老师", Role.TEACHER));
        identity.withUser(9, "王老师", Role.TEACHER);
        qa.withQuestion(new QuestionRef(10, 7, null, "问题", null, false));

        long id = bountyService.open(TEACHER, 10, 100, 7);

        assertThat(accounts.findByUserId(9).orElseThrow().availablePoints()).isEqualTo(900);
        assertThat(accounts.findByUserId(9).orElseThrow().frozenPoints()).isEqualTo(100);
        Bounty bounty = bounties.findLatestByQuestion(10).orElseThrow();
        assertThat(bounty.id()).isEqualTo(id);
        assertThat(events.ofType(BountyOpenedEvent.class)).hasSize(1);

        // 同一问题重复发起 → 冲突
        assertThatThrownBy(() -> bountyService.open(TEACHER, 10, 50, 7))
                .isInstanceOf(com.campusoverflow.shared.domain.ConflictException.class);
    }

    @Test
    void 发起人自己的回答被采纳时关闭悬赏并退款() {
        handlers.on(new UserRegisteredEvent("r1", NOW, 9, "王老师", Role.TEACHER));
        ReputationAccount teacher = accounts.findByUserId(9).orElseThrow();
        teacher.freezePoints(50);
        accounts.save(teacher);
        Bounty bounty = Bounty.open(10, true, TEACHER, true, 50, 7, NOW);
        bounties.save(bounty);

        bountyService.settleOnAccept(10, 5, 9, "evt-self-accept");

        assertThat(bounties.findLatestByQuestion(10).orElseThrow().status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(accounts.findByUserId(9).orElseThrow().frozenPoints()).isZero();
        assertThat(accounts.findByUserId(9).orElseThrow().availablePoints()).isEqualTo(1000);
        assertThat(events.ofType(BountyClosedEvent.class)).hasSize(1);
    }

    @Test
    void 到期悬赏被定时任务关闭并退款() {
        handlers.on(new UserRegisteredEvent("r1", NOW, 9, "王老师", Role.TEACHER));
        ReputationAccount teacher = accounts.findByUserId(9).orElseThrow();
        teacher.freezePoints(50);
        accounts.save(teacher);
        Bounty bounty = Bounty.open(10, true, TEACHER, true, 50, 1, NOW.minusSeconds(86400));
        bounties.save(bounty);

        int closed = bountyService.expireDue();

        assertThat(closed).isEqualTo(1);
        assertThat(bounties.findLatestByQuestion(10).orElseThrow().status()).isEqualTo(BountyStatus.EXPIRED);
        assertThat(accounts.findByUserId(9).orElseThrow().availablePoints()).isEqualTo(1000);
        assertThat(events.ofType(BountyClosedEvent.class)).hasSize(1);
    }

    @Test
    void 声誉流水仅本人与管理员可查看() {
        accounts.save(ReputationAccount.open(7, 0));
        ledger.append(LedgerEntry.of(7, "e1", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 50, NOW));
        ReputationQueryService queries = new ReputationQueryService(accounts, ledger, badges, identity);

        assertThatThrownBy(() -> queries.ledger(7, new Actor(8, "小红", Role.STUDENT), new PageRequest(1, 20)))
                .isInstanceOf(ForbiddenException.class);

        assertThat(queries.ledger(7, new Actor(7, "小明", Role.STUDENT), new PageRequest(1, 20)).items()).hasSize(1);
        assertThat(queries.ledger(7, new Actor(99, "管理员", Role.ADMIN), new PageRequest(1, 20)).items()).hasSize(1);
    }

    @Test
    void 声誉档案仅本人可见积分字段() {
        accounts.save(ReputationAccount.open(7, 0));
        identity.withUser(7, "小明", Role.STUDENT);
        ReputationQueryService queries = new ReputationQueryService(accounts, ledger, badges, identity);

        assertThat(queries.profile(7, null).availablePoints()).isNull();
        assertThat(queries.profile(7, new Actor(7, "小明", Role.STUDENT)).availablePoints()).isZero();
        assertThat(queries.profile(7, null).displayName()).isEqualTo("小明");
    }

    @Test
    void 排行榜消费声誉变更事件并按总榜排序() {
        accounts.save(ReputationAccount.open(1, 0));
        accounts.save(ReputationAccount.open(2, 0));
        identity.withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT);

        leaderboardService.on(new com.campusoverflow.reputation.api.event.ReputationChangedEvent(
                "e1", NOW, 1, 15, 15, "ANSWER_ACCEPTED"));
        leaderboardService.on(new com.campusoverflow.reputation.api.event.ReputationChangedEvent(
                "e2", NOW, 2, 30, 30, "ANSWER_ACCEPTED"));
        leaderboardService.on(new com.campusoverflow.reputation.api.event.ReputationChangedEvent(
                "e1", NOW, 1, 15, 15, "ANSWER_ACCEPTED")); // 重复投递被幂等丢弃

        assertThat(leaderboard.increments.get(1L)).isEqualTo(15);
        assertThat(leaderboard.increments.get(2L)).isEqualTo(30);

        accounts.save(ReputationAccount.reconstitute(1, 15, 0, 0, 1));
        accounts.save(ReputationAccount.reconstitute(2, 30, 0, 0, 1));
        List<LeaderboardEntryView> top = leaderboardService.top(LeaderboardPeriod.ALL, 10);
        assertThat(top).extracting(LeaderboardEntryView::userId).containsExactly(2L, 1L);
        assertThat(top.get(0).displayName()).isEqualTo("小红");
        assertThat(top.get(0).rank()).isEqualTo(1);
    }

    @Test
    void 对账任务发现余额与流水不一致的账户() {
        ReputationAccount ok = ReputationAccount.open(1, 0);
        ok.applyReputation(15);
        accounts.save(ok);
        ledger.append(LedgerEntry.of(1, "e1", ReputationReason.ANSWER_ACCEPTED, 15, "ANSWER", 50, NOW));

        ReputationAccount broken = ReputationAccount.open(2, 0);
        broken.applyReputation(99);
        accounts.save(broken);

        List<ReconciliationService.Mismatch> result = reconciliation.reconcile();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(2L);
        assertThat(result.get(0).ledgerSum()).isZero();
    }

    @Test
    void 声誉接口批量查询返回余额映射() {
        ReputationAccount a = ReputationAccount.open(1, 0);
        a.applyReputation(10);
        accounts.save(a);
        ReputationApiImpl api = new ReputationApiImpl(accounts);

        assertThat(api.summary(1).reputation()).isEqualTo(10);
        assertThat(api.summary(404).reputation()).isZero();
        assertThat(api.reputations(List.of(1L, 404L))).containsEntry(1L, 10).doesNotContainKey(404L);
    }

    // ---------------- 手写测试替身 ----------------

    static class InMemoryAccounts implements ReputationAccountRepository {
        final Map<Long, ReputationAccount> store = new LinkedHashMap<>();

        @Override
        public Optional<ReputationAccount> findByUserId(long userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public List<ReputationAccount> findAllByUserId(Collection<Long> userIds) {
            return userIds.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<ReputationAccount> topByReputation(int limit) {
            return store.values().stream()
                    .sorted((x, y) -> Integer.compare(y.reputation(), x.reputation()))
                    .limit(limit).toList();
        }

        @Override
        public List<ReputationAccount> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public ReputationAccount save(ReputationAccount account) {
            store.put(account.userId(), account);
            return account;
        }
    }

    static class InMemoryLedger implements LedgerRepository {
        final List<LedgerEntry> entries = new ArrayList<>();
        int positiveVoteGainToday;

        @Override
        public boolean exists(String eventId, long userId, ReputationReason reason) {
            return entries.stream().anyMatch(
                    e -> e.eventId().equals(eventId) && e.userId() == userId && e.reason() == reason);
        }

        @Override
        public void append(LedgerEntry entry) {
            entries.add(new LedgerEntry((long) entries.size() + 1, entry.userId(), entry.eventId(), entry.reason(),
                    entry.delta(), entry.refType(), entry.refId(), entry.createdAt()));
        }

        @Override
        public PageResult<LedgerEntry> findByUser(long userId, PageRequest page) {
            List<LedgerEntry> mine = entries.stream().filter(e -> e.userId() == userId).toList();
            return new PageResult<>(mine, mine.size(), page.page(), page.size());
        }

        @Override
        public long countByUserAndReason(long userId, ReputationReason reason) {
            return entries.stream().filter(e -> e.userId() == userId && e.reason() == reason).count();
        }

        @Override
        public int sumPositiveVoteDeltaSince(long userId, Instant since) {
            return positiveVoteGainToday;
        }

        @Override
        public Map<Long, Long> sumByUser() {
            Map<Long, Long> sums = new HashMap<>();
            for (LedgerEntry e : entries) {
                sums.merge(e.userId(), (long) e.delta(), Long::sum);
            }
            return sums;
        }
    }

    static class InMemoryBounties implements BountyRepository {
        final Map<Long, Bounty> store = new LinkedHashMap<>();

        @Override
        public Optional<Bounty> findOpenByQuestion(long questionId) {
            return store.values().stream()
                    .filter(b -> b.questionId() == questionId && b.status() == BountyStatus.OPEN).findFirst();
        }

        @Override
        public Optional<Bounty> findLatestByQuestion(long questionId) {
            return store.values().stream().filter(b -> b.questionId() == questionId)
                    .max((x, y) -> Long.compare(x.id(), y.id()));
        }

        @Override
        public List<Bounty> findDue(Instant now, int limit) {
            return store.values().stream().filter(b -> b.isDue(now)).limit(limit).toList();
        }

        @Override
        public Bounty save(Bounty bounty) {
            if (bounty.id() == null) {
                bounty.assignId(store.size() + 1L);
            }
            store.put(bounty.id(), bounty);
            return bounty;
        }
    }

    static class InMemoryBadges implements BadgeRepository {
        final List<BadgeAward> awards = new ArrayList<>();

        @Override
        public boolean has(long userId, BadgeType type) {
            return awards.stream().anyMatch(a -> a.userId() == userId && a.type() == type);
        }

        @Override
        public void award(BadgeAward award) {
            awards.add(award);
        }

        @Override
        public List<BadgeAward> findByUser(long userId) {
            return awards.stream().filter(a -> a.userId() == userId).toList();
        }
    }

    static class RecordingEvents implements IntegrationEventPublisher {
        final List<IntegrationEvent> published = new ArrayList<>();

        @Override
        public void publish(IntegrationEvent event) {
            published.add(event);
        }

        <T extends IntegrationEvent> List<T> ofType(Class<T> type) {
            return published.stream().filter(type::isInstance).map(type::cast).toList();
        }
    }

    static class RecordingProcessed implements ProcessedEventStore {
        final List<String> processed = new ArrayList<>();

        @Override
        public boolean markProcessed(String eventId, String consumer) {
            if (processed.contains(eventId)) {
                return false;
            }
            processed.add(eventId);
            return true;
        }
    }

    static class InMemoryLeaderboard implements LeaderboardStore {
        final Map<Long, Integer> increments = new HashMap<>();
        final List<Entry> entries = new ArrayList<>();

        @Override
        public void increment(long userId, int delta, Instant at) {
            increments.merge(userId, delta, Integer::sum);
        }

        @Override
        public List<Entry> top(LeaderboardPeriod period, Instant at, int limit) {
            return entries;
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
            return users.get(userId) != null && users.get(userId).role() != Role.STUDENT;
        }
    }

    static class StubQa implements QaQueryApi {
        final Map<Long, QuestionRef> questions = new HashMap<>();

        StubQa withQuestion(QuestionRef ref) {
            questions.put(ref.id(), ref);
            return this;
        }

        @Override
        public Optional<QuestionRef> findQuestion(long questionId) {
            return Optional.ofNullable(questions.get(questionId));
        }
    }

    static class RecordingAudit implements AuditTrail {
        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
        }
    }
}
