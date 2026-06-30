import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api';

const client = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface ApiResponseWrapper<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: number;
}

export interface UploadHistoryDto {
  id: number;
  fileName: string;
  fileType: 'CSV' | 'PDF';
  uploadTime: string;
  transformationTime?: string;
  processingDurationMs?: number;
  status: 'PENDING' | 'PROCESSED' | 'ERROR';
  errorMessage?: string;
  fileSize?: number;
  recordsProcessed?: number;
}

export interface ProvenanceDto {
  sourceType: 'CSV' | 'PDF';
  sourceName: string;
  extractionMethod: 'CSV_PARSER' | 'PDF_REGEXP' | 'PDF_HEURISTIC' | 'MANUAL_MERGE';
  extractedAt: string;
  confidenceScore: number;
}

export interface FieldProvenanceDto {
  fieldName: string;
  provenance: ProvenanceDto;
  rawValue: string;
}

export interface CandidateEmailDto {
  id: number;
  email: string;
  provenance: ProvenanceDto;
}

export interface CandidatePhoneDto {
  id: number;
  phone: string;
  provenance: ProvenanceDto;
}

export interface CandidateLinkDto {
  id: number;
  url: string;
  provenance: ProvenanceDto;
}

export interface CandidateSkillDto {
  id: number;
  skillName: string;
  provenance: ProvenanceDto;
}

export interface ExperienceDto {
  id: number;
  company: string;
  role: string;
  startDate: string;
  endDate: string;
  description: string;
  provenance: ProvenanceDto;
}

export interface EducationDto {
  id: number;
  institution: string;
  degree: string;
  fieldOfStudy: string;
  startDate: string;
  endDate: string;
  provenance: ProvenanceDto;
}

export interface ProjectDto {
  id: number;
  title: string;
  description: string;
  technologies: string;
  provenance: ProvenanceDto;
}

export interface CanonicalProfileDto {
  id: number;
  fullName: string;
  headline: string;
  location: string;
  yearsOfExperience: number;
  overallConfidence: number;
  createdAt: string;
  updatedAt: string;
  emails: CandidateEmailDto[];
  phones: CandidatePhoneDto[];
  links: CandidateLinkDto[];
  skills: CandidateSkillDto[];
  experiences: ExperienceDto[];
  educations: EducationDto[];
  projects: ProjectDto[];
  fieldProvenances: FieldProvenanceDto[];
}

export interface TransformationSummaryDto {
  totalRawProcessed: number;
  newProfilesCreated: number;
  profilesMerged: number;
  durationMs: number;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface DashboardStats {
  totalCandidates: number;
  duplicateCandidates: number;
  averageConfidence: number;
  filesUploaded: number;
}

// API methods
export const uploadCSV = async (file: File): Promise<ApiResponseWrapper<UploadHistoryDto>> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await client.post<ApiResponseWrapper<UploadHistoryDto>>('/upload/csv', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const uploadResume = async (file: File): Promise<ApiResponseWrapper<UploadHistoryDto>> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await client.post<ApiResponseWrapper<UploadHistoryDto>>('/upload/resume', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const triggerTransform = async (): Promise<ApiResponseWrapper<TransformationSummaryDto>> => {
  const response = await client.post<ApiResponseWrapper<TransformationSummaryDto>>('/transform');
  return response.data;
};

export const getCandidates = async (params: {
  search?: string;
  skill?: string;
  location?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}): Promise<ApiResponseWrapper<PageResponse<CanonicalProfileDto>>> => {
  const response = await client.get<ApiResponseWrapper<PageResponse<CanonicalProfileDto>>>('/candidates', { params });
  return response.data;
};

export const getCandidateById = async (id: number): Promise<ApiResponseWrapper<CanonicalProfileDto>> => {
  const response = await client.get<ApiResponseWrapper<CanonicalProfileDto>>(`/candidate/${id}`);
  return response.data;
};

export const deleteCandidate = async (id: number): Promise<ApiResponseWrapper<void>> => {
  const response = await client.delete<ApiResponseWrapper<void>>(`/candidate/${id}`);
  return response.data;
};

export const projectCandidate = async (id: number, config: any): Promise<ApiResponseWrapper<any>> => {
  const response = await client.post<ApiResponseWrapper<any>>(`/candidate/${id}/project`, config);
  return response.data;
};

export const getUniqueLocations = async (): Promise<ApiResponseWrapper<string[]>> => {
  const response = await client.get<ApiResponseWrapper<string[]>>('/candidates/locations');
  return response.data;
};

export const getUniqueSkills = async (): Promise<ApiResponseWrapper<string[]>> => {
  const response = await client.get<ApiResponseWrapper<string[]>>('/candidates/skills');
  return response.data;
};

export const getDashboardStats = async (): Promise<ApiResponseWrapper<DashboardStats>> => {
  const response = await client.get<ApiResponseWrapper<DashboardStats>>('/candidates/dashboard-stats');
  return response.data;
};

export const getHistory = async (): Promise<ApiResponseWrapper<UploadHistoryDto[]>> => {
  const response = await client.get<ApiResponseWrapper<UploadHistoryDto[]>>('/history');
  return response.data;
};

export default client;
