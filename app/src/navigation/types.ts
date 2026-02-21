import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, NavigatorScreenParams } from '@react-navigation/native';

// Root Stack (Auth vs Main App)
export type RootStackParamList = {
  Auth: undefined;
  Main: NavigatorScreenParams<MainTabParamList>;
  ServerSetup: undefined;
};

// Auth Stack
export type AuthStackParamList = {
  Login: undefined;
  ServerConfig: undefined;
};

// Main Tab Navigator
export type MainTabParamList = {
  Activities: NavigatorScreenParams<ActivitiesStackParamList>;
  Manage: NavigatorScreenParams<ManageStackParamList>;
  Notifications: undefined;
  Profile: NavigatorScreenParams<ProfileStackParamList>;
};

// Activities Stack (dynamic content)
export type ActivitiesStackParamList = {
  ActivitiesHome: undefined;
  TaskList: undefined;
  RewardList: undefined;
  ChildBehaviorList: undefined;
  PendingApproval: undefined;
  PendingRedemptions: undefined;
  BehaviorOverview: undefined;
  BehaviorEvaluate: { childId: number };
  ChildDetail: { childId: number };
};

// Manage Stack (master data / configuration)
export type ManageStackParamList = {
  ManageHome: undefined;
  TaskManagement: undefined;
  TaskEdit: { taskId?: number };
  RewardList: undefined;
  RewardCreate: { rewardId?: number } | undefined;
  RewardEdit: { rewardId?: number };
  BehaviorManage: undefined;
  AdminUserList: undefined;
};

// Profile Stack
export type ProfileStackParamList = {
  ProfileMain: undefined;
  ProfileEdit: undefined;
  PointsHistory: undefined;
  Settings: undefined;
  Notifications: undefined;
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

export type ActivitiesStackScreenProps<T extends keyof ActivitiesStackParamList> =
  CompositeScreenProps<
    NativeStackScreenProps<ActivitiesStackParamList, T>,
    MainTabScreenProps<keyof MainTabParamList>
  >;

// Declare global types for useNavigation hook
declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
