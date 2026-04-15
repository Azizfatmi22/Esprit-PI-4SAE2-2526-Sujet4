export interface Event {
    id?: number;
    name: string;
    description: string;
    details: string;
    location: string;
    latitude: number;
    longitude: number;
    imageUrl?: string;
    date: Date;
    endDate?: Date;
    status?: 'UPCOMING' | 'ONGOING' | 'FINISHED' | 'CANCELLED' | 'FULL';
    attendees: number;
    maxParticipants?: number;
    currentParticipants?: number;
    category: string;
    createdAt: Date;
    updatedAt?: Date;
    forumTopicId?: number;
}
