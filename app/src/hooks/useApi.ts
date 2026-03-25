import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  notificationsApi,
  pointsApi,
  redemptionsApi,
  rewardsApi,
  taskDefinitionsApi,
  taskInstancesApi,
  usersApi
} from '../api/services';
import { selectIsParent, useAuthStore } from '../store/authStore';

// Query Keys
export const queryKeys = {
  taskInstances: (params?: object) => ['taskInstances', params],
  taskDefinitions: (params?: object) => ['taskDefinitions', params],
  taskDefinition: (id: number) => ['taskDefinitions', id],
  pendingTasks: ['pendingTasks'],
  rewards: (params?: object) => ['rewards', params],
  reward: (id: number) => ['rewards', id],
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

import {
  CreateRewardRequest,
  CreateTaskDefinitionRequest
} from '../types/api';

// Task Hooks
export const useTaskDefinitions = (assignedUserId?: number) => {
  return useQuery({
    queryKey: queryKeys.taskDefinitions({ assignedUserId }),
    queryFn: () => taskDefinitionsApi.list(assignedUserId),
  });
};

export const useTaskDefinition = (id: number) => {
  return useQuery({
    queryKey: queryKeys.taskDefinition(id),
    queryFn: () => taskDefinitionsApi.getById(id),
    enabled: !!id,
  });
};

export const useCreateTaskDefinition = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTaskDefinitionRequest) => taskDefinitionsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['taskDefinitions'] });
      queryClient.invalidateQueries({ queryKey: ['taskInstances'] });
    },
  });
};

export const useUpdateTaskDefinition = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CreateTaskDefinitionRequest> }) =>
      taskDefinitionsApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['taskDefinitions'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.taskDefinition(id) });
    },
  });
};

export const useDeleteTaskDefinition = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => taskDefinitionsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['taskDefinitions'] });
    },
  });
};

export const useTaskInstances = (params?: { assignedUserId?: number; status?: string }) => {
  return useQuery({
    queryKey: queryKeys.taskInstances(params),
    queryFn: () => taskInstancesApi.list(params),
    staleTime: 0,           // Immer als veraltet betrachten – neue Aufgaben erscheinen sofort beim nächsten Mount
    refetchInterval: 30000, // Alle 30 s automatisch neu laden (wichtig wenn Eltern auf anderem Gerät eine Aufgabe erstellen)
  });
};

export const usePendingTasks = () => {
  return useQuery({
    queryKey: queryKeys.pendingTasks,
    queryFn: () => taskInstancesApi.getPending(),
    refetchInterval: 15000,
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
      queryClient.invalidateQueries({ queryKey: ['points'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.children });
      queryClient.invalidateQueries({ queryKey: queryKeys.profile });
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

export const useReward = (id: number) => {
  return useQuery({
    queryKey: queryKeys.reward(id),
    queryFn: () => rewardsApi.getById(id),
    enabled: !!id,
  });
};

export const useCreateReward = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateRewardRequest) => rewardsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rewards'] });
    },
  });
};

export const useUpdateReward = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CreateRewardRequest & { active: boolean }> }) =>
      rewardsApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['rewards'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.reward(id) });
    },
  });
};

export const useDeactivateReward = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => rewardsApi.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rewards'] });
    },
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
    refetchInterval: 15000,
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
  const isParent = useAuthStore(selectIsParent);

  return useQuery({
    queryKey: queryKeys.children,
    queryFn: () => usersApi.listChildren(),
    enabled: isParent,
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
