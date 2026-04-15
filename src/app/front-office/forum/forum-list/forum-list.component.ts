import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { forkJoin, Subscription, filter, skip } from 'rxjs';
import { Forum, PostCategory } from '../models/forum.model';
import { ReactionType } from '../models/reaction.model';
import { ForumService } from '../services/forum.service';
import { EventService } from '../../events/services/event.service';
import { HolidayService, Holiday } from '../../events/services/holiday.service';
import { Event } from '../../events/models/event.model';
import { UserService } from '../../services/user.service';
import { LeaderboardEntry, ReputationProfile } from '../models/reputation.model';

interface ForumWithExtras extends Forum {
    likeCount?: number;
    dislikeCount?: number;
    authorName?: string;
    avatarUrl?: string;
    staticImage?: string;
    isReacting?: boolean;
    reactionType?: ReactionType | null;
    isCurrentUser?: boolean;
}

@Component({
    selector: 'app-forum-list',
    templateUrl: './forum-list.component.html',
    styleUrls: ['./forum-list.component.scss']
})
export class ForumListComponent implements OnInit, OnDestroy {
    forums: ForumWithExtras[] = [];
    loading = true;
    ReactionType = ReactionType;
    showMyPosts = false;
    selectedCategory: PostCategory | 'ALL' = 'ALL';
    categories: PostCategory[] = [
        'TECHNOLOGY', 'JAVA', 'CPP', 'AI', 'DESIGN', 'MARKETING', 'BUSINESS', 'LANGUAGES', 'GENERAL'
    ];
    private navSub!: Subscription;

    // Map userId to static names/avatars for demonstration
    userMappings: { [key: string]: { name: string, avatar: string } } = {
        '10': { name: 'John Doe', avatar: 'https://i.pravatar.cc/150?img=12' },
        '1': { name: 'Admin', avatar: 'https://i.pravatar.cc/150?img=32' },
        '2': { name: 'Jane Smith', avatar: 'https://i.pravatar.cc/150?img=45' }
    };

    // Static images for posts
    staticPostImages = [
        'https://images.unsplash.com/photo-1557804506-669a67965ba0?w=800&h=400&fit=crop',
        'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&h=400&fit=crop',
        'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&h=400&fit=crop'
    ];

    // User profile data - loaded from backend
    userPostsCount = 0;

    events: Event[] = [];
    holidays: Holiday[] = [];
    loadingHolidays = false;
    loadingEvents = false;
    eventsError: string | null = null;
    isTrainer = false;
    currentUserId = '';
    currentUserFullName = '';
    currentUserReputation: ReputationProfile | null = null;
    leaderboard: LeaderboardEntry[] = [];

    constructor(
        private forumService: ForumService,
        private eventService: EventService,
        private holidayService: HolidayService,
        private router: Router,
        private userService: UserService
    ) { }

    ngOnInit(): void {
        this.userService.loadUser().then(user => {
            if (user) {
                this.currentUserId = user.id;
                this.currentUserFullName = user.fullName || user.username;
            } else {
                // Fallback to direct check if loadUser didn't return
                const currentUser = this.userService.getUser();
                if (currentUser) {
                    this.currentUserId = currentUser.id;
                    this.currentUserFullName = currentUser.fullName || currentUser.username;
                }
            }
            console.log('Current User ID:', this.currentUserId);
            
            this.isTrainer = this.userService.isTrainer();

            this.loadForums();   // always load immediately on component init
            this.loadEvents();
            this.loadHolidays();
            this.loadReputationData();
            this.loadUserPostsCount();
        });

        // Also reload whenever the user navigates back to this route
        // (skip(1) skips the NavigationEnd that already fired for the current initial load)
        this.navSub = this.router.events.pipe(
            filter(event => event instanceof NavigationEnd),
            skip(1)
        ).subscribe(() => {
            this.loadForums();
        });
    }

    ngOnDestroy(): void {
        this.navSub?.unsubscribe();
    }

    loadEvents(): void {
        this.loadingEvents = true;
        this.eventsError = null;
        this.eventService.getEvents().subscribe({
            next: (events) => {
                this.events = (events || []).slice(0, 3);
                this.loadingEvents = false;
            },
            error: (err) => {
                console.error('Error loading events:', err);
                this.events = [];
                this.eventsError = 'Failed to load events.';
                this.loadingEvents = false;
            }
        });
    }

    loadHolidays(): void {
        const currentYear = new Date().getFullYear();
        this.loadingHolidays = true;
        this.holidayService.getHolidays(currentYear).subscribe({
            next: (holidays) => {
                const today = new Date();
                this.holidays = (holidays || [])
                    .filter(h => new Date(h.date) >= today)
                    .slice(0, 5);
                this.loadingHolidays = false;
            },
            error: (err) => {
                console.error('Error loading holidays:', err);
                this.holidays = [];
                this.loadingHolidays = false;
            }
        });
    }

    formatHolidayDate(dateStr: string): string {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }

    loadReputationData(): void {
        if (this.currentUserId) {
            this.forumService.getUserReputation(this.currentUserId).subscribe({
                next: (profile) => {
                    this.currentUserReputation = profile;
                },
                error: (err) => {
                    console.error('Error loading current user reputation:', err);
                    this.currentUserReputation = null;
                }
            });
        }
        this.forumService.getLeaderboard(5).subscribe({
            next: (rows) => {
                this.leaderboard = rows.map(row => ({
                    ...row,
                    fullName: this.getLeaderboardUserName(row)
                }));
            },
            error: (err) => {
                console.error('Error loading leaderboard:', err);
                this.leaderboard = [];
            }
        });
    }

    getLeaderboardUserName(entry: LeaderboardEntry): string {
        if (entry.userId === this.currentUserId && this.currentUserFullName) {
            return this.currentUserFullName;
        }
        const mapping = this.userMappings[entry.userId];
        if (mapping) {
            return mapping.name;
        }
        return entry.fullName || `User ${entry.userId}`;
    }

    loadUserPostsCount(): void {
        if (!this.currentUserId) return;
        this.forumService.getForums(this.currentUserId).subscribe({
            next: (posts) => {
                this.userPostsCount = posts.length;
            },
            error: (err) => {
                console.error('Error loading user posts count:', err);
                this.userPostsCount = 0;
            }
        });
    }

    loadForums(): void {
        this.loading = true;
        const selectedUserId = this.isTrainer && this.showMyPosts ? this.currentUserId : undefined;
        const categoryFilter = this.selectedCategory === 'ALL' ? undefined : this.selectedCategory as PostCategory;

        this.forumService.getForums(selectedUserId, categoryFilter).subscribe({
            next: (posts) => {
                if (posts.length === 0) {
                    this.forums = [];
                    this.loading = false;
                    return;
                }

                // For each post, fetch reaction counts
                const requests = posts.map(post =>
                    this.forumService.getReactionCounts(post.id!)
                );

                forkJoin(requests).subscribe({
                    next: (counts) => {
                        this.forums = posts.map((post, index) => ({
                            ...post,
                            likeCount: counts[index].likeCount,
                            dislikeCount: counts[index].dislikeCount,
                            authorName: this.getAuthorName(post.userId),
                            avatarUrl: this.getAuthorAvatar(post.userId),
                            staticImage: this.staticPostImages[index % this.staticPostImages.length],
                            isCurrentUser: post.userId === this.currentUserId
                        }));
                        this.loading = false;
                    },
                    error: (err) => {
                        console.error('Error fetching reaction counts:', err);
                        // Still show posts even if counts fail
                        this.forums = posts.map((post, index) => ({
                            ...post,
                            authorName: this.getAuthorName(post.userId),
                            avatarUrl: this.getAuthorAvatar(post.userId),
                            staticImage: this.staticPostImages[index % this.staticPostImages.length],
                            isCurrentUser: post.userId === this.currentUserId
                        }));
                        this.loading = false;
                    }
                });
            },
            error: (error) => {
                console.error('Error loading forums:', error);
                this.loading = false;
            }
        });
    }

    onCategoryChange(category: PostCategory | 'ALL'): void {
        this.selectedCategory = category;
        this.loadForums();
    }

    onReact(event: MouseEvent, postId: number, type: ReactionType): void {
        event.stopPropagation();
        const post = this.forums.find(f => f.id === postId);
        if (!post) return;

        // Optimistic Update
        const previousLikes = post.likeCount || 0;
        const previousDislikes = post.dislikeCount || 0;

        // Simple logic: if clicking like, increment like. 
        // Real logic usually handles toggling/switching, but for "live feel" we increment.
        if (type === ReactionType.LIKE) {
            post.likeCount = (post.likeCount || 0) + 1;
        } else {
            post.dislikeCount = (post.dislikeCount || 0) + 1;
        }

        post.isReacting = true;
        post.reactionType = type;

        this.forumService.reactToPost(postId, { userId: this.currentUserId, type }).subscribe({
            next: () => {
                // Remove animation class after a short delay
                setTimeout(() => post.isReacting = false, 600);

                // Final Sync with server
                this.forumService.getReactionCounts(postId).subscribe(counts => {
                    post.likeCount = counts.likeCount;
                    post.dislikeCount = counts.dislikeCount;
                });
                this.loadReputationData();
            },
            error: (err) => {
                console.error('Error reacting to post:', err);
                // Revert on error
                post.likeCount = previousLikes;
                post.dislikeCount = previousDislikes;
                post.isReacting = false;
                post.reactionType = null;
            }
        });
    }

    viewForum(id: number | undefined): void {
        if (id) {
            this.router.navigate(['/forum', id]);
        }
    }

    createForum(): void {
        this.router.navigate(['/forum/create']);
    }

    deleteForum(event: MouseEvent, id: number | undefined): void {
        event.stopPropagation();
        if (id && confirm('Are you sure you want to delete this forum?')) {
            this.forumService.deleteForum(id).subscribe({
                next: () => {
                    this.loadForums();
                },
                error: (error) => {
                    console.error('Error deleting forum:', error);
                }
            });
        }
    }

    getAuthorName(userId: string): string {
        if (userId === this.currentUserId && this.currentUserFullName) {
            return this.currentUserFullName;
        }
        const mapping = this.userMappings[userId];
        if (mapping) {
            return mapping.name;
        }
        return `User ${userId}`;
    }

    getAuthorAvatar(userId: string): string {
        const name = this.getAuthorName(userId);
        return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=FC8EAC&color=fff&size=128`;
    }

    formatDate(date: any): string {
        const now = new Date();
        const postDate = new Date(date);
        const diffMs = now.getTime() - postDate.getTime();
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 60) {
            return `${diffMins}m ago`;
        } else if (diffHours < 24) {
            return `${diffHours}h ago`;
        } else if (diffDays < 7) {
            return `${diffDays}d ago`;
        } else {
            return postDate.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric'
            });
        }
    }

    formatEventDate(date: any): { month: string; day: string } {
        const eventDate = new Date(date);
        return {
            month: eventDate.toLocaleDateString('en-US', { month: 'short' }).toUpperCase(),
            day: eventDate.getDate().toString()
        };
    }

    get displayedForums(): ForumWithExtras[] {
        if (this.showMyPosts) {
            return this.forums.filter(f => f.userId === this.currentUserId);
        }
        return this.forums;
    }

    setPostFilter(showMyPosts: boolean): void {
        this.showMyPosts = showMyPosts;
        this.loadForums();
    }

    formatLevel(level: string | undefined): string {
        if (!level) return 'Beginner';
        const normalized = level.toLowerCase();
        return normalized.charAt(0).toUpperCase() + normalized.slice(1);
    }
}
