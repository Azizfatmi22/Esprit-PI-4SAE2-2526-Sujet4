import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Forum } from '../models/forum.model';
import { ForumService } from '../services/forum.service';
import { UserService } from '../../services/user.service';
import { CommentResponse } from '../models/comment.model';
import { ReactionCountResponse, ReactionType } from '../models/reaction.model';
import { ReputationProfile } from '../models/reputation.model';

@Component({
    selector: 'app-forum-details',
    templateUrl: './forum-details.component.html',
    styleUrls: ['./forum-details.component.scss']
})
export class ForumDetailsComponent implements OnInit, OnDestroy {
    forum: Forum | undefined;
    comments: CommentResponse[] = [];
    reactionCounts: ReactionCountResponse = { likeCount: 0, dislikeCount: 0 };
    loading = true;
    forumId: number | null = null;
    newComment = '';
    submittingComment = false;
    ReactionType = ReactionType;
    isReacting = false;
    currentReactionType: ReactionType | null = null;
    isTrainer = false;
    currentUserId = '';
    currentUserFullName = '';
    userReputationMap: { [userId: string]: ReputationProfile } = {};

    userMappings: { [key: string]: { name: string, avatar: string } } = {
        '10': { name: 'John Doe', avatar: 'https://i.pravatar.cc/150?img=12' },
        '1': { name: 'Admin', avatar: 'https://i.pravatar.cc/150?img=32' },
        '2': { name: 'Jane Smith', avatar: 'https://i.pravatar.cc/150?img=45' }
    };

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private forumService: ForumService,
        private cdr: ChangeDetectorRef,
        private userService: UserService
    ) { }

    ngOnInit(): void {
        const user = this.userService.getUser();
        if (user) {
            this.currentUserId = user.id;
            this.currentUserFullName = user.fullName || user.username;
        }
        this.isTrainer = this.userService.isTrainer();

        this.route.params.subscribe(params => {
            this.forumId = +params['id'];
            this.loadAll();
        });
    }

    ngOnDestroy(): void { }

    /** Load post + comments + reactions in parallel; set loading=false only when all are ready */
    loadAll(): void {
        if (!this.forumId) return;
        this.loading = true;

        forkJoin({
            forum: this.forumService.getForumById(this.forumId),
            comments: this.forumService.getComments(this.forumId).pipe(catchError(() => of([]))),
            reactions: this.forumService.getReactionCounts(this.forumId).pipe(
                catchError(() => of({ likeCount: 0, dislikeCount: 0 }))
            )
        }).subscribe({
            next: ({ forum, comments, reactions }) => {
                this.forum = forum;
                this.comments = comments;
                this.reactionCounts = reactions;
                this.loadUsersReputation();
                this.loading = false;
                this.cdr.markForCheck();   // force view update regardless of change-detection strategy
            },
            error: (error) => {
                console.error('Error loading forum:', error);
                this.loading = false;
                this.cdr.markForCheck();
            }
        });
    }

    loadComments(): void {
        if (this.forumId) {
            this.forumService.getComments(this.forumId).subscribe({
                next: (comments) => {
                    this.comments = comments;
                    this.loadUsersReputation();
                    this.cdr.markForCheck();
                },
                error: (err) => console.error('Error loading comments:', err)
            });
        }
    }

    loadReactions(): void {
        if (this.forumId) {
            this.forumService.getReactionCounts(this.forumId).subscribe({
                next: (counts) => {
                    this.reactionCounts = counts;
                    this.cdr.markForCheck();
                },
                error: (err) => console.error('Error loading reactions:', err)
            });
        }
    }

    onReact(type: ReactionType): void {
        if (!this.forumId) return;

        const previousCounts = { ...this.reactionCounts };

        if (type === ReactionType.LIKE) {
            this.reactionCounts.likeCount++;
        } else {
            this.reactionCounts.dislikeCount++;
        }

        this.isReacting = true;
        this.currentReactionType = type;
        this.cdr.markForCheck();

        this.forumService.reactToPost(this.forumId, { userId: this.currentUserId, type }).subscribe({
            next: () => {
                setTimeout(() => {
                    this.isReacting = false;
                    this.cdr.markForCheck();
                }, 600);
                this.loadReactions();
            },
            error: (err) => {
                console.error('Error reacting:', err);
                this.reactionCounts = previousCounts;
                this.isReacting = false;
                this.currentReactionType = null;
                this.cdr.markForCheck();
            }
        });
    }

    addComment(): void {
        if (this.forumId && this.newComment.trim()) {
            this.submittingComment = true;
            this.forumService.addComment(this.forumId, {
                userId: this.currentUserId,
                content: this.newComment
            }).subscribe({
                next: (comment) => {
                    if (comment.status === 'REJECTED') {
                        alert('Your comment has been rejected due to offensive content. It will be reviewed by an admin.');
                    } else if (comment.status === 'PENDING') {
                        alert('Your comment is under review for potential spam. It will be visible once approved by an admin.');
                    }
                    this.comments = [...this.comments, comment];   // new array ref triggers change detection
                    this.loadUsersReputation();
                    this.newComment = '';
                    this.submittingComment = false;
                    this.cdr.markForCheck();
                },
                error: (err) => {
                    console.error('Error adding comment:', err);
                    this.submittingComment = false;
                    this.cdr.markForCheck();
                }
            });
        }
    }

    getAuthorInfo(userId: string) {
        if (userId === this.currentUserId && this.currentUserFullName) {
            return {
                name: this.currentUserFullName,
                avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(this.currentUserFullName)}&background=FC8EAC&color=fff&size=128`
            };
        }
        const mapping = this.userMappings[userId];
        if (mapping) {
            return mapping;
        }
        return {
            name: `User ${userId}`,
            avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(userId)}&background=FC8EAC&color=fff&size=128`
        };
    }

    loadUsersReputation(): void {
        const ids = new Set<string>();
        if (this.forum?.userId) {
            ids.add(this.forum.userId);
        }
        this.comments.forEach(comment => ids.add(comment.userId));
        ids.forEach((id) => {
            this.forumService.getUserReputation(id).subscribe({
                next: (profile) => {
                    this.userReputationMap[id] = profile;
                    this.cdr.markForCheck();
                },
                error: () => { }
            });
        });
    }

    markAsBestAnswer(commentId?: number): void {
        if (!this.forumId || !commentId) return;
        this.forumService.markBestAnswer(this.forumId, commentId).subscribe({
            next: () => {
                this.loadAll();
            },
            error: (err) => {
                console.error('Error marking best answer:', err);
            }
        });
    }

    canMarkBestAnswer(comment: CommentResponse): boolean {
        if (!this.isTrainer) return false;
        return !comment.isBestAnswer;
    }

    getUserLevel(userId: string): string {
        const level = this.userReputationMap[userId]?.level;
        if (!level) return 'Beginner';
        const normalized = level.toLowerCase();
        return normalized.charAt(0).toUpperCase() + normalized.slice(1);
    }

    goBack(): void {
        this.router.navigate(['/forum']);
    }

    editForum(): void {
        if (this.forumId) {
            this.router.navigate(['/forum', this.forumId, 'edit']);
        }
    }

    deleteForum(): void {
        if (this.forumId && confirm('Are you sure you want to delete this forum?')) {
            this.forumService.deleteForum(this.forumId).subscribe({
                next: () => {
                    this.router.navigate(['/forum']);
                },
                error: (error) => {
                    console.error('Error deleting forum:', error);
                }
            });
        }
    }

    formatDate(date: any): string {
        if (!date) return 'Just now';
        return new Date(date).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}
