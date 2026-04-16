export type ContentStatus = 'APPROVED' | 'PENDING' | 'REJECTED';

export type PostCategory = 'TECHNOLOGY' | 'JAVA' | 'CPP' | 'AI' | 'DESIGN' | 'MARKETING' | 'BUSINESS' | 'LANGUAGES' | 'GENERAL';

export interface Forum {
    id?: number;
    userId: string;
    formationId: number;
    title: string;
    content: string;
    category?: PostCategory;
    imageUrl?: string;
    bestAnswerCommentId?: number;
    createdAt?: string;
    status?: ContentStatus;
    reviewedByAdmin?: boolean;
}

export interface PostRequest {
    userId: string;
    formationId: number;
    title: string;
    content: string;
    category?: PostCategory;
    imageUrl?: string;
}

export interface PostResponse extends Forum { }
