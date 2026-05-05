import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ForumService } from '../../front-office/forum/services/forum.service';
import { PostResponse, ContentStatus } from '../../front-office/forum/models/forum.model';
import { CommentResponse } from '../../front-office/forum/models/comment.model';
import { RibbonsComponent } from '../../front-office/events/ribbons/ribbons.component';

@Component({
    selector: 'app-forum-admin',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink, RibbonsComponent],
    templateUrl: './forum-admin.component.html',
    styleUrls: ['./forum-admin.component.scss']
})
export class ForumAdminComponent implements OnInit {
    forums: PostResponse[] = [];
    pendingPosts: PostResponse[] = [];
    pendingComments: CommentResponse[] = [];
    loading = true;
    currentTab: 'all' | 'pending-posts' | 'pending-comments' = 'all';

    stats = {
        totalPosts: 0,
        pendingPosts: 0,
        pendingComments: 0
    };

    searchTerm: string = '';

    constructor(
        private forumService: ForumService,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.loadAllData();
    }

    loadAllData(): void {
        this.loading = true;
        this.forumService.getForums().subscribe({
            next: (data: PostResponse[]) => {
                this.forums = data;
                this.stats.totalPosts = data.length;
                this.loadPendingData();
            },
            error: (error: any) => {
                console.error('Error loading forums for admin:', error);
                this.loading = false;
            }
        });
    }

    loadPendingData(): void {
        this.forumService.getPendingPosts().subscribe({
            next: (data: PostResponse[]) => {
                this.pendingPosts = data;
                this.stats.pendingPosts = data.length;
                
                this.forumService.getPendingComments().subscribe({
                    next: (comments: CommentResponse[]) => {
                        this.pendingComments = comments;
                        this.stats.pendingComments = comments.length;
                        this.loading = false;
                    },
                    error: () => this.loading = false
                });
            },
            error: () => this.loading = false
        });
    }

    approvePost(id: number | undefined): void {
        if (!id) return;
        this.forumService.updatePostStatus(id, 'APPROVED').subscribe(() => {
            this.pendingPosts = this.pendingPosts.filter(p => p.id !== id);
            this.stats.pendingPosts--;
            this.loadAllData(); // Refresh all to show the new approved post
        });
    }

    rejectPost(id: number | undefined): void {
        if (!id) return;
        this.forumService.updatePostStatus(id, 'REJECTED').subscribe(() => {
            this.pendingPosts = this.pendingPosts.filter(p => p.id !== id);
            this.stats.pendingPosts--;
        });
    }

    approveComment(id: number | undefined): void {
        if (!id) return;
        this.forumService.updateCommentStatus(id, 'APPROVED').subscribe(() => {
            this.pendingComments = this.pendingComments.filter(c => c.id !== id);
            this.stats.pendingComments--;
        });
    }

    rejectComment(id: number | undefined): void {
        if (!id) return;
        this.forumService.updateCommentStatus(id, 'REJECTED').subscribe(() => {
            this.pendingComments = this.pendingComments.filter(c => c.id !== id);
            this.stats.pendingComments--;
        });
    }

    deletePost(id: number | undefined): void {
        if (id && confirm('Are you sure you want to delete this post? This action cannot be undone.')) {
            this.forumService.deleteForum(id).subscribe({
                next: () => {
                    this.forums = this.forums.filter(f => f.id !== id);
                    this.stats.totalPosts = this.forums.length;
                },
                error: (error: any) => {
                    console.error('Error deleting post:', error);
                    alert('Failed to delete post. Please try again.');
                }
            });
        }
    }

    viewPost(id: number | undefined): void {
        if (id) {
            this.router.navigate(['/forum', id]);
        }
    }

    editPost(id: number | undefined): void {
        if (id) {
            this.router.navigate(['/forum/edit', id]);
        }
    }

    get filteredForums() {
        return this.forums.filter(f =>
            f.title.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
            f.content.toLowerCase().includes(this.searchTerm.toLowerCase())
        );
    }

    formatDate(dateStr: string | undefined): string {
        if (!dateStr) return 'N/A';
        return new Date(dateStr).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }
}
