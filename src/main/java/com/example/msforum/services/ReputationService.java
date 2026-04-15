package com.example.msforum.services;

import com.example.msforum.dto.LeaderboardEntryResponse;
import com.example.msforum.dto.ReputationProfileResponse;
import com.example.msforum.entities.Comment;
import com.example.msforum.entities.Post;
import com.example.msforum.entities.ReactionType;
import java.util.List;

public interface ReputationService {
    void onPostCreated(Post post);

    void onCommentCreated(Comment comment);

    void onPostReactionChanged(Post post, String reactorUserId, ReactionType previousType, ReactionType newType);

    void onBestAnswerSelected(Post post, Comment previousBestAnswer, Comment newBestAnswer);

    ReputationProfileResponse getProfile(String userId);

    List<LeaderboardEntryResponse> getLeaderboard(int limit);
}
