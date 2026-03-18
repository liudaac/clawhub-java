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
