// API Types - Generated from OpenAPI spec

export type UserRole = 'ADMIN' | 'PARENT' | 'CHILD';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'CHILD_COMPLETED' | 'APPROVED' | 'REJECTED' | 'EXPIRED';
export type RecurrenceType = 'ONCE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type RewardStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'DELIVERED';
export type PointTransactionType = 'TASK' | 'REWARD' | 'BEHAVIOR' | 'PENALTY' | 'BONUS';
export type NotificationType = 'TASK_COMPLETED' | 'TASK_APPROVED' | 'TASK_REJECTED' | 'TASK_EXPIRED' | 'REWARD_REQUESTED' | 'REWARD_APPROVED' | 'REWARD_REJECTED' | 'POINTS_EARNED';

// User
export interface UserResponse {
  id: number;
  username: string;
  firstName: string;
  role: UserRole;
  active: boolean;
  theme?: string;
  language?: string;
  avatarType?: string;
  avatarIconName?: string;
  avatarPath?: string;
  totalPoints: number;
  createdAt: string;
}

export interface ChildResponse {
  id: number;
  username: string;
  firstName: string;
  avatarType?: string;
  avatarIconName?: string;
  avatarPath?: string;
  totalPoints: number;
}

export interface UserProfileResponse {
  id: number;
  username: string;
  firstName: string;
  role: UserRole;
  theme?: string;
  language?: string;
  avatarType?: string;
  avatarIconName?: string;
  avatarPath?: string;
  totalPoints: number;
}

export interface UpdateProfileRequest {
  username?: string;
  firstName?: string;
  theme?: 'LIGHT' | 'DARK';
  language?: 'en' | 'de';
  avatarType?: 'DEFAULT' | 'ICON' | 'PHOTO';
  avatarIconName?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UsernameCheckResponse {
  available: boolean;
  message: string;
}

// Auth
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// Task Definition
export interface TaskDefinitionResponse {
  id: number;
  title: string;
  description?: string;
  basePoints: number;
  recurrenceType: RecurrenceType;
  assignedUsers: ChildResponse[];
  createdBy?: UserResponse;
  startDate?: string;
  endDate?: string;
  weeklyDays?: string[];
  createdAt: string;
}

export interface CreateTaskDefinitionRequest {
  title: string;
  description?: string;
  basePoints: number;
  recurrenceType: RecurrenceType;
  assignedUserIds: number[];
  startDate?: string;
  endDate?: string;
  weeklyDays?: string[];
}

// Task Instance
export interface TaskInstanceResponse {
  id: number;
  taskDefinition: TaskDefinitionResponse;
  assignedUser: ChildResponse;
  dueDate?: string;
  status: TaskStatus;
  completedAt?: string;
  approvedAt?: string;
  awardedPoints?: number;
  approvedBy?: UserResponse;
  parentNotes?: string;
  createdAt: string;
}

export interface ApproveTaskRequest {
  awardedPoints?: number;
  notes?: string;
}

// Reward
export interface RewardResponse {
  id: number;
  title: string;
  description?: string;
  pointsCost: number;
  imagePath?: string;
  active: boolean;
  createdAt: string;
}

export interface CreateRewardRequest {
  title: string;
  description?: string;
  pointsCost: number;
}

export interface RewardRedemptionResponse {
  id: number;
  user: ChildResponse;
  reward: RewardResponse;
  pointsSpent: number;
  status: RewardStatus;
  requestedAt: string;
  approvedAt?: string;
  approvedBy?: UserResponse;
  deliveredAt?: string;
  notes?: string;
}

// Behavior
export interface BehaviorResponse {
  id: number;
  title: string;
  guideline: string;
  plusPoints: number;
  minusPoints: number;
  user?: ChildResponse;
  rank: number;
  active: boolean;
  createdAt: string;
}

export interface BehaviorEvaluationResponse {
  id: number;
  behavior: BehaviorResponse;
  user: ChildResponse;
  currentPoints: number;
  remarks?: string;
  committed: boolean;
  createdBy?: UserResponse;
  createdAt: string;
  updatedAt?: string;
}

// Points
export interface PointBalanceResponse {
  userId: number;
  totalPoints: number;
}

export interface PointTransactionResponse {
  id: number;
  userId: number;
  points: number;
  type: PointTransactionType;
  description?: string;
  referenceId?: number;
  remarks?: string;
  createdBy?: UserResponse;
  createdAt: string;
  balanceAfter?: number;
}

export interface PointTransactionsResponse {
  transactions: PointTransactionResponse[];
  total: number;
  limit: number;
  offset: number;
}

// Notification
export interface NotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  referenceId?: number;
  createdAt: string;
  readAt?: string;
  read: boolean;
}

// Error
export interface ApiErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
