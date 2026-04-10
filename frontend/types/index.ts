export interface User {
  id: string
  handle: string
  name?: string
  bio?: string
  avatarUrl?: string
  role: 'admin' | 'moderator' | 'user'
  createdAt: string
}

export interface SkillVersion {
  id: string
  version: string
  tag?: string
  changelog: string
  createdBy: User
  createdAt: string
}

export interface Skill {
  id: string
  slug: string
  displayName: string
  summary?: string
  owner: User
  latestVersion?: SkillVersion
  badges: Record<string, unknown>
  moderationStatus: 'active' | 'hidden' | 'removed'
  capabilityTags: string[]
  statsDownloads: number
  statsStars: number
  statsVersions: number
  statsComments: number
  createdAt: string
  updatedAt: string
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  timestamp: string
  page?: number
  size?: number
  total?: number
}

export interface Comment {
  id: string
  skillId?: string
  soulId?: string
  user: User
  body: string
  createdAt: string
  updatedAt: string
}

export interface TrustedPublisher {
  id: string
  repository: string
  repositoryOwner: string
  workflowFilename: string
  environment: string
  createdAt: string
}

export interface TrustedPublisherRequest {
  repository: string
  repositoryId: string
  repositoryOwner: string
  repositoryOwnerId: string
  workflowFilename: string
  environment: string
}

export interface OidcTokenRequest {
  oidcToken: string
}

export interface OidcTokenResponse {
  token: string
  expiresAt: string
  skillSlug: string
  repository: string
}
