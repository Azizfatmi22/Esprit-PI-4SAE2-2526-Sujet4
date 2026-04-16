import { ContentStatus } from './forum.model';

export interface Comment {
    id?: number;
    userId: string;
    content: string;
    isBestAnswer?: boolean;
    createdAt?: string;
    status?: ContentStatus;
    reviewedByAdmin?: boolean;
}

export interface CommentRequest {
    userId: string;
    content: string;
}

export interface CommentResponse extends Comment { }
