
export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface TokenPair {
  accessToken: string
  refreshToken?: string
  tokenType?: string
}

export interface LoginResponse {
  userId: number
  username: string
  role: string
  tokenPair: TokenPair
}

export interface ChunkUploadState {
  uploadId: string
  originalFilename: string
  contentType?: string
  totalSize: number
  totalChunks: number
  status: string
  uploadedChunks: number[]
  uploadedCount: number
  readyToComplete: boolean
}

export interface UploadResult {
  uploadId?: string
  fieldName?: string
  originalFilename?: string
  filename?: string
  filepath: string
  url?: string
  signedUrl?: string
  size?: number
  contentType?: string
  fileMd5?: string
  storageType?: string
  metadata?: Record<string, unknown>
}

export interface ActivityLog {
  id: number
  time: string
  level: 'info' | 'warn' | 'error'
  message: string
}

export interface RequestOptions extends RequestInit {
  auth?: boolean
  bearerToken?: string
}

export interface QrCheckResult {
  status: string
  avatar?: string
  nickname?: string
  tokenPair?: TokenPair
  token?: string
}
