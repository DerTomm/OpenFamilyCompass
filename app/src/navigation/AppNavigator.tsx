import { MaterialCommunityIcons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import React, { useEffect } from 'react';
import { ActivityIndicator, Platform, StyleSheet, View } from 'react-native';
import { Text, useTheme } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useI18n } from '../i18n/I18nContext';
import { selectIsAdmin, selectIsChild, useAuthStore } from '../store/authStore';

// Screens
import { UserListScreen } from '../screens/admin/UserListScreen';
import { BehaviorListScreen } from '../screens/child/BehaviorListScreen';
import { BehaviorOverviewScreen } from '../screens/parent/BehaviorOverviewScreen';
import { BehaviorManageScreen } from '../screens/parent/BehaviorManageScreen';
import { BehaviorEvaluateScreen } from '../screens/parent/BehaviorEvaluateScreen';
import { ChildDetailScreen } from '../screens/parent/ChildDetailScreen';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { ChildDashboardScreen } from '../screens/child/DashboardScreen';
import { NotificationsScreen } from '../screens/notifications/NotificationsScreen';
import { ParentDashboardScreen } from '../screens/parent/DashboardScreen';
import { ProfileScreen } from '../screens/profile/ProfileScreen';
import { ServerSetupScreen } from '../screens/setup/ServerSetupScreen';
import { RewardListScreen } from '../screens/shop/RewardListScreen';
import { RewardEditScreen } from '../screens/shop/RewardEditScreen';
import { PendingRedemptionsScreen } from '../screens/shop/PendingRedemptionsScreen';
import { PendingApprovalScreen } from '../screens/tasks/PendingApprovalScreen';
import { TaskListScreen } from '../screens/tasks/TaskListScreen';
import { TaskManagementScreen } from '../screens/tasks/TaskManagementScreen';
import { TaskDefinitionEditScreen } from '../screens/tasks/TaskDefinitionEditScreen';
import { ManageHomeScreen } from '../screens/manage/ManageHomeScreen';

// Types
import {
  ActivitiesStackParamList,
  MainTabParamList,
  ManageStackParamList,
  ProfileStackParamList,
  RootStackParamList,
} from './types';

const RootStack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();
const ActivitiesStack = createNativeStackNavigator<ActivitiesStackParamList>();
const ManageStack = createNativeStackNavigator<ManageStackParamList>();
const ProfileStack = createNativeStackNavigator<ProfileStackParamList>();
const NotificationsStack = createNativeStackNavigator();

const createStackScreenOptions = (theme: any) => ({
  headerStyle: { backgroundColor: theme.colors.surface },
  headerTintColor: theme.colors.onSurface,
  headerTitleStyle: { color: theme.colors.onSurface },
});

// Activities Stack (dynamic content)
const ActivitiesStackNavigator: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  const isChild = useAuthStore(selectIsChild);

  const HomeComponent = isChild ? ChildDashboardScreen : ParentDashboardScreen;

  return (
    <ActivitiesStack.Navigator screenOptions={createStackScreenOptions(theme)}>
      <ActivitiesStack.Screen
        name="ActivitiesHome"
        component={HomeComponent}
        options={{ title: t('nav.activities') }}
      />

      {/* Child dynamic content */}
      {isChild && (
        <>
          <ActivitiesStack.Screen
            name="TaskList"
            component={TaskListScreen}
            options={{ title: t('tasks.my.title') }}
          />
          <ActivitiesStack.Screen
            name="RewardList"
            component={RewardListScreen}
            options={{ title: t('shop.title.page') }}
          />
          <ActivitiesStack.Screen
            name="ChildBehaviorList"
            component={BehaviorListScreen}
            options={{ title: t('child.behaviors.title') }}
          />
        </>
      )}

      {/* Parent dynamic content */}
      {!isChild && (
        <>
          <ActivitiesStack.Screen
            name="PendingApproval"
            component={PendingApprovalScreen}
            options={{ title: t('tasks.pending.title') }}
          />
          <ActivitiesStack.Screen
            name="PendingRedemptions"
            component={PendingRedemptionsScreen}
            options={{ title: t('rewards.pending.title') }}
          />
          <ActivitiesStack.Screen
            name="BehaviorOverview"
            component={BehaviorOverviewScreen}
            options={{ title: t('behavior.evaluate') }}
          />
          <ActivitiesStack.Screen
            name="BehaviorEvaluate"
            component={BehaviorEvaluateScreen}
            options={{ title: t('behavior.evaluate') }}
          />
          <ActivitiesStack.Screen
            name="ChildDetail"
            component={ChildDetailScreen}
            options={{ title: t('child.detail') }}
          />
        </>
      )}
    </ActivitiesStack.Navigator>
  );
};

// Manage Stack (master data / configuration)
const ManageStackNavigator: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  const isAdmin = useAuthStore(selectIsAdmin);

  return (
    <ManageStack.Navigator screenOptions={createStackScreenOptions(theme)}>
      <ManageStack.Screen
        name="ManageHome"
        component={ManageHomeScreen}
        options={{ title: t('nav.manage') }}
      />
      <ManageStack.Screen
        name="TaskManagement"
        component={TaskManagementScreen}
        options={{ title: t('tasks.title') }}
      />
      <ManageStack.Screen
        name="TaskEdit"
        component={TaskDefinitionEditScreen}
        options={{ title: t('task.create.title') }}
      />
      <ManageStack.Screen
        name="RewardList"
        component={RewardListScreen}
        options={{ title: t('rewards.title') }}
      />
      <ManageStack.Screen
        name="RewardCreate"
        component={RewardEditScreen}
        options={{ title: t('reward.create.title') }}
      />
      <ManageStack.Screen
        name="RewardEdit"
        component={RewardEditScreen}
        options={{ title: t('reward.create.title') }}
      />
      <ManageStack.Screen
        name="BehaviorManage"
        component={BehaviorManageScreen}
        options={{ title: t('behavior.manage.title') }}
      />
      {isAdmin && (
        <ManageStack.Screen
          name="AdminUserList"
          component={UserListScreen}
          options={{ title: t('admin.user.management') }}
        />
      )}
    </ManageStack.Navigator>
  );
};

// Profile Stack
const ProfileStackNavigator: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  return (
    <ProfileStack.Navigator screenOptions={createStackScreenOptions(theme)}>
      <ProfileStack.Screen
        name="ProfileMain"
        component={ProfileScreen}
        options={{ title: t('nav.profile') }}
      />
    </ProfileStack.Navigator>
  );
};

const NotificationsStackNavigator: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();

  return (
    <NotificationsStack.Navigator screenOptions={createStackScreenOptions(theme)}>
      <NotificationsStack.Screen
        name="NotificationsHome"
        component={NotificationsScreen}
        options={{ title: t('nav.notifications') }}
      />
    </NotificationsStack.Navigator>
  );
};

const VersionFooter: React.FC = () => {
  const theme = useTheme();

  return (
    <View style={[styles.footer, { backgroundColor: theme.colors.surfaceVariant, borderTopColor: theme.colors.outlineVariant }]}>
      <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
        Version 1.0.0
      </Text>
    </View>
  );
};

// Auth Stack
const AuthNavigator: React.FC = () => {
  return (
    <RootStack.Navigator screenOptions={{ headerShown: false }}>
      <RootStack.Screen name="Auth" component={LoginScreen} />
    </RootStack.Navigator>
  );
};

const MainTabsNavigator: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  const isChild = useAuthStore(selectIsChild);
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.mainTabsContainer}>
      <Tab.Navigator
        screenOptions={({ route }) => ({
          headerShown: false,
          tabBarActiveTintColor: theme.colors.primary,
          tabBarInactiveTintColor: theme.colors.onSurfaceVariant,
          tabBarStyle: {
            backgroundColor: theme.colors.surface,
            borderTopColor: theme.colors.surfaceVariant,
            paddingBottom: insets.bottom > 0 ? insets.bottom : 8,
            height: (insets.bottom > 0 ? insets.bottom : 8) + 56,
          },
          tabBarLabelStyle: {
            fontSize: 12,
            fontWeight: '600',
          },
          tabBarIcon: ({ color, size }) => {
            const iconName =
              route.name === 'Activities'
                ? 'home'
                : route.name === 'Manage'
                  ? 'cog'
                  : route.name === 'Notifications'
                    ? 'bell'
                    : 'account';
            return <MaterialCommunityIcons name={iconName as any} size={size} color={color} />;
          },
        })}
      >
          <Tab.Screen
            name="Activities"
            component={ActivitiesStackNavigator}
            options={{ title: t('nav.activities') }}
          />

          {!isChild && (
            <Tab.Screen
              name="Manage"
              component={ManageStackNavigator}
              options={{ title: t('nav.manage') }}
            />
          )}

          <Tab.Screen
            name="Notifications"
            component={NotificationsStackNavigator}
            options={{ title: t('nav.notifications') }}
          />

          <Tab.Screen
            name="Profile"
            component={ProfileStackNavigator}
            options={{ title: t('nav.profile') }}
          />
        </Tab.Navigator>
    </View>
  );
};

// Root Navigator
export const AppNavigator: React.FC = () => {
  const { isAuthenticated, isLoading, isSetupComplete, initialize, completeSetup } = useAuthStore();
  const theme = useTheme();

  useEffect(() => {
    initialize();
  }, [initialize]);

  if (isLoading) {
    return (
      <View style={[styles.loading, { backgroundColor: theme.colors.background }]}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  // Show setup screen if server URL not configured
  if (!isSetupComplete) {
    return (
      <NavigationContainer>
        <RootStack.Navigator screenOptions={{ headerShown: false }}>
          <RootStack.Screen name="ServerSetup">
            {() => <ServerSetupScreen onComplete={completeSetup} />}
          </RootStack.Screen>
        </RootStack.Navigator>
      </NavigationContainer>
    );
  }

  return (
    <NavigationContainer>
      {isAuthenticated ? <MainTabsNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  mainTabsContainer: {
    flex: 1,
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
  footer: {
    borderTopWidth: 1,
    paddingHorizontal: 16,
    paddingVertical: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
});
