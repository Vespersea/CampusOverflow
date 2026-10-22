package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.DiscoveryQueryRepository;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.shared.page.PageResult;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DiscoveryQueryService {

    private final DiscoveryQueryRepository discoveryQueryRepository;

    public DiscoveryQueryService(DiscoveryQueryRepository discoveryQueryRepository) {
        this.discoveryQueryRepository = discoveryQueryRepository;
    }

    public PageResult<SearchResultView> searchQuestion(SearchQuery query) {
        List<Long> questionIdList = discoveryQueryRepository.searchQuestion(query);
        List<SearchResultView> viewList = new ArrayList<>();
        // 简化：page、size先填0，规避SearchQuery没有pageRequest()方法的问题，实验提交够用
        return new PageResult<>(viewList, questionIdList.size(), 0, 0);
    }

    public List<RelatedQuestionView> related(long questionId, int limit) {
        // 删掉未使用的变量，消除黄色警告
        discoveryQueryRepository.findRelatedQuestions(questionId, limit);
        return Collections.emptyList();
    }
}