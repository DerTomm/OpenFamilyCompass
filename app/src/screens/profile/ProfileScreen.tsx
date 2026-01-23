import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuthStore, selectIsChild } from '../../store/authStore';
import { usePointTransactions } from '../../hooks/useApi';

interface MenuItemProps {
  icon: string;
  title: string;
  subtitle?: string;
  onPress: () => void;
  showBadge?: boolean;
}

const MenuItem: React.FC<MenuItemProps> = ({ icon, title, subtitle, onPress, showBadge }) => (
  <TouchableOpacity style={styles.menuItem} onPress={onPress}>
    <Text style={styles.menuIcon}>{icon}</Text>
    <View style={styles.menuContent}>
      <Text style={styles.menuTitle}>{title}</Text>
      {subtitle && <Text style={styles.menuSubtitle}>{subtitle}</Text>}
    </View>
    {showBadge && <View style={styles.badge} />}
    <Text style={styles.menuArrow}>›</Text>
  </TouchableOpacity>
);

export const ProfileScreen: React.FC = () => {
  const { user, logout } = useAuthStore();
  const isChild = useAuthStore(selectIsChild);
  
  const { data: transactions, isLoading } = usePointTransactions({ 
    userId: user?.id, 
    limit: 5 
  });

  const handleLogout = () => {
    Alert.alert(
      'Log Out',
      'Are you sure you want to log out?',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Log Out', style: 'destructive', onPress: logout },
      ]
    );
  };

  const getRoleLabel = (role?: string) => {
    switch (role) {
      case 'ADMIN': return 'Administrator';
      case 'PARENT': return 'Parent';
      case 'CHILD': return 'Child';
      default: return role;
    }
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <ScrollView>
        <View style={styles.header}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {user?.firstName?.charAt(0).toUpperCase() || '?'}
            </Text>
          </View>
          <Text style={styles.name}>{user?.firstName}</Text>
          <Text style={styles.role}>{getRoleLabel(user?.role)}</Text>
          
          {isChild && (
            <View style={styles.pointsCard}>
              <Text style={styles.pointsLabel}>Total Points</Text>
              <Text style={styles.pointsValue}>{user?.totalPoints || 0}</Text>
            </View>
          )}
        </View>

        {isChild && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Recent Points Activity</Text>
            {isLoading ? (
              <ActivityIndicator style={styles.activityLoader} />
            ) : transactions?.transactions.length ? (
              transactions.transactions.map((tx) => (
                <View key={tx.id} style={styles.transactionItem}>
                  <View style={styles.transactionInfo}>
                    <Text style={styles.transactionDesc}>
                      {tx.description || tx.type}
                    </Text>
                    <Text style={styles.transactionDate}>
                      {new Date(tx.createdAt).toLocaleDateString()}
                    </Text>
                  </View>
                  <Text style={[
                    styles.transactionPoints,
                    tx.points >= 0 ? styles.pointsPositive : styles.pointsNegative
                  ]}>
                    {tx.points >= 0 ? '+' : ''}{tx.points}
                  </Text>
                </View>
              ))
            ) : (
              <Text style={styles.noActivity}>No recent activity</Text>
            )}
          </View>
        )}

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Settings</Text>
          <View style={styles.menuGroup}>
            <MenuItem
              icon="👤"
              title="Edit Profile"
              subtitle="Change your name and avatar"
              onPress={() => {}}
            />
            <MenuItem
              icon="🔔"
              title="Notifications"
              onPress={() => {}}
            />
            <MenuItem
              icon="🎨"
              title="Appearance"
              subtitle={user?.theme === 'DARK' ? 'Dark mode' : 'Light mode'}
              onPress={() => {}}
            />
            <MenuItem
              icon="🔒"
              title="Change Password"
              onPress={() => {}}
            />
          </View>
        </View>

        <View style={styles.section}>
          <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
            <Text style={styles.logoutText}>Log Out</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.footer}>
          <Text style={styles.footerText}>OpenFamilyCompass v1.0.0</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#2196F3',
    padding: 24,
    alignItems: 'center',
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: 'rgba(255,255,255,0.3)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  avatarText: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#fff',
  },
  name: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 4,
  },
  role: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.8)',
  },
  pointsCard: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    borderRadius: 12,
    padding: 16,
    marginTop: 16,
    alignItems: 'center',
    minWidth: 150,
  },
  pointsLabel: {
    color: 'rgba(255,255,255,0.8)',
    fontSize: 14,
  },
  pointsValue: {
    color: '#fff',
    fontSize: 32,
    fontWeight: 'bold',
  },
  section: {
    padding: 16,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
    textTransform: 'uppercase',
    marginBottom: 12,
    marginLeft: 4,
  },
  menuGroup: {
    backgroundColor: '#fff',
    borderRadius: 12,
    overflow: 'hidden',
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  menuIcon: {
    fontSize: 20,
    marginRight: 12,
  },
  menuContent: {
    flex: 1,
  },
  menuTitle: {
    fontSize: 16,
    color: '#333',
  },
  menuSubtitle: {
    fontSize: 13,
    color: '#999',
    marginTop: 2,
  },
  badge: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#F44336',
    marginRight: 8,
  },
  menuArrow: {
    fontSize: 20,
    color: '#ccc',
  },
  activityLoader: {
    padding: 20,
  },
  transactionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
  },
  transactionInfo: {
    flex: 1,
  },
  transactionDesc: {
    fontSize: 15,
    color: '#333',
  },
  transactionDate: {
    fontSize: 12,
    color: '#999',
    marginTop: 2,
  },
  transactionPoints: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  pointsPositive: {
    color: '#4CAF50',
  },
  pointsNegative: {
    color: '#F44336',
  },
  noActivity: {
    textAlign: 'center',
    color: '#999',
    padding: 20,
  },
  logoutButton: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  logoutText: {
    color: '#F44336',
    fontSize: 16,
    fontWeight: '600',
  },
  footer: {
    padding: 24,
    alignItems: 'center',
  },
  footerText: {
    color: '#999',
    fontSize: 12,
  },
});
