export interface ReputationEvent {
    actionType: string;
    pointsDelta: number;
    sourceType: string;
    sourceId: string;
    createdAt: string;
}

export interface ReputationProfile {
    userId: string;
    points: number;
    level: 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT';
    rank: number;
    postsCount: number;
    commentsCount: number;
    likesReceivedCount: number;
    bestAnswersCount: number;
    recentEvents: ReputationEvent[];
}

export interface LeaderboardEntry {
    rank: number;
    userId: string;
    fullName?: string;
    points: number;
    level: 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT';
}
