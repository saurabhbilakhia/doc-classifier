export type Role = 'ADMIN' | 'USER';
export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type DataType = 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'CURRENCY';
export type RuleType = 'REGEX' | 'JSON_PATH' | 'XPATH';

export interface Tokens { accessToken: string; refreshToken: string; }
export interface JwtClaims { sub: string; email?: string; role?: Role; exp?: number; }

export interface DocumentDto {
  id: number;
  filename: string;
  mimeType?: string;
  sizeBytes?: number;
  status: DocumentStatus;
  classificationName?: string | null;
  summary?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ExtractedDataPoint {
  id: number;
  key: string;
  valueString?: string | null;
  valueNumber?: string | null;
  valueDate?: string | null;
  confidence?: number | null;
  page?: number | null;
}

export interface Classification {
  id: number;
  name: string;
  description?: string | null;
  priority: number;
  threshold: number;
}

export interface ClassificationPattern {
  id: number;
  classificationId: number;
  pattern: string;
  flags?: string | null;
}

export interface DataPointDefinition {
  id: number;
  classificationId: number;
  key: string;
  label?: string | null;
  type: DataType;
  ruleType: RuleType;
  expression: string;
  required: boolean;
}
