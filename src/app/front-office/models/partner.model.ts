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
    CERTIFICATION_PARTNER = 'CERTIFICATION_PARTNER'
}

export enum PartnerStatus {
    PENDING = 'PENDING',
    ACCEPTED = 'ACCEPTED',
    REJECTED = 'REJECTED'
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
    documents?: PartnerDocument[];
    createdAt?: Date;
    updatedAt?: Date;
}
