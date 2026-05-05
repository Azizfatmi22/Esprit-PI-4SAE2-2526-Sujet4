import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PostRequest, PostResponse, PostCategory } from '../models/forum.model';
import { ForumService } from '../services/forum.service';
import { ImageUploadService } from '../services/image-upload.service';
import { UserService } from '../../services/user.service';

@Component({
    selector: 'app-forum-form',
    templateUrl: './forum-form.component.html',
    styleUrls: ['./forum-form.component.scss']
})
export class ForumFormComponent implements OnInit {
    forum: PostRequest = {
        title: '',
        content: '',
        userId: '',
        formationId: 5,
        category: 'GENERAL',
        imageUrl: ''
    };

    categories: PostCategory[] = [
        'TECHNOLOGY', 'JAVA', 'CPP', 'AI', 'DESIGN', 'MARKETING', 'BUSINESS', 'LANGUAGES', 'GENERAL'
    ];

    isEditMode = false;
    forumId: number | null = null;
    loading = false;
    imagePreview: string | null = null;
    selectedFile: File | null = null;
    uploadingImage = false;
    trainerName: string = '';

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private forumService: ForumService,
        private imageUploadService: ImageUploadService,
        private userService: UserService
    ) { }

    ngOnInit(): void {
        if (!this.userService.isTrainer()) {
            this.router.navigate(['/forum']);
            return;
        }
        const user = this.userService.getUser();
        if (user) {
            this.forum.userId = user.id;
            this.trainerName = user.fullName;
        } else {
            // fallback if not fully loaded yet or no session
            this.forum.userId = 'unknown';
            this.trainerName = 'Unknown Trainer';
        }

        this.route.params.subscribe(params => {
            if (params['id']) {
                this.isEditMode = true;
                this.forumId = +params['id'];
                this.loadForum();
            }
        });
    }

    loadForum(): void {
        if (this.forumId) {
            this.loading = true;
            this.forumService.getForumById(this.forumId).subscribe({
                next: (forum: PostResponse) => {
                    if (forum) {
                        this.forum = forum;
                        if (forum.imageUrl) {
                            this.imagePreview = forum.imageUrl;
                        }
                    }
                    this.loading = false;
                },
                error: (error) => {
                    console.error('Error loading forum:', error);
                    this.loading = false;
                }
            });
        }
    }

    onImageSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            const file = input.files[0];
            if (file.type.startsWith('image/')) {
                this.selectedFile = file;
                const reader = new FileReader();
                reader.onload = (e) => {
                    this.imagePreview = e.target?.result as string;
                };
                reader.readAsDataURL(file);
            } else {
                alert('Please select an image file.');
            }
        }
    }

    removeImage(): void {
        this.selectedFile = null;
        this.imagePreview = null;
        this.forum.imageUrl = '';
    }

    saveForum(): void {
        if (!this.validateForm()) {
            return;
        }

        this.loading = true;

        if (this.selectedFile) {
            this.uploadingImage = true;
            this.imageUploadService.uploadImage(this.selectedFile).subscribe({
                next: (response) => {
                    if (response.url) {
                        this.forum.imageUrl = response.url;
                    }
                    this.uploadingImage = false;
                    this.createOrUpdatePost();
                },
                error: (error) => {
                    console.error('Error uploading image:', error);
                    this.uploadingImage = false;
                    this.createOrUpdatePost();
                }
            });
        } else {
            this.createOrUpdatePost();
        }
    }

    createOrUpdatePost(): void {
        if (this.isEditMode && this.forumId) {
            const updatePayload: PostRequest = {
                title: this.forum.title,
                content: this.forum.content,
                userId: this.forum.userId,
                formationId: this.forum.formationId,
                category: this.forum.category,
                imageUrl: this.forum.imageUrl
            };

            this.forumService.updateForum(this.forumId, updatePayload).subscribe({
                next: (response) => {
                    this.router.navigate(['/forum']);
                },
                error: (error: any) => {
                    console.error('Error updating forum:', error);
                    this.loading = false;
                }
            });
        } else {
            const createPayload: PostRequest = {
                title: this.forum.title,
                content: this.forum.content,
                userId: this.forum.userId,
                formationId: this.forum.formationId,
                category: this.forum.category
            };
            if (this.forum.imageUrl) {
                createPayload.imageUrl = this.forum.imageUrl;
            }

            this.forumService.createForum(createPayload).subscribe({
                next: (createdForum) => {
                    if (createdForum.status === 'REJECTED') {
                        alert('Your post has been rejected due to offensive content. It will be reviewed by an admin.');
                    } else if (createdForum.status === 'PENDING') {
                        alert('Your post is under review for potential spam. It will be visible once approved by an admin.');
                    }
                    this.router.navigate(['/forum']);
                },
                error: (error) => {
                    console.error('Error creating forum:', error);
                    this.loading = false;
                }
            });
        }
    }

    validateForm(): boolean {
        if (!this.forum.title?.trim()) {
            alert('Please enter a title');
            return false;
        }
        if (!this.forum.content?.trim()) {
            alert('Please enter content');
            return false;
        }
        return true;
    }

    cancel(): void {
        if (this.isEditMode && this.forumId) {
            this.router.navigate(['/forum', this.forumId]);
        } else {
            this.router.navigate(['/forum']);
        }
    }
}
