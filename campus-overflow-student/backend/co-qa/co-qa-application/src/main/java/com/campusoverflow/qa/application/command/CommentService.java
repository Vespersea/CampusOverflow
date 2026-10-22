package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：评论与二级回复。评论中的 @昵称 由 Discovery 解析并发送提醒。 */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public CommentService(CommentRepository comments, QuestionRepository questions, AnswerRepository answers,
                          IntegrationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostCommentCommand cmd) {
        long questionId = resolveQuestionId(cmd);
        Comment parent = null;
        if (cmd.parentId() != null) {
            parent = comments.findById(cmd.parentId())
                    .orElseThrow(() -> new NotFoundException("QA-4003", "父评论不存在"));
        }
        Comment comment = Comment.post(cmd.targetType(), cmd.targetId(), questionId, actor.userId(), parent,
                cmd.body(), clock.instant());
        Comment saved = comments.save(comment);
        events.publish(new CommentPostedEvent(EventIds.next(), clock.instant(), saved.id(), questionId,
                actor.userId(), saved.body()));
        return saved.id();
    }

    /** 校验评论目标存在（且未删除），返回所属问题 ID。 */
    private long resolveQuestionId(PostCommentCommand cmd) {
        return switch (cmd.targetType()) {
            case QUESTION -> {
                Question q = questions.findById(cmd.targetId())
                        .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在"));
                q.ensureOpenForAnswers();
                yield cmd.targetId();
            }
            case ANSWER -> {
                Answer a = answers.findById(cmd.targetId())
                        .orElseThrow(() -> new NotFoundException("QA-4002", "回答不存在"));
                if (a.isDeleted()) {
                    throw new BusinessRuleException("QA-1014", "该回答已删除");
                }
                yield a.questionId();
            }
        };
    }
}
