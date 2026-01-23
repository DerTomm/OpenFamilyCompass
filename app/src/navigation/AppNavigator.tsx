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

// Types
import { RootStackParamList, MainTabParamList } from './types';

const RootStack = createNativeStackNavigator<RootStackParamList>();
const MainTab = createBottomTabNavigator<MainTabParamList>();

// Placeholder screens
const PlaceholderScreen: React.FC<{ title: string }> = ({ title }) => (
  <View style={styles.placeholder}>
    <ActivityIndicator size="large" />
  </View>
);

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
        component={() => <PlaceholderScreen title="Tasks" />}
        options={{
          title: 'Tasks',
          tabBarIcon: ({ color }) => <TabIcon name="tasks" color={color} />,
        }}
      />
      <MainTab.Screen
        name="Shop"
        component={() => <PlaceholderScreen title="Shop" />}
        options={{
          title: 'Shop',
          tabBarIcon: ({ color }) => <TabIcon name="shop" color={color} />,
        }}
      />
      <MainTab.Screen
        name="Profile"
        component={() => <PlaceholderScreen title="Profile" />}
        options={{
          title: 'Profile',
          tabBarIcon: ({ color }) => <TabIcon name="profile" color={color} />,
        }}
      />
      {isAdmin && (
        <MainTab.Screen
          name="Admin"
          component={() => <PlaceholderScreen title="Admin" />}
          options={{
            title: 'Admin',
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
  placeholder: {
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
