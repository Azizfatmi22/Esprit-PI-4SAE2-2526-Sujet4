package com.example.msforum.controllers;

import com.example.msforum.dto.LeaderboardEntryResponse;
import com.example.msforum.dto.ReputationProfileResponse;
import com.example.msforum.services.ReputationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reputation")
public class ReputationController {

    private final ReputationService reputationService;

    public ReputationController(ReputationService reputationService) {
        this.reputationService = reputationService;
    }

    @GetMapping("/users/{userId}")
    public ReputationProfileResponse getUserReputation(@PathVariable String userId) {
        return reputationService.getProfile(userId);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryResponse> getLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        return reputationService.getLeaderboard(limit);
    }
}
