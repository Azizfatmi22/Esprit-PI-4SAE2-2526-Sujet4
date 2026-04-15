export enum ReactionType {
    LIKE = 'LIKE',
    DISLIKE = 'DISLIKE'
}

export interface ReactionRequest {
    userId: string;
    type: ReactionType;
}

export interface ReactionCountResponse {
    likeCount: number;
    dislikeCount: number;
    postId?: number;
}
