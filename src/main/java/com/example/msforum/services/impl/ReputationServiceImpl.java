package com.example.msforum.services.impl;

import com.example.msforum.dto.LeaderboardEntryResponse;
import com.example.msforum.dto.ReputationEventResponse;
import com.example.msforum.dto.ReputationProfileResponse;
import com.example.msforum.entities.Comment;
import com.example.msforum.entities.Post;
import com.example.msforum.entities.ReactionType;
import com.example.msforum.entities.ReputationActionType;
import com.example.msforum.entities.ReputationEvent;
import com.example.msforum.entities.ReputationLevel;
import com.example.msforum.entities.UserReputation;
import com.example.msforum.repositories.ReputationEventRepository;
import com.example.msforum.repositories.UserReputationRepository;
import com.example.msforum.services.ReputationService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReputationServiceImpl implements ReputationService {

    private static final int POINTS_POST_CREATED = 10;
    private static final int POINTS_COMMENT_CREATED = 5;
    private static final int POINTS_POST_LIKED = 2;
    private static final int POINTS_BEST_ANSWER = 15;
    private static final int INTERMEDIATE_MIN_POINTS = 100;
    private static final int EXPERT_MIN_POINTS = 300;

    private final UserReputationRepository userReputationRepository;
    private final ReputationEventRepository reputationEventRepository;

    public ReputationServiceImpl(UserReputationRepository userReputationRepository,
                                 ReputationEventRepository reputationEventRepository) {
        this.userReputationRepository = userReputationRepository;
        this.reputationEventRepository = reputationEventRepository;
    }

    @Override
    public void onPostCreated(Post post) {
        UserReputation reputation = getOrCreate(post.getUserId());
        normalizeDefaults(reputation);
        reputation.setPostsCount(reputation.getPostsCount() + 1);
        applyDelta(reputation, POINTS_POST_CREATED);
        logEvent(post.getUserId(), ReputationActionType.POST_CREATED, POINTS_POST_CREATED, "POST",
            String.valueOf(post.getId()));
    }

    @Override
    public void onCommentCreated(Comment comment) {
        UserReputation reputation = getOrCreate(comment.getUserId());
        normalizeDefaults(reputation);
        reputation.setCommentsCount(reputation.getCommentsCount() + 1);
        applyDelta(reputation, POINTS_COMMENT_CREATED);
        logEvent(comment.getUserId(), ReputationActionType.COMMENT_CREATED, POINTS_COMMENT_CREATED, "COMMENT",
            String.valueOf(comment.getId()));
    }

    @Override
    public void onPostReactionChanged(Post post, String reactorUserId, ReactionType previousType, ReactionType newType) {
        if (post.getUserId() == null || post.getUserId().equals(reactorUserId)) {
            return;
        }
        int delta = 0;
        int likesReceivedDelta = 0;
        if (previousType != ReactionType.LIKE && newType == ReactionType.LIKE) {
            delta = POINTS_POST_LIKED;
            likesReceivedDelta = 1;
        } else if (previousType == ReactionType.LIKE && newType != ReactionType.LIKE) {
            delta = -POINTS_POST_LIKED;
            likesReceivedDelta = -1;
        }
        if (delta == 0) {
            return;
        }
        UserReputation reputation = getOrCreate(post.getUserId());
        normalizeDefaults(reputation);
        reputation.setLikesReceivedCount(Math.max(0, reputation.getLikesReceivedCount() + likesReceivedDelta));
        applyDelta(reputation, delta);
        logEvent(post.getUserId(), ReputationActionType.POST_LIKED, delta, "POST", String.valueOf(post.getId()));
    }

    @Override
    public void onBestAnswerSelected(Post post, Comment previousBestAnswer, Comment newBestAnswer) {
        if (previousBestAnswer != null) {
            UserReputation previousReputation = getOrCreate(previousBestAnswer.getUserId());
            normalizeDefaults(previousReputation);
            previousReputation.setBestAnswersCount(Math.max(0, previousReputation.getBestAnswersCount() - 1));
            applyDelta(previousReputation, -POINTS_BEST_ANSWER);
            logEvent(previousBestAnswer.getUserId(), ReputationActionType.BEST_ANSWER_SELECTED, -POINTS_BEST_ANSWER,
                "COMMENT", String.valueOf(previousBestAnswer.getId()));
        }
        if (newBestAnswer != null) {
            UserReputation newReputation = getOrCreate(newBestAnswer.getUserId());
            normalizeDefaults(newReputation);
            newReputation.setBestAnswersCount(newReputation.getBestAnswersCount() + 1);
            applyDelta(newReputation, POINTS_BEST_ANSWER);
            logEvent(newBestAnswer.getUserId(), ReputationActionType.BEST_ANSWER_SELECTED, POINTS_BEST_ANSWER,
                "COMMENT", String.valueOf(newBestAnswer.getId()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReputationProfileResponse getProfile(String userId) {
        UserReputation reputation = userReputationRepository.findByUserId(userId).orElseGet(() -> {
            UserReputation empty = new UserReputation();
            empty.setUserId(userId);
            empty.setPoints(0);
            empty.setLevel(ReputationLevel.BEGINNER);
            empty.setPostsCount(0);
            empty.setCommentsCount(0);
            empty.setLikesReceivedCount(0);
            empty.setBestAnswersCount(0);
            return empty;
        });
        normalizeDefaults(reputation);
        int rank = (int) userReputationRepository.countByPointsGreaterThan(reputation.getPoints()) + 1;
        List<ReputationEventResponse> recentEvents = reputationEventRepository
            .findTop20ByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(event -> ReputationEventResponse.builder()
                .actionType(event.getActionType().name())
                .pointsDelta(event.getPointsDelta())
                .sourceType(event.getSourceType())
                .sourceId(event.getSourceId())
                .createdAt(event.getCreatedAt())
                .build())
            .collect(Collectors.toList());
        return ReputationProfileResponse.builder()
            .userId(userId)
            .points(reputation.getPoints())
            .level(reputation.getLevel().name())
            .rank(rank)
            .postsCount(reputation.getPostsCount())
            .commentsCount(reputation.getCommentsCount())
            .likesReceivedCount(reputation.getLikesReceivedCount())
            .bestAnswersCount(reputation.getBestAnswersCount())
            .recentEvents(recentEvents)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<UserReputation> rows = userReputationRepository.findAllByOrderByPointsDescUpdatedAtAsc(PageRequest.of(0, safeLimit));
        return rows.stream()
            .map(row -> LeaderboardEntryResponse.builder()
                .rank((int) userReputationRepository.countByPointsGreaterThan(row.getPoints()) + 1)
                .userId(row.getUserId())
                .points(row.getPoints())
                .level(row.getLevel().name())
                .build())
            .collect(Collectors.toList());
    }

    private UserReputation getOrCreate(String userId) {
        UserReputation reputation = userReputationRepository.findByUserId(userId).orElseGet(() -> {
            UserReputation newReputation = new UserReputation();
            newReputation.setUserId(userId);
            newReputation.setPoints(0);
            newReputation.setLevel(ReputationLevel.BEGINNER);
            newReputation.setPostsCount(0);
            newReputation.setCommentsCount(0);
            newReputation.setLikesReceivedCount(0);
            newReputation.setBestAnswersCount(0);
            return userReputationRepository.save(newReputation);
        });
        normalizeDefaults(reputation);
        return reputation;
    }

    private void applyDelta(UserReputation reputation, int delta) {
        normalizeDefaults(reputation);
        int updatedPoints = Math.max(0, reputation.getPoints() + delta);
        reputation.setPoints(updatedPoints);
        reputation.setLevel(resolveLevel(updatedPoints));
        userReputationRepository.save(reputation);
    }

    private void normalizeDefaults(UserReputation reputation) {
        if (reputation.getPoints() == null) {
            reputation.setPoints(0);
        }
        if (reputation.getLevel() == null) {
            reputation.setLevel(ReputationLevel.BEGINNER);
        }
        if (reputation.getPostsCount() == null) {
            reputation.setPostsCount(0);
        }
        if (reputation.getCommentsCount() == null) {
            reputation.setCommentsCount(0);
        }
        if (reputation.getLikesReceivedCount() == null) {
            reputation.setLikesReceivedCount(0);
        }
        if (reputation.getBestAnswersCount() == null) {
            reputation.setBestAnswersCount(0);
        }
    }

    private ReputationLevel resolveLevel(int points) {
        if (points >= EXPERT_MIN_POINTS) {
            return ReputationLevel.EXPERT;
        }
        if (points >= INTERMEDIATE_MIN_POINTS) {
            return ReputationLevel.INTERMEDIATE;
        }
        return ReputationLevel.BEGINNER;
    }

    private void logEvent(String userId, ReputationActionType actionType, int delta, String sourceType, String sourceId) {
        ReputationEvent event = new ReputationEvent();
        event.setUserId(userId);
        event.setActionType(actionType);
        event.setPointsDelta(delta);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        reputationEventRepository.save(event);
    }
}
