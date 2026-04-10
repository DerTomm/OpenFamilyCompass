import {
  ApproveTaskRequest,
  BehaviorEvaluationResponse,
  BehaviorResponse,
  ChangePasswordRequest,
  ChildResponse,
  CreateRewardRequest,
  CreateTaskDefinitionRequest,
  NotificationResponse,
  PointBalanceResponse,
  PointTransactionResponse,
  PointTransactionsResponse,
  RewardRedemptionResponse,
  RewardResponse,
  TaskDefinitionResponse,
  TaskInstanceResponse,
  UpdateProfileRequest,
  UsernameCheckResponse,
  UserProfileResponse,
  UserResponse,
} from '../types/api';
import { api } from './client';

// Auth & Profile
export const authApi = {
  getMe: () => api.get<UserResponse>('/auth/me'),
  changePassword: (data: ChangePasswordRequest) => api.post<void>('/auth/change-password', data),
};

export const profileApi = {
  get: () => api.get<UserProfileResponse>('/profile'),
  update: (data: UpdateProfileRequest) => api.put<UserProfileResponse>('/profile', data),
  changePassword: (data: ChangePasswordRequest) => api.post<void>('/profile/password', data),
  checkUsername: (username: string) => api.get<UsernameCheckResponse>('/profile/username/check', { username }),
  getAvatarIcons: () => api.get<string[]>('/profile/avatar/icons'),
};

// Users (Admin)
export const usersApi = {
  list: (role?: string, active?: boolean) => {
    const params: Record<string, any> = {};
    if (role) params.role = role;
    if (active !== undefined) params.active = active;
    return api.get<UserResponse[]>('/users', params);
  },
  getById: (id: number) => api.get<UserResponse>(`/users/${id}`),
  create: (data: { username: string; password: string; firstName: string; role: string }) =>
    api.post<UserResponse>('/users', data),
  update: (id: number, data: Partial<{ firstName: string; password: string; active: boolean; role: string }>) =>
    api.put<UserResponse>(`/users/${id}`, data),
  deactivate: (id: number) => api.delete<void>(`/users/${id}`),
  deletePermanent: (id: number) => api.delete<void>(`/users/${id}/permanent`),
  listChildren: () => api.get<ChildResponse[]>('/users/children'),
};

// Task Definitions
export const taskDefinitionsApi = {
  list: (assignedUserId?: number) =>
    api.get<TaskDefinitionResponse[]>('/tasks/definitions', { assignedUserId }),
  getById: (id: number) => api.get<TaskDefinitionResponse>(`/tasks/definitions/${id}`),
  create: (data: CreateTaskDefinitionRequest) =>
    api.post<TaskDefinitionResponse>('/tasks/definitions', data),
  update: (id: number, data: Partial<CreateTaskDefinitionRequest>) =>
    api.put<TaskDefinitionResponse>(`/tasks/definitions/${id}`, data),
  delete: (id: number) => api.delete<void>(`/tasks/definitions/${id}`),
};

// Task Instances
export const taskInstancesApi = {
  list: (params?: { assignedUserId?: number; status?: string; deadlineFrom?: string; deadlineTo?: string }) =>
    api.get<TaskInstanceResponse[]>('/tasks/instances', params),
  getById: (id: number) => api.get<TaskInstanceResponse>(`/tasks/instances/${id}`),
  complete: (id: number) => api.post<TaskInstanceResponse>(`/tasks/instances/${id}/complete`),
  approve: (id: number, data?: ApproveTaskRequest) =>
    api.post<TaskInstanceResponse>(`/tasks/instances/${id}/approve`, data),
  reject: (id: number, notes?: string) =>
    api.post<TaskInstanceResponse>(`/tasks/instances/${id}/reject`, { notes }),
  getPending: () => api.get<TaskInstanceResponse[]>('/tasks/pending'),
};

// Rewards
export const rewardsApi = {
  list: (active?: boolean) => api.get<RewardResponse[]>('/rewards', { active }),
  getById: (id: number) => api.get<RewardResponse>(`/rewards/${id}`),
  create: (data: CreateRewardRequest) => api.post<RewardResponse>('/rewards', data),
  update: (id: number, data: Partial<CreateRewardRequest & { active: boolean }>) =>
    api.put<RewardResponse>(`/rewards/${id}`, data),
  deactivate: (id: number) => api.delete<void>(`/rewards/${id}`),
  uploadImage: (id: number, uri: string, mimeType?: string) => {
    const formData = new FormData();
    const ext = uri.split('.').pop()?.toLowerCase() ?? 'jpg';
    const type = mimeType ?? (ext === 'png' ? 'image/png' : 'image/jpeg');
    formData.append('file', { uri, name: `reward-image.${ext}`, type } as any);
    return api.upload<RewardResponse>(`/rewards/${id}/image`, formData);
  },
};

// Reward Redemptions
export const redemptionsApi = {
  list: (params?: { userId?: number; status?: string }) =>
    api.get<RewardRedemptionResponse[]>('/rewards/redemptions', params),
  request: (rewardId: number) =>
    api.post<RewardRedemptionResponse>('/rewards/redemptions', { rewardId }),
  approve: (id: number) => api.post<RewardRedemptionResponse>(`/rewards/redemptions/${id}/approve`),
  reject: (id: number, notes?: string) =>
    api.post<RewardRedemptionResponse>(`/rewards/redemptions/${id}/reject`, { notes }),
  cancel: (id: number) => api.post<RewardRedemptionResponse>(`/rewards/redemptions/${id}/cancel`),
  deliver: (id: number) => api.post<RewardRedemptionResponse>(`/rewards/redemptions/${id}/deliver`),
  getPending: () => api.get<RewardRedemptionResponse[]>('/rewards/redemptions/pending'),
};

// Behaviors
export const behaviorsApi = {
  list: (params?: { userId?: number; active?: boolean }) =>
    api.get<BehaviorResponse[]>('/behaviors', params),
  getById: (id: number) => api.get<BehaviorResponse>(`/behaviors/${id}`),
  create: (data: { title: string; guideline: string; plusPoints: number; minusPoints: number; userId?: number; rank?: number }) =>
    api.post<BehaviorResponse>('/behaviors', data),
  update: (id: number, data: Partial<{ title: string; guideline: string; plusPoints: number; minusPoints: number; rank: number; active: boolean }>) =>
    api.put<BehaviorResponse>(`/behaviors/${id}`, data),
  deactivate: (id: number) => api.delete<void>(`/behaviors/${id}`),
};

// Behavior Evaluations
export const evaluationsApi = {
  list: (params?: { userId?: number; committed?: boolean }) =>
    api.get<BehaviorEvaluationResponse[]>('/behaviors/evaluations', params),
  save: (data: { behaviorId: number; userId: number; currentPoints: number; remarks?: string }) =>
    api.post<BehaviorEvaluationResponse>('/behaviors/evaluations', data),
  commit: (userId: number) =>
    api.post<{ totalPointsAwarded: number; evaluationsCommitted: number }>('/behaviors/evaluations/commit', { userId }),
};

// Points
export const pointsApi = {
  getBalance: () => api.get<PointBalanceResponse>('/points/balance'),
  getBalanceForUser: (userId: number) => api.get<PointBalanceResponse>(`/points/balance/${userId}`),
  getTransactions: (params?: { userId?: number; type?: string; from?: string; to?: string; limit?: number; offset?: number }) =>
    api.get<PointTransactionsResponse>('/points/transactions', params),
  awardBonus: (userId: number, points: number, description: string) =>
    api.post('/points/bonus', { userId, points, description }),
  deductPenalty: (userId: number, points: number, description: string) =>
    api.post('/points/penalty', { userId, points, description }),
  addPoints: (userId: number, points: number, type: 'BONUS' | 'PENALTY', description: string) =>
    api.post<PointTransactionResponse>('/points/add', { userId, points, type, description }),
};

// Notifications
export const notificationsApi = {
  list: (params?: { unreadOnly?: boolean; limit?: number }) =>
    api.get<NotificationResponse[]>('/notifications', params),
  markAsRead: (id: number) => api.post<void>(`/notifications/${id}/read`),
  markAllAsRead: () => api.post<void>('/notifications/read-all'),
  getUnreadCount: () => api.get<{ count: number }>('/notifications/unread-count'),
  registerDevice: (token: string, platform: 'android' | 'ios' | 'web', deviceId: string) =>
    api.post<void>('/notifications/register-device', { token, platform, deviceId }),
  unregisterDevice: (token: string) =>
    api.post<void>('/notifications/unregister-device', { token }),
};
