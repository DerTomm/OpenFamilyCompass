import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, NavigatorScreenParams } from '@react-navigation/native';

// Root Stack (Auth vs Main App)
export type RootStackParamList = {
  Auth: undefined;
  Main: NavigatorScreenParams<MainTabParamList>;
};

// Auth Stack
export type AuthStackParamList = {
  Login: undefined;
  ServerConfig: undefined;
};

// Main Tab Navigator
export type MainTabParamList = {
  Dashboard: undefined;
  Tasks: NavigatorScreenParams<TasksStackParamList>;
  Shop: NavigatorScreenParams<ShopStackParamList>;
  Profile: NavigatorScreenParams<ProfileStackParamList>;
  // Admin only
  Admin: NavigatorScreenParams<AdminStackParamList>;
};

// Tasks Stack
export type TasksStackParamList = {
  TaskList: undefined;
  TaskDetail: { taskId: number };
  TaskCreate: undefined;
  TaskEdit: { taskId: number };
  PendingApproval: undefined;
};

// Shop Stack
export type ShopStackParamList = {
  RewardList: undefined;
  RewardDetail: { rewardId: number };
  RewardCreate: undefined;
  RewardEdit: { rewardId: number };
  RedemptionHistory: undefined;
  PendingRedemptions: undefined;
};

// Profile Stack
export type ProfileStackParamList = {
  ProfileMain: undefined;
  ProfileEdit: undefined;
  PointsHistory: undefined;
  Settings: undefined;
  Notifications: undefined;
};

// Admin Stack
export type AdminStackParamList = {
  UserList: undefined;
  UserCreate: undefined;
  UserEdit: { userId: number };
  BehaviorList: undefined;
  BehaviorCreate: undefined;
  BehaviorEdit: { behaviorId: number };
  BehaviorEvaluations: { userId: number };
};

// Screen Props Types
export type RootStackScreenProps<T extends keyof RootStackParamList> = 
  NativeStackScreenProps<RootStackParamList, T>;

export type AuthStackScreenProps<T extends keyof AuthStackParamList> = 
  NativeStackScreenProps<AuthStackParamList, T>;

export type MainTabScreenProps<T extends keyof MainTabParamList> = 
  CompositeScreenProps<
    BottomTabScreenProps<MainTabParamList, T>,
    RootStackScreenProps<keyof RootStackParamList>
  >;

export type TasksStackScreenProps<T extends keyof TasksStackParamList> = 
  CompositeScreenProps<
    NativeStackScreenProps<TasksStackParamList, T>,
    MainTabScreenProps<keyof MainTabParamList>
  >;

// Declare global types for useNavigation hook
declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
