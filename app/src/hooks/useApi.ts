import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  taskInstancesApi,
  taskDefinitionsApi,
  rewardsApi,
  redemptionsApi,
  usersApi,
  pointsApi,
  notificationsApi,
  profileApi,
} from '../api/services';
import { useAuthStore } from '../store/authStore';

// Query Keys
export const queryKeys = {
  taskInstances: (params?: object) => ['taskInstances', params],
  taskDefinitions: (params?: object) => ['taskDefinitions', params],
  pendingTasks: ['pendingTasks'],
  rewards: (params?: object) => ['rewards', params],
  redemptions: (params?: object) => ['redemptions', params],
  pendingRedemptions: ['pendingRedemptions'],
  children: ['children'],
  users: (params?: object) => ['users', params],
  points: (userId?: number) => ['points', userId],
  transactions: (params?: object) => ['transactions', params],
  notifications: (params?: object) => ['notifications', params],
  unreadCount: ['unreadCount'],
  profile: ['profile'],
};

// Task Hooks
export const useTaskInstances = (params?: { assignedUserId?: number; status?: string }) => {
  return useQuery({
    queryKey: queryKeys.taskInstances(params),
    queryFn: () => taskInstancesApi.list(params),
  });
};

export const usePendingTasks = () => {
  return useQuery({
    queryKey: queryKeys.pendingTasks,
    queryFn: () => taskInstancesApi.getPending(),
  });
};

export const useCompleteTask = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (taskId: number) => taskInstancesApi.complete(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['taskInstances'] });
    },
  });
};

export const useApproveTask = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, data }: { taskId: number; data?: { awardedPoints?: number; notes?: string } }) =>
      taskInstancesApi.approve(taskId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['taskInstances'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.pendingTasks });
    },
  });
};

export const useRejectTask = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, notes }: { taskId: number; notes?: string }) =>
      taskInstancesApi.reject(taskId, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['taskInstances'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.pendingTasks });
    },
  });
};

// Reward Hooks
export const useRewards = (active?: boolean) => {
  return useQuery({
    queryKey: queryKeys.rewards({ active }),
    queryFn: () => rewardsApi.list(active),
  });
};

export const useRedemptions = (params?: { userId?: number; status?: string }) => {
  return useQuery({
    queryKey: queryKeys.redemptions(params),
    queryFn: () => redemptionsApi.list(params),
  });
};

export const usePendingRedemptions = () => {
  return useQuery({
    queryKey: queryKeys.pendingRedemptions,
    queryFn: () => redemptionsApi.getPending(),
  });
};

export const useRequestRedemption = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (rewardId: number) => redemptionsApi.request(rewardId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['redemptions'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.points() });
    },
  });
};

// Children Hook (for parents)
export const useChildren = () => {
  return useQuery({
    queryKey: queryKeys.children,
    queryFn: () => usersApi.listChildren(),
  });
};

// Points Hooks
export const usePointBalance = (userId?: number) => {
  return useQuery({
    queryKey: queryKeys.points(userId),
    queryFn: () => userId ? pointsApi.getBalanceForUser(userId) : pointsApi.getBalance(),
  });
};

export const usePointTransactions = (params?: { userId?: number; limit?: number }) => {
  return useQuery({
    queryKey: queryKeys.transactions(params),
    queryFn: () => pointsApi.getTransactions(params),
  });
};

// Notifications Hooks
export const useNotifications = (params?: { unreadOnly?: boolean; limit?: number }) => {
  return useQuery({
    queryKey: queryKeys.notifications(params),
    queryFn: () => notificationsApi.list(params),
  });
};

export const useUnreadCount = () => {
  return useQuery({
    queryKey: queryKeys.unreadCount,
    queryFn: () => notificationsApi.getUnreadCount(),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};

export const useMarkAsRead = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: number) => notificationsApi.markAsRead(notificationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.unreadCount });
    },
  });
};
