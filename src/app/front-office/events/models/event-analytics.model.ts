export interface EventParticipation {
    eventName: string;
    currentParticipants: number;
    maxParticipants: number;
}

export interface EventAnalytics {
    totalEvents: number;
    upcomingEvents: number;
    ongoingEvents: number;
    finishedEvents: number;
    averageParticipationRate: number;
    mostPopularEventName: string;
    eventParticipations?: EventParticipation[];
}
