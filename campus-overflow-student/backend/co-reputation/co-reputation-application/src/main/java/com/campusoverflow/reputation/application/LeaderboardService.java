package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.shared.event.ProcessedEventStore;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 排行榜：周/月/学期榜来自 Redis ZSET，总榜直接读账户表。 */
@Service
public class LeaderboardService {

    private final LeaderboardStore store;
    private final ReputationAccountRepository accounts;
    private final IdentityApi identity;
    private final ProcessedEventStore processed;
    private final Clock clock;

    public LeaderboardService(LeaderboardStore store, ReputationAccountRepository accounts, IdentityApi identity,
                              ProcessedEventStore processed, Clock clock) {
        this.store = store;
        this.accounts = accounts;
        this.identity = identity;
        this.processed = processed;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void on(ReputationChangedEvent e) {
        // 至少一次投递：先去重，再只把增量写进各周期 ZSET；总榜直接读账户表
        if (!processed.markProcessed(e.eventId(), "reputation.leaderboard")) {
            return;
        }
        store.increment(e.userId(), e.delta(), e.occurredAt());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryView> top(LeaderboardPeriod period, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        if (period == LeaderboardPeriod.ALL) {
            return rank(accounts.topByReputation(safeLimit).stream()
                    .map(a -> new LeaderboardStore.Entry(a.userId(), a.reputation()))
                    .toList());
        }
        return rank(store.top(period, clock.instant(), safeLimit));
    }

    private List<LeaderboardEntryView> rank(List<LeaderboardStore.Entry> entries) {
        Map<Long, UserSummary> users = identity.findUsers(
                entries.stream().map(LeaderboardStore.Entry::userId).toList());
        List<LeaderboardEntryView> views = new ArrayList<>(entries.size());
        for (LeaderboardStore.Entry e : entries) {
            UserSummary u = users.get(e.userId());
            views.add(new LeaderboardEntryView(views.size() + 1, e.userId(),
                    u == null ? "用户" + e.userId() : u.displayName(), u != null && u.verified(), e.score()));
        }
        return views;
    }
}
