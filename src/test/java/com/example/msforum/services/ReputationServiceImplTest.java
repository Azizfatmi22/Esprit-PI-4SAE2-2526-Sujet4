package com.example.msforum.services;

import com.example.msforum.dto.LeaderboardEntryResponse;
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
import com.example.msforum.services.impl.ReputationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationServiceImplTest {

    @Mock
    private UserReputationRepository userReputationRepository;

    @Mock
    private ReputationEventRepository reputationEventRepository;

    @InjectMocks
    private ReputationServiceImpl reputationService;

    private UserReputation userReputation;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        userReputation = new UserReputation();
        userReputation.setId(1L);
        userReputation.setUserId("user1");
        userReputation.setPoints(50);
        userReputation.setLevel(ReputationLevel.BEGINNER);
        userReputation.setPostsCount(2);
        userReputation.setCommentsCount(3);
        userReputation.setLikesReceivedCount(1);
        userReputation.setBestAnswersCount(0);

        post = new Post();
        post.setId(1L);
        post.setUserId("user1");

        comment = new Comment();
        comment.setId(10L);
        comment.setUserId("user1");
        comment.setPost(post);
    }

    // ── onPostCreated ────────────────────────────────────────────────────────

    @Test
    void onPostCreated_existingUser_incrementsPointsAndPostsCount() {
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        reputationService.onPostCreated(post);

        assertThat(userReputation.getPoints()).isEqualTo(60); // 50 + 10
        assertThat(userReputation.getPostsCount()).isEqualTo(3);

        ArgumentCaptor<ReputationEvent> eventCaptor = ArgumentCaptor.forClass(ReputationEvent.class);
        verify(reputationEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getActionType()).isEqualTo(ReputationActionType.POST_CREATED);
        assertThat(eventCaptor.getValue().getPointsDelta()).isEqualTo(10);
    }

    @Test
    void onPostCreated_newUser_createsReputationAndGrants10Points() {
        when(userReputationRepository.findByUserId("newuser")).thenReturn(Optional.empty());
        UserReputation newRep = new UserReputation();
        newRep.setUserId("newuser");
        newRep.setPoints(0);
        newRep.setLevel(ReputationLevel.BEGINNER);
        newRep.setPostsCount(0);
        newRep.setCommentsCount(0);
        newRep.setLikesReceivedCount(0);
        newRep.setBestAnswersCount(0);
        when(userReputationRepository.save(any(UserReputation.class))).thenReturn(newRep);
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        Post newPost = new Post();
        newPost.setId(5L);
        newPost.setUserId("newuser");

        reputationService.onPostCreated(newPost);

        verify(userReputationRepository, atLeastOnce()).save(any(UserReputation.class));
    }

    @Test
    void onPostCreated_reaching100Points_upgradesLevelToIntermediate() {
        userReputation.setPoints(90);
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        reputationService.onPostCreated(post);

        assertThat(userReputation.getPoints()).isEqualTo(100);
        assertThat(userReputation.getLevel()).isEqualTo(ReputationLevel.INTERMEDIATE);
    }

    @Test
    void onPostCreated_reaching300Points_upgradesLevelToExpert() {
        userReputation.setPoints(290);
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        reputationService.onPostCreated(post);

        assertThat(userReputation.getPoints()).isEqualTo(300);
        assertThat(userReputation.getLevel()).isEqualTo(ReputationLevel.EXPERT);
    }

    // ── onCommentCreated ─────────────────────────────────────────────────────

    @Test
    void onCommentCreated_existingUser_incrementsPointsAndCommentsCount() {
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        reputationService.onCommentCreated(comment);

        assertThat(userReputation.getPoints()).isEqualTo(55); // 50 + 5
        assertThat(userReputation.getCommentsCount()).isEqualTo(4);

        ArgumentCaptor<ReputationEvent> eventCaptor = ArgumentCaptor.forClass(ReputationEvent.class);
        verify(reputationEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getActionType()).isEqualTo(ReputationActionType.COMMENT_CREATED);
        assertThat(eventCaptor.getValue().getPointsDelta()).isEqualTo(5);
    }

    // ── onPostReactionChanged ────────────────────────────────────────────────

    @Test
    void onPostReactionChanged_newLike_adds2PointsToAuthor() {
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // reactor is different from post author
        reputationService.onPostReactionChanged(post, "reactor99", null, ReactionType.LIKE);

        assertThat(userReputation.getPoints()).isEqualTo(52); // 50 + 2
        assertThat(userReputation.getLikesReceivedCount()).isEqualTo(2);
    }

    @Test
    void onPostReactionChanged_removeLike_removes2PointsFromAuthor() {
        userReputation.setLikesReceivedCount(3);
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // Changing from LIKE to DISLIKE removes the like
        reputationService.onPostReactionChanged(post, "reactor99", ReactionType.LIKE, ReactionType.DISLIKE);

        assertThat(userReputation.getPoints()).isEqualTo(48); // 50 - 2
        assertThat(userReputation.getLikesReceivedCount()).isEqualTo(2);
    }

    @Test
    void onPostReactionChanged_selfReaction_isIgnored() {
        // post author reacts to own post — no reputation change
        reputationService.onPostReactionChanged(post, "user1", null, ReactionType.LIKE);

        verify(userReputationRepository, never()).findByUserId(any());
        verify(userReputationRepository, never()).save(any());
    }

    @Test
    void onPostReactionChanged_dislikeToDislike_noPointsChange() {
        // no delta when switching between non-like types
        reputationService.onPostReactionChanged(post, "reactor99", ReactionType.DISLIKE, ReactionType.DISLIKE);

        verify(userReputationRepository, never()).findByUserId(any());
    }

    @Test
    void onPostReactionChanged_pointsNeverGoBelowZero() {
        userReputation.setPoints(1);
        userReputation.setLikesReceivedCount(0);
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        reputationService.onPostReactionChanged(post, "reactor99", ReactionType.LIKE, ReactionType.DISLIKE);

        assertThat(userReputation.getPoints()).isEqualTo(0); // max(0, 1 - 2) = 0
    }

    // ── onBestAnswerSelected ─────────────────────────────────────────────────

    @Test
    void onBestAnswerSelected_newBestAnswer_grants15Points() {
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        reputationService.onBestAnswerSelected(post, null, comment);

        assertThat(userReputation.getPoints()).isEqualTo(65); // 50 + 15
        assertThat(userReputation.getBestAnswersCount()).isEqualTo(1);
    }

    @Test
    void onBestAnswerSelected_replacingPreviousBest_deductsPreviousAndGrantsNew() {
        Comment previousBest = new Comment();
        previousBest.setId(9L);
        previousBest.setUserId("user2");
        previousBest.setPost(post);

        UserReputation prevReputation = new UserReputation();
        prevReputation.setUserId("user2");
        prevReputation.setPoints(80);
        prevReputation.setLevel(ReputationLevel.BEGINNER);
        prevReputation.setPostsCount(0);
        prevReputation.setCommentsCount(0);
        prevReputation.setLikesReceivedCount(0);
        prevReputation.setBestAnswersCount(1);

        when(userReputationRepository.findByUserId("user2")).thenReturn(Optional.of(prevReputation));
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.save(any(UserReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reputationEventRepository.save(any(ReputationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        reputationService.onBestAnswerSelected(post, previousBest, comment);

        // Previous best loses 15 points
        assertThat(prevReputation.getPoints()).isEqualTo(65);  // 80 - 15
        assertThat(prevReputation.getBestAnswersCount()).isEqualTo(0);

        // New best gains 15 points
        assertThat(userReputation.getPoints()).isEqualTo(65);  // 50 + 15
        assertThat(userReputation.getBestAnswersCount()).isEqualTo(1);
    }

    // ── getProfile ───────────────────────────────────────────────────────────

    @Test
    void getProfile_existingUser_returnsCorrectProfile() {
        when(userReputationRepository.findByUserId("user1")).thenReturn(Optional.of(userReputation));
        when(userReputationRepository.countByPointsGreaterThan(50)).thenReturn(3L);
        when(reputationEventRepository.findTop20ByUserIdOrderByCreatedAtDesc("user1")).thenReturn(List.of());

        ReputationProfileResponse profile = reputationService.getProfile("user1");

        assertThat(profile.getUserId()).isEqualTo("user1");
        assertThat(profile.getPoints()).isEqualTo(50);
        assertThat(profile.getLevel()).isEqualTo("BEGINNER");
        assertThat(profile.getRank()).isEqualTo(4); // 3 users with more points + 1
        assertThat(profile.getPostsCount()).isEqualTo(2);
        assertThat(profile.getCommentsCount()).isEqualTo(3);
    }

    @Test
    void getProfile_nonExistingUser_returnsEmptyProfile() {
        when(userReputationRepository.findByUserId("unknown")).thenReturn(Optional.empty());
        when(userReputationRepository.countByPointsGreaterThan(0)).thenReturn(0L);
        when(reputationEventRepository.findTop20ByUserIdOrderByCreatedAtDesc("unknown")).thenReturn(List.of());

        ReputationProfileResponse profile = reputationService.getProfile("unknown");

        assertThat(profile.getUserId()).isEqualTo("unknown");
        assertThat(profile.getPoints()).isEqualTo(0);
        assertThat(profile.getLevel()).isEqualTo("BEGINNER");
    }

    // ── getLeaderboard ───────────────────────────────────────────────────────

    @Test
    void getLeaderboard_returnsTopUsersSortedByPoints() {
        UserReputation top1 = new UserReputation();
        top1.setUserId("topUser");
        top1.setPoints(500);
        top1.setLevel(ReputationLevel.EXPERT);
        top1.setPostsCount(10);
        top1.setCommentsCount(20);
        top1.setLikesReceivedCount(50);
        top1.setBestAnswersCount(5);

        when(userReputationRepository.findAllByOrderByPointsDescUpdatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(top1));
        when(userReputationRepository.countByPointsGreaterThan(500)).thenReturn(0L);

        List<LeaderboardEntryResponse> leaderboard = reputationService.getLeaderboard(10);

        assertThat(leaderboard).hasSize(1);
        assertThat(leaderboard.get(0).getUserId()).isEqualTo("topUser");
        assertThat(leaderboard.get(0).getPoints()).isEqualTo(500);
        assertThat(leaderboard.get(0).getRank()).isEqualTo(1);
        assertThat(leaderboard.get(0).getLevel()).isEqualTo("EXPERT");
    }

    @Test
    void getLeaderboard_limitClampedToMax100() {
        when(userReputationRepository.findAllByOrderByPointsDescUpdatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of());

        reputationService.getLeaderboard(9999);

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userReputationRepository).findAllByOrderByPointsDescUpdatedAtAsc(pageCaptor.capture());
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getLeaderboard_limitClampedToMin1() {
        when(userReputationRepository.findAllByOrderByPointsDescUpdatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of());

        reputationService.getLeaderboard(0);

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userReputationRepository).findAllByOrderByPointsDescUpdatedAtAsc(pageCaptor.capture());
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(1);
    }
}
