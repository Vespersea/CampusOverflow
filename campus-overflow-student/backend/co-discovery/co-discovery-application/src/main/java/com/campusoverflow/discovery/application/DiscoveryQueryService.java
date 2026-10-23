package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.search.RelatedQuestionPolicy;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchPort;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.shared.page.PageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：搜索与筛选、相关问题推荐。 */
@Service
@Transactional(readOnly = true)
public class DiscoveryQueryService {

    private final SearchPort search;
    private final SearchIndex index;
    private final IdentityApi identity;

    public DiscoveryQueryService(SearchPort search, SearchIndex index, IdentityApi identity) {
        this.search = search;
        this.index = index;
        this.identity = identity;
    }

    public PageResult<SearchResultView> search(SearchQuery query) {
        PageResult<SearchHit> hits = search.search(query);
        Map<Long, UserSummary> authors = identity.findUsers(
                hits.items().stream().map(SearchHit::authorId).distinct().toList());
        return hits.map(hit -> {
            UserSummary author = authors.get(hit.authorId());
            return new SearchResultView(hit.questionId(), hit.title(), hit.snippet(), hit.tags(), hit.courseId(),
                    author == null ? new SearchResultView.Author(hit.authorId(), "用户" + hit.authorId(), "STUDENT",
                            false)
                            : new SearchResultView.Author(author.id(), author.displayName(), author.role().name(),
                            author.verified()),
                    hit.score(), hit.answerCount(), hit.accepted(), hit.bountyOpen(), hit.createdAt());
        });
    }

    public List<RelatedQuestionView> related(long questionId, int limit) {
        List<String> tags = index.findTags(questionId);
        if (tags.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        // 先取足量候选再排序截断，避免在低相似度候选中提前截断
        List<SearchIndex.RelatedCandidate> candidates =
                index.findCandidatesSharingTags(questionId, tags, Math.max(safeLimit * 4, 20));
        return RelatedQuestionPolicy.rank(candidates, tags.size(), safeLimit).stream()
                .map(c -> new RelatedQuestionView(c.questionId(), c.title(), c.answerCount(), c.accepted(),
                        RelatedQuestionPolicy.jaccard(c.sharedTags(), tags.size(), c.tagCount())))
                .toList();
    }
}
