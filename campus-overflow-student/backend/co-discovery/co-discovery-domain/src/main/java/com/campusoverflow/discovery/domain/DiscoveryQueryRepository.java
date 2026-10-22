package com.campusoverflow.discovery.domain;

import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.qa.domain.Question;
import java.util.List;

public interface DiscoveryQueryRepository {
    List<Long> searchQuestion(SearchQuery query);
    List<Question> findRelatedQuestions(Long questionId, int limit);
}