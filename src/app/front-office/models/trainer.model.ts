export enum TrainerStatus {
    PENDING = 'PENDING',
    ACCEPTED = 'ACCEPTED',
    REJECTED = 'REJECTED'
}

export enum Technology {
    JAVA = 'JAVA',
    PYTHON = 'PYTHON',
    JAVASCRIPT = 'JAVASCRIPT',
    TYPESCRIPT = 'TYPESCRIPT',
    CSHARP = 'CSHARP',
    PHP = 'PHP',
    GO = 'GO',
    RUST = 'RUST',
    SWIFT = 'SWIFT',
    KOTLIN = 'KOTLIN'
}

export interface TrainerHiring {
    id?: string;
    name: string;
    forename: string;
    location: string;
    email: string;
    phone: string;
    motivationLetter: string;
    yearsOfExperience: number;
    technology: Technology | string;
    partnerId: string;
    partnerName?: string;
    jobId?: string;
    jobTitle?: string;

    // Intelligence Scores
    skillSyncScore?: number;
    plagiarismFlag?: boolean;
    toneClarityScore?: number;
    acceptanceProbability?: number;
    intelligentAnalysisContext?: string;
    isBlankCv?: boolean;

    status?: TrainerStatus;
    score?: number;
    createdAt?: string;
    updatedAt?: string;
}
