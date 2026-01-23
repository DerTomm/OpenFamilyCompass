import React, { useEffect } from 'react';
import { ActivityIndicator, View, StyleSheet, Text } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useAuthStore, selectIsChild, selectIsParent, selectIsAdmin } from '../store/authStore';

// Screens
import { LoginScreen } from '../screens/auth/LoginScreen';
import { ChildDashboardScreen } from '../screens/child/DashboardScreen';
import { ParentDashboardScreen } from '../screens/parent/DashboardScreen';
import { TaskListScreen } from '../screens/tasks/TaskListScreen';
import { PendingApprovalScreen } from '../screens/tasks/PendingApprovalScreen';
import { RewardListScreen } from '../screens/shop/RewardListScreen';
import { PendingRedemptionsScreen } from '../screens/shop/PendingRedemptionsScreen';
import { ProfileScreen } from '../screens/profile/ProfileScreen';
import { NotificationsScreen } from '../screens/notifications/NotificationsScreen';
import { UserListScreen } from '../screens/admin/UserListScreen';

// Types
import { RootStackParamList, MainTabParamList } from './types';

const RootStack = createNativeStackNavigator<RootStackParamList>();
const MainTab = createBottomTabNavigator<MainTabParamList>();
const TasksStack = createNativeStackNavigator();
const ShopStack = createNativeStackNavigator();
const ProfileStack = createNativeStackNavigator();
const AdminStack = createNativeStackNavigator();

// Tasks Stack Navigator
const TasksStackNavigator: React.FC = () => {
  const isChild = useAuthStore(selectIsChild);
  
  return (
    <TasksStack.Navigator>
      <TasksStack.Screen name="TaskList" component={TaskListScreen} options={{ title: 'My Tasks' }} />
      {!isChild && (
        <TasksStack.Screen 
          name="PendingApproval" 
          component={PendingApprovalScreen} 
          options={{ title: 'Pending Approval' }} 
        />
      )}
    </TasksStack.Navigator>
  );
};

// Shop Stack Navigator
const ShopStackNavigator: React.FC = () => {
  const isChild = useAuthStore(selectIsChild);
  
  return (
    <ShopStack.Navigator>
      <ShopStack.Screen name="RewardList" component={RewardListScreen} options={{ title: 'Reward Shop' }} />
      {!isChild && (
        <ShopStack.Screen 
          name="PendingRedemptions" 
          component={PendingRedemptionsScreen} 
          options={{ title: 'Pending Redemptions' }} 
        />
      )}
    </ShopStack.Navigator>
  );
};

// Profile Stack Navigator
const ProfileStackNavigator: React.FC = () => {
  return (
    <ProfileStack.Navigator>
      <ProfileStack.Screen name="ProfileMain" component={ProfileScreen} options={{ headerShown: false }} />
      <ProfileStack.Screen name="Notifications" component={NotificationsScreen} options={{ title: 'Notifications' }} />
    </ProfileStack.Navigator>
  );
};

// Admin Stack Navigator
const AdminStackNavigator: React.FC = () => {
  return (
    <AdminStack.Navigator>
      <AdminStack.Screen name="UserList" component={UserListScreen} options={{ title: 'Users' }} />
    </AdminStack.Navigator>
  );
};

// Main Tab Navigator
const MainTabNavigator: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const isChild = useAuthStore(selectIsChild);
  const isParent = useAuthStore(selectIsParent);
  const isAdmin = useAuthStore(selectIsAdmin);

  // Choose dashboard based on role
  const DashboardComponent = isChild ? ChildDashboardScreen : ParentDashboardScreen;

  return (
    <MainTab.Navigator
      screenOptions={{
        tabBarActiveTintColor: '#2196F3',
        tabBarInactiveTintColor: '#999',
        headerShown: true,
      }}
    >
      <MainTab.Screen
        name="Dashboard"
        component={DashboardComponent}
        options={{
          title: 'Home',
          tabBarIcon: ({ color }) => <TabIcon name="home" color={color} />,
        }}
      />
      <MainTab.Screen
        name="Tasks"
        component={TasksStackNavigator}
        options={{
          title: 'Tasks',
          headerShown: false,
          tabBarIcon: ({ color }) => <TabIcon name="tasks" color={color} />,
        }}
      />
      <MainTab.Screen
        name="Shop"
        component={ShopStackNavigator}
        options={{
          title: 'Shop',
          headerShown: false,
          tabBarIcon: ({ color }) => <TabIcon name="shop" color={color} />,
        }}
      />
      <MainTab.Screen
        name="Profile"
        component={ProfileStackNavigator}
        options={{
          title: 'Profile',
          headerShown: false,
          tabBarIcon: ({ color }) => <TabIcon name="profile" color={color} />,
        }}
      />
      {isAdmin && (
        <MainTab.Screen
          name="Admin"
          component={AdminStackNavigator}
          options={{
            title: 'Admin',
            headerShown: false,
            tabBarIcon: ({ color }) => <TabIcon name="admin" color={color} />,
          }}
        />
      )}
    </MainTab.Navigator>
  );
};

// Simple text-based tab icon (replace with proper icons later)
const TabIcon: React.FC<{ name: string; color: string }> = ({ name, color }) => {
  const icons: Record<string, string> = {
    home: '🏠',
    tasks: '✅',
    shop: '🎁',
    profile: '👤',
    admin: '⚙️',
  };
  return (
    <View style={styles.tabIcon}>
      <Text style={{ fontSize: 20, opacity: color === '#999' ? 0.5 : 1 }}>
        {icons[name] || '•'}
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

// Root Navigator
export const AppNavigator: React.FC = () => {
  const { isAuthenticated, isLoading, initialize } = useAuthStore();

  useEffect(() => {
    initialize();
  }, [initialize]);

  if (isLoading) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator size="large" color="#2196F3" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      {isAuthenticated ? <MainTabNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  tabIcon: {
    width: 24,
    height: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
