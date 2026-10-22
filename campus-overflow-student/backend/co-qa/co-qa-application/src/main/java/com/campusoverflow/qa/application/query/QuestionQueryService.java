package com.campusoverflow.qa.application.query;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 问答查询用例（读路径）。 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;

    private final QaReadModel readModel;
    private final IdentityApi identity;
    private final ContentRenderer renderer;
    private final ViewCounter viewCounter;
    private final QuestionListCache cache;

    public QuestionQueryService(QaReadModel readModel, IdentityApi identity, ContentRenderer renderer,
                                ViewCounter viewCounter, QuestionListCache cache) {
        this.readModel = readModel;
        this.identity = identity;
        this.renderer = renderer;
        this.viewCounter = viewCounter;
        this.cache = cache;
    }

    public PageResult<QuestionSummaryView> list(Long courseId, PageRequest page) {
        return cache.get(courseId, page).orElseGet(() -> {
            PageResult<QuestionRow> rows = readModel.listQuestions(courseId, page);
            Set<Long> authorIds = rows.items().stream().map(QuestionRow::authorId)
                    .collect(Collectors.toSet());
            Map<Long, UserSummary> users = identity.findUsers(authorIds);
            PageResult<QuestionSummaryView> views = rows.map(r -> toSummary(r, users));
            cache.put(courseId, page, views);
            return views;
        });
    }

    public QuestionDetailView detail(long questionId, Actor actorOrNull) {
        QuestionRow q = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在"));
        List<AnswerRow> answerRows = readModel.findAnswers(questionId);
        List<CommentRow> commentRows = readModel.findComments(questionId);
        viewCounter.increment(questionId);

        Set<Long> userIds = new HashSet<>();
        userIds.add(q.authorId());
        answerRows.forEach(a -> {
            userIds.add(a.authorId());
            if (a.endorsedBy() != null) {
                userIds.add(a.endorsedBy());
            }
        });
        commentRows.forEach(c -> userIds.add(c.authorId()));
        Map<Long, UserSummary> users = identity.findUsers(userIds);

        List<Long> answerIds = answerRows.stream().map(AnswerRow::id).toList();
        Map<String, Integer> myVotes = actorOrNull == null ? Map.of()
                : readModel.findMyVotes(actorOrNull.userId(), questionId, answerIds);
        boolean isAuthor = actorOrNull != null && actorOrNull.is(q.authorId());
        boolean manages = actorOrNull != null && identity.canManageCourse(actorOrNull.userId(), q.courseId());

        CourseRefView course = q.courseId() == null ? null
                : identity.findCourse(q.courseId()).map(c -> new CourseRefView(c.id(), c.code(), c.name()))
                        .orElse(null);

        List<AnswerRow> sorted = new ArrayList<>(answerRows);
        sorted.sort(Comparator
                .comparing((AnswerRow a) -> q.acceptedAnswerId() != null && q.acceptedAnswerId() == a.id(),
                        Comparator.reverseOrder())
                .thenComparing(AnswerRow::score, Comparator.reverseOrder())
                .thenComparing(AnswerRow::createdAt));

        Map<Long, List<CommentRow>> commentsByAnswer = commentRows.stream()
                .filter(c -> "ANSWER".equals(c.targetType()))
                .collect(Collectors.groupingBy(CommentRow::targetId));
        List<CommentRow> questionComments = commentRows.stream()
                .filter(c -> "QUESTION".equals(c.targetType())).toList();

        List<AnswerView> answers = sorted.stream().map(a -> {
            boolean accepted = q.acceptedAnswerId() != null && q.acceptedAnswerId() == a.id();
            boolean canEdit = (actorOrNull != null && actorOrNull.is(a.authorId())) || manages;
            return new AnswerView(a.id(), a.body(), renderer.toSafeHtml(a.body()),
                    AuthorView.of(a.authorId(), users.get(a.authorId())), a.score(), accepted,
                    a.endorsedBy() == null ? null : AuthorView.of(a.endorsedBy(), users.get(a.endorsedBy())),
                    myVotes.getOrDefault("ANSWER:" + a.id(), 0), canEdit, a.createdAt(), a.updatedAt(),
                    toComments(commentsByAnswer.getOrDefault(a.id(), List.of()), users));
        }).toList();

        PermissionsView permissions = new PermissionsView(isAuthor || manages, isAuthor || manages,
                isAuthor || manages, manages);
        return new QuestionDetailView(q.id(), q.title(), q.body(), renderer.toSafeHtml(q.body()), q.tags(), course,
                AuthorView.of(q.authorId(), users.get(q.authorId())), q.score(), q.answerCount(), q.viewCount(),
                q.acceptedAnswerId(), myVotes.getOrDefault("QUESTION:" + questionId, 0), q.createdAt(), q.updatedAt(),
                permissions, toComments(questionComments, users), answers);
    }

    public List<TagView> popularTags(int limit) {
        return readModel.popularTags(limit);
    }

    private QuestionSummaryView toSummary(QuestionRow r, Map<Long, UserSummary> users) {
        return new QuestionSummaryView(r.id(), r.title(), renderer.toPlainText(r.body(), EXCERPT_LENGTH),
                r.tags(), r.courseId(), AuthorView.of(r.authorId(), users.get(r.authorId())), r.score(),
                r.answerCount(), r.viewCount(), r.acceptedAnswerId() != null, r.createdAt());
    }

    private static List<CommentView> toComments(List<CommentRow> rows, Map<Long, UserSummary> users) {
        return rows.stream().map(c -> new CommentView(c.id(), AuthorView.of(c.authorId(), users.get(c.authorId())),
                c.parentId(), c.body(), c.createdAt())).toList();
    }
}
