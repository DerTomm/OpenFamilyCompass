import { MaterialCommunityIcons } from '@expo/vector-icons';
import { NavigationContainer, useNavigation } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Dimensions, StyleSheet, TouchableOpacity, View } from 'react-native';
import { Appbar, Avatar, Divider, Drawer, Menu, Modal, Portal, Text, useTheme } from 'react-native-paper';
import { useI18n } from '../i18n/I18nContext';
import { selectIsAdmin, selectIsChild, useAuthStore } from '../store/authStore';

// Screens
import { UserListScreen } from '../screens/admin/UserListScreen';
import { BehaviorListScreen } from '../screens/child/BehaviorListScreen';
import { BehaviorManageScreen } from '../screens/parent/BehaviorManageScreen';
import { BehaviorEvaluateScreen } from '../screens/parent/BehaviorEvaluateScreen';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { ChildDashboardScreen } from '../screens/child/DashboardScreen';
import { NotificationsScreen } from '../screens/notifications/NotificationsScreen';
import { ParentDashboardScreen } from '../screens/parent/DashboardScreen';
import { ProfileScreen } from '../screens/profile/ProfileScreen';
import { ServerSetupScreen } from '../screens/setup/ServerSetupScreen';
import { PendingRedemptionsScreen } from '../screens/shop/PendingRedemptionsScreen';
import { RewardListScreen } from '../screens/shop/RewardListScreen';
import { PendingApprovalScreen } from '../screens/tasks/PendingApprovalScreen';
import { TaskListScreen } from '../screens/tasks/TaskListScreen';

// Types
import { RootStackParamList } from './types';

const RootStack = createNativeStackNavigator<RootStackParamList>();
const MainStack = createNativeStackNavigator();
const TasksStack = createNativeStackNavigator();
const ShopStack = createNativeStackNavigator();
const ProfileStack = createNativeStackNavigator();
const AdminStack = createNativeStackNavigator();

// Tasks Stack Navigator
const TasksStackNavigator: React.FC = () => {
  const isChild = useAuthStore(selectIsChild);
  const theme = useTheme();

  return (
    <TasksStack.Navigator
      screenOptions={{
        headerStyle: {
          backgroundColor: theme.colors.surface,
        },
        headerTintColor: theme.colors.onSurface,
      }}
    >
      <TasksStack.Screen
        name="TaskList"
        component={TaskListScreen}
        options={{
          title: 'Meine Aufgaben',
          headerLeft: () => null,
        }}
      />
      {!isChild && (
        <TasksStack.Screen
          name="PendingApproval"
          component={PendingApprovalScreen}
          options={{ title: 'Freigabe ausstehend' }}
        />
      )}
    </TasksStack.Navigator>
  );
};

// Shop Stack Navigator
const ShopStackNavigator: React.FC = () => {
  const isChild = useAuthStore(selectIsChild);
  const theme = useTheme();

  return (
    <ShopStack.Navigator
      screenOptions={{
        headerStyle: {
          backgroundColor: theme.colors.surface,
        },
        headerTintColor: theme.colors.onSurface,
      }}
    >
      <ShopStack.Screen
        name="RewardList"
        component={RewardListScreen}
        options={{
          title: 'Belohnungs-Shop',
          headerLeft: () => null,
        }}
      />
      {!isChild && (
        <ShopStack.Screen
          name="PendingRedemptions"
          component={PendingRedemptionsScreen}
          options={{ title: 'Einlösungen ausstehend' }}
        />
      )}
    </ShopStack.Navigator>
  );
};

// Profile Stack Navigator
const ProfileStackNavigator: React.FC = () => {
  const theme = useTheme();

  return (
    <ProfileStack.Navigator
      screenOptions={{
        headerStyle: {
          backgroundColor: theme.colors.surface,
        },
        headerTintColor: theme.colors.onSurface,
      }}
    >
      <ProfileStack.Screen
        name="ProfileMain"
        component={ProfileScreen}
        options={{
          headerShown: false,
        }}
      />
      <ProfileStack.Screen
        name="Notifications"
        component={NotificationsScreen}
        options={{ title: 'Benachrichtigungen' }}
      />
    </ProfileStack.Navigator>
  );
};

// Admin Stack Navigator
const AdminStackNavigator: React.FC = () => {
  const theme = useTheme();

  return (
    <AdminStack.Navigator
      screenOptions={{
        headerStyle: {
          backgroundColor: theme.colors.surface,
        },
        headerTintColor: theme.colors.onSurface,
      }}
    >
      <AdminStack.Screen
        name="UserList"
        component={UserListScreen}
        options={{
          title: 'Benutzer',
          headerLeft: () => null,
        }}
      />
    </AdminStack.Navigator>
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

// Custom Header with Navigation
interface CustomHeaderProps {
  currentRoute: string;
  onNavigate: (route: string) => void;
}

// Check if we're on a small screen (mobile)
const useIsSmallScreen = () => {
  const [isSmall, setIsSmall] = useState(Dimensions.get('window').width < 768);

  useEffect(() => {
    const subscription = Dimensions.addEventListener('change', ({ window }) => {
      setIsSmall(window.width < 768);
    });
    return () => subscription?.remove();
  }, []);

  return isSmall;
};

const CustomHeader: React.FC<CustomHeaderProps> = ({ currentRoute, onNavigate }) => {
  const theme = useTheme();
  const { t } = useI18n();
  const isChild = useAuthStore(selectIsChild);
  const isAdmin = useAuthStore(selectIsAdmin);
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const [userMenuVisible, setUserMenuVisible] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const isSmallScreen = useIsSmallScreen();

  const menuItems = [
    { route: 'Dashboard', icon: 'home', label: t('nav.parent.dashboard'), show: true },
    { route: 'Tasks', icon: 'clipboard-list', label: t('nav.tasks'), show: true },
    { route: 'Shop', icon: 'gift', label: t('nav.rewards'), show: true },
    { route: 'Notifications', icon: 'bell', label: 'Benachrichtigungen', show: true },
    { route: 'Admin', icon: 'shield-account', label: 'Admin', show: isAdmin },
  ];

  const handleLogout = () => {
    setUserMenuVisible(false);
    setDrawerVisible(false);
    logout();
  };

  const handleProfileClick = () => {
    setUserMenuVisible(false);
    setDrawerVisible(false);
    onNavigate('Profile');
  };

  const handleNavigation = (route: string) => {
    setDrawerVisible(false);
    onNavigate(route);
  };

  // Mobile Drawer Navigation
  const renderMobileDrawer = () => (
    <Portal>
      <Modal
        visible={drawerVisible}
        onDismiss={() => setDrawerVisible(false)}
        contentContainerStyle={[
          styles.drawerModal,
          { backgroundColor: theme.colors.surface }
        ]}
      >
        <View style={styles.drawerHeader}>
          <Avatar.Text
            size={48}
            label={user?.firstName?.charAt(0).toUpperCase() || 'U'}
            style={{ backgroundColor: theme.colors.primary }}
          />
          <View style={styles.drawerUserInfo}>
            <Text variant="titleMedium" style={{ color: theme.colors.onSurface }}>
              {user?.firstName} {user?.lastName}
            </Text>
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
              {user?.username}
            </Text>
          </View>
        </View>
        <Divider />
        <View style={styles.drawerContent}>
          {menuItems.filter(item => item.show).map((item) => (
            <Drawer.Item
              key={item.route}
              icon={item.icon}
              label={item.label}
              active={currentRoute === item.route}
              onPress={() => handleNavigation(item.route)}
            />
          ))}
          <Divider style={{ marginVertical: 8 }} />
          <Drawer.Item
            icon="account-circle"
            label="Profil & Einstellungen"
            onPress={handleProfileClick}
          />
          <Drawer.Item
            icon="logout"
            label={t('nav.logout')}
            onPress={handleLogout}
          />
        </View>
      </Modal>
    </Portal>
  );

  return (
    <>
      {isSmallScreen && renderMobileDrawer()}
      <Appbar.Header style={{ backgroundColor: theme.colors.surface }} elevated>
        <View style={styles.headerContent}>
          {/* Mobile: Hamburger Menu */}
          {isSmallScreen && (
            <Appbar.Action
              icon="menu"
              onPress={() => setDrawerVisible(true)}
              color={theme.colors.onSurface}
            />
          )}

          {/* Logo - always visible */}
          <View style={styles.headerLeft}>
            <MaterialCommunityIcons
              name="compass"
              size={isSmallScreen ? 24 : 28}
              color={theme.colors.primary}
              style={{ marginRight: isSmallScreen ? 4 : 8 }}
            />
            {!isSmallScreen && (
              <Text variant="titleLarge" style={{ color: theme.colors.primary, fontWeight: 'bold' }}>
                OpenFamilyCompass
              </Text>
            )}
          </View>

          {/* Desktop: Navigation Buttons */}
          {!isSmallScreen && (
            <View style={styles.headerNav}>
              {menuItems.filter(item => item.show).map((item) => (
                <Appbar.Action
                  key={item.route}
                  icon={item.icon}
                  onPress={() => onNavigate(item.route)}
                  color={currentRoute === item.route ? theme.colors.primary : theme.colors.onSurfaceVariant}
                  style={[
                    styles.navButton,
                    currentRoute === item.route && { backgroundColor: theme.colors.primaryContainer }
                  ]}
                />
              ))}
            </View>
          )}

          {/* Desktop: User Menu / Mobile: Just Avatar */}
          <View style={styles.headerRight}>
            {isSmallScreen ? (
              <Avatar.Text
                size={32}
                label={user?.firstName?.charAt(0).toUpperCase() || 'U'}
                style={{ backgroundColor: theme.colors.primary }}
              />
            ) : (
              <Menu
                visible={userMenuVisible}
                onDismiss={() => setUserMenuVisible(false)}
                anchor={
                  <TouchableOpacity
                    style={styles.userMenuButton}
                    onPress={() => setUserMenuVisible(true)}
                  >
                    <Avatar.Text
                      size={36}
                      label={user?.firstName?.charAt(0).toUpperCase() || 'U'}
                      style={{ backgroundColor: theme.colors.primary }}
                    />
                    <Text variant="bodyMedium" style={{ color: theme.colors.onSurface, marginLeft: 12 }}>
                      {user?.firstName}
                    </Text>
                    <MaterialCommunityIcons
                      name={userMenuVisible ? 'chevron-up' : 'chevron-down'}
                      size={20}
                      color={theme.colors.onSurfaceVariant}
                      style={{ marginLeft: 4 }}
                    />
                  </TouchableOpacity>
                }
                anchorPosition="bottom"
              >
                <Menu.Item
                  onPress={handleProfileClick}
                  leadingIcon="account-circle"
                  title="Profil & Einstellungen"
                />
                <Divider />
                <Menu.Item
                  onPress={handleLogout}
                  leadingIcon="logout"
                  title={t('nav.logout')}
                />
              </Menu>
            )}
          </View>
        </View>
      </Appbar.Header>
    </>
  );
};

// Custom Footer
const CustomFooter: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();

  return (
    <View style={[styles.footer, { backgroundColor: theme.colors.surfaceVariant }]}>
      <View style={styles.footerContent}>
        <View style={styles.footerSection}>
          <MaterialCommunityIcons
            name="information"
            size={16}
            color={theme.colors.onSurfaceVariant}
            style={{ marginRight: 4 }}
          />
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            Version 1.0.0
          </Text>
        </View>

        <View style={styles.footerSection}>
          <MaterialCommunityIcons
            name="open-source-initiative"
            size={16}
            color={theme.colors.onSurfaceVariant}
            style={{ marginRight: 4 }}
          />
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {t('login.footer')}
          </Text>
        </View>

        <View style={styles.footerSection}>
          <Text
            variant="bodySmall"
            style={{ color: theme.colors.primary, textDecorationLine: 'underline', cursor: 'pointer' }}
            onPress={() => {/* TODO: Navigate to help page */ }}
          >
            Hilfe
          </Text>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant, marginHorizontal: 8 }}>
            •
          </Text>
          <Text
            variant="bodySmall"
            style={{ color: theme.colors.primary, textDecorationLine: 'underline', cursor: 'pointer' }}
            onPress={() => {/* TODO: Navigate to docs */ }}
          >
            Dokumentation
          </Text>
        </View>
      </View>
    </View>
  );
};

// Main Stack Navigator with Custom Header
const MainStackNavigator: React.FC = () => {
  const theme = useTheme();
  const isChild = useAuthStore(selectIsChild);
  const isAdmin = useAuthStore(selectIsAdmin);
  const [currentRoute, setCurrentRoute] = React.useState('Dashboard');

  const DashboardComponent = isChild ? ChildDashboardScreen : ParentDashboardScreen;

  const navigation = useNavigation();

  const handleNavigate = (route: string) => {
    setCurrentRoute(route);
    navigation.navigate(route as never);
  };

  return (
    <View style={{ flex: 1 }}>
      <CustomHeader currentRoute={currentRoute} onNavigate={handleNavigate} />

      <View style={{ flex: 1 }}>
        <MainStack.Navigator
          screenOptions={{
            headerShown: false,
          }}
          screenListeners={{
            state: (e) => {
              const state = e.data.state;
              if (state) {
                const route = state.routes[state.index];
                setCurrentRoute(route.name);
              }
            },
          }}
        >
          <MainStack.Screen name="Dashboard" component={DashboardComponent} />
          <MainStack.Screen name="Tasks" component={TasksStackNavigator} />
          <MainStack.Screen name="Shop" component={ShopStackNavigator} />
          <MainStack.Screen name="Notifications" component={NotificationsScreen} />
          <MainStack.Screen name="Profile" component={ProfileStackNavigator} />
          {isAdmin && (
            <MainStack.Screen name="Admin" component={AdminStackNavigator} />
          )}
        </MainStack.Navigator>
      </View>

      <CustomFooter />
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
      {isAuthenticated ? <MainStackNavigator /> : <AuthNavigator />}
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerContent: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 8,
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  headerNav: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  userMenuButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
  },
  navButton: {
    marginHorizontal: 2,
  },
  footer: {
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.1)',
  },
  footerContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingVertical: 16,
    flexWrap: 'wrap',
    gap: 16,
  },
  footerSection: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  // Mobile Drawer Styles
  drawerModal: {
    position: 'absolute',
    top: 0,
    left: 0,
    bottom: 0,
    width: 280,
    borderTopRightRadius: 16,
    borderBottomRightRadius: 16,
  },
  drawerHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    paddingTop: 48,
  },
  drawerUserInfo: {
    marginLeft: 12,
    flex: 1,
  },
  drawerContent: {
    flex: 1,
    paddingTop: 8,
  },
});
