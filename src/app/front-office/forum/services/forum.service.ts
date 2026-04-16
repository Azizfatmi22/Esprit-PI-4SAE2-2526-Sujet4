import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PostRequest, PostResponse, ContentStatus, PostCategory } from '../models/forum.model';
import { CommentRequest, CommentResponse } from '../models/comment.model';
import { ReactionRequest, ReactionCountResponse } from '../models/reaction.model';
import { LeaderboardEntry, ReputationProfile } from '../models/reputation.model';

import { environment } from '../../environment/envirement';

@Injectable({
    providedIn: 'root'
})
export class ForumService {
    private baseApiUrl = `${environment.apiGatewayUrl}/api`;
    private postsApiUrl = `${this.baseApiUrl}/posts`;
    private moderationApiUrl = `${this.baseApiUrl}/moderation`;
    private reputationApiUrl = `${this.baseApiUrl}/reputation`;

    constructor(private http: HttpClient) { }

    // --- Posts ---
    getForums(userId?: string, category?: PostCategory): Observable<PostResponse[]> {
        let params = new HttpParams();
        if (userId && userId.trim().length > 0) {
            params = params.set('userId', userId);
        }
        if (category) {
            params = params.set('category', category);
        }
        return this.http.get<PostResponse[]>(this.postsApiUrl, { params });
    }

    getForumById(id: number): Observable<PostResponse> {
        return this.http.get<PostResponse>(`${this.postsApiUrl}/${id}`);
    }

    createForum(post: PostRequest): Observable<PostResponse> {
        return this.http.post<PostResponse>(this.postsApiUrl, post);
    }

    updateForum(id: number, post: PostRequest): Observable<PostResponse> {
        return this.http.put<PostResponse>(`${this.postsApiUrl}/${id}`, post);
    }

    // --- Moderation ---
    getPendingPosts(): Observable<PostResponse[]> {
        return this.http.get<PostResponse[]>(`${this.moderationApiUrl}/posts/pending`);
    }

    updatePostStatus(id: number, status: ContentStatus): Observable<PostResponse> {
        const params = new HttpParams().set('status', status);
        return this.http.put<PostResponse>(`${this.moderationApiUrl}/posts/${id}/status`, {}, { params });
    }

    getPendingComments(): Observable<CommentResponse[]> {
        return this.http.get<CommentResponse[]>(`${this.moderationApiUrl}/comments/pending`);
    }

    updateCommentStatus(id: number, status: ContentStatus): Observable<CommentResponse> {
        const params = new HttpParams().set('status', status);
        return this.http.put<CommentResponse>(`${this.moderationApiUrl}/comments/${id}/status`, {}, { params });
    }

    // --- Comments ---
    getComments(postId: number): Observable<CommentResponse[]> {
        return this.http.get<CommentResponse[]>(`${this.postsApiUrl}/${postId}/comments`);
    }

    addComment(postId: number, request: CommentRequest): Observable<CommentResponse> {
        return this.http.post<CommentResponse>(`${this.postsApiUrl}/${postId}/comments`, request);
    }

    markBestAnswer(postId: number, commentId: number): Observable<CommentResponse> {
        return this.http.put<CommentResponse>(`${this.postsApiUrl}/${postId}/comments/${commentId}/best-answer`, {});
    }

    // --- Reactions ---
    reactToPost(postId: number, request: ReactionRequest): Observable<void> {
        return this.http.post<void>(`${this.postsApiUrl}/${postId}/reactions`, request);
    }

    getReactionCounts(postId: number): Observable<ReactionCountResponse> {
        return this.http.get<ReactionCountResponse>(`${this.postsApiUrl}/${postId}/reactions/count`);
    }

    // --- Delete ---
    deleteForum(id: number): Observable<void> {
        return this.http.delete<void>(`${this.postsApiUrl}/${id}`);
    }

    getUserReputation(userId: string): Observable<ReputationProfile> {
        return this.http.get<ReputationProfile>(`${this.reputationApiUrl}/users/${userId}`);
    }

    getLeaderboard(limit = 10): Observable<LeaderboardEntry[]> {
        const params = new HttpParams().set('limit', String(limit));
        return this.http.get<LeaderboardEntry[]>(`${this.reputationApiUrl}/leaderboard`, { params });
    }
}
