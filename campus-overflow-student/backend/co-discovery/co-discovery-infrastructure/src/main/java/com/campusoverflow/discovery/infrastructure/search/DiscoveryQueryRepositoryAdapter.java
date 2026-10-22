package com.campusoverflow.discovery.infrastructure.search;

import com.campusoverflow.discovery.domain.DiscoveryQueryRepository;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.shared.page.PageResult;
import org.springframework.stereotype.Repository;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DiscoveryQueryRepositoryAdapter implements DiscoveryQueryRepository {

    private final LikeSearchAdapter likeSearchAdapter;

    public DiscoveryQueryRepositoryAdapter(LikeSearchAdapter likeSearchAdapter) {
        this.likeSearchAdapter = likeSearchAdapter;
    }

    @Override
    public List<Long> searchQuestion(SearchQuery query) {
        PageResult<SearchHit> pageResult = likeSearchAdapter.search(query);
        return pageResult.items().stream()
                .map(SearchHit::questionId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Question> findRelatedQuestions(Long questionId, int limit) {
        return Collections.emptyList();
    }
}