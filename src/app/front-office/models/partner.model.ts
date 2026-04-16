export enum City {
    ARIANA = 'ARIANA',
    BEJA = 'BEJA',
    BEN_AROUS = 'BEN_AROUS',
    BIZERTE = 'BIZERTE',
    GABES = 'GABES',
    GAFSA = 'GAFSA',
    JENDOUBA = 'JENDOUBA',
    KAIROUAN = 'KAIROUAN',
    KASSERINE = 'KASSERINE',
    KEBILI = 'KEBILI',
    KEF = 'KEF',
    MAHDIA = 'MAHDIA',
    MANOUBA = 'MANOUBA',
    MEDENINE = 'MEDENINE',
    MONASTIR = 'MONASTIR',
    NABEUL = 'NABEUL',
    SFAX = 'SFAX',
    SIDI_BOUZID = 'SIDI_BOUZID',
    SILIANA = 'SILIANA',
    SOUSSE = 'SOUSSE',
    TATAOUINE = 'TATAOUINE',
    TOZEUR = 'TOZEUR',
    TUNIS = 'TUNIS',
    ZAGHOUAN = 'ZAGHOUAN'
}

export enum DocumentType {
    BUSINESS_REGISTRATION = 'BUSINESS_REGISTRATION',
    COMPANY_PROFILE = 'COMPANY_PROFILE',
    LOGO = 'LOGO'
}

export enum LegalForm {
    SARL = 'SARL',
    SUARL = 'SUARL',
    SA = 'SA',
    SAS = 'SAS',
    EURL = 'EURL'
}

export enum PartnershipType {
    TRAINING_PARTNER = 'TRAINING_PARTNER',
    SPONSOR = 'SPONSOR',
    CONTENT_PARTNER = 'CONTENT_PARTNER',
    CERTIFICATION_PARTNER = 'CERTIFICATION_PARTNER'
}

export enum PartnerStatus {
    PENDING = 'PENDING',
    INVESTIGATING = 'INVESTIGATING',
    ACCEPTED = 'ACCEPTED',
    REJECTED = 'REJECTED'
}

export enum PartnerTier {
    BRONZE = 'BRONZE',
    SILVER = 'SILVER',
    GOLD = 'GOLD',
    PLATINUM = 'PLATINUM'
}

export interface PartnerDocument {
    id?: string;
    fileName: string;
    documentType: DocumentType | string;
    filePath: string;
}

export interface PartnerHiring {
    id?: string;
    organizationName: string;
    legalForm: LegalForm;
    email: string;
    phone: string;
    website: string;
    city: City;
    address: string;
    partnershipType: PartnershipType;
    status?: PartnerStatus;
    trustScore?: number;
    tier?: PartnerTier;
    trustAnalysis?: string;
    documents?: PartnerDocument[];
    createdAt?: Date;
    updatedAt?: Date;
}
