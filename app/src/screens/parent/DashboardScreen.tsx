import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuthStore } from '../../store/authStore';

interface QuickActionProps {
  title: string;
  count?: number;
  color: string;
  onPress: () => void;
}

const QuickAction: React.FC<QuickActionProps> = ({ title, count, color, onPress }) => (
  <TouchableOpacity style={[styles.quickAction, { borderLeftColor: color }]} onPress={onPress}>
    <Text style={styles.quickActionTitle}>{title}</Text>
    {count !== undefined && (
      <View style={[styles.badge, { backgroundColor: color }]}>
        <Text style={styles.badgeText}>{count}</Text>
      </View>
    )}
  </TouchableOpacity>
);

export const ParentDashboardScreen: React.FC = () => {
  const user = useAuthStore((state) => state.user);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Text style={styles.greeting}>
            Welcome back, {user?.firstName || 'Parent'}!
          </Text>
          <Text style={styles.subGreeting}>
            Here's what's happening in your family
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Quick Actions</Text>
          <View style={styles.quickActionsGrid}>
            <QuickAction
              title="Tasks to Approve"
              count={0}
              color="#FF9800"
              onPress={() => {}}
            />
            <QuickAction
              title="Reward Requests"
              count={0}
              color="#9C27B0"
              onPress={() => {}}
            />
            <QuickAction
              title="Evaluate Behaviors"
              count={0}
              color="#2196F3"
              onPress={() => {}}
            />
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Children Overview</Text>
          <View style={styles.placeholder}>
            <Text style={styles.placeholderText}>
              Children cards will appear here
            </Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Recent Activity</Text>
          <View style={styles.placeholder}>
            <Text style={styles.placeholderText}>
              Activity feed coming soon
            </Text>
          </View>
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
  content: {
    padding: 16,
  },
  header: {
    marginBottom: 24,
  },
  greeting: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  subGreeting: {
    fontSize: 16,
    color: '#666',
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  quickActionsGrid: {
    gap: 12,
  },
  quickAction: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderLeftWidth: 4,
  },
  quickActionTitle: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
  },
  badge: {
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 4,
    minWidth: 24,
    alignItems: 'center',
  },
  badgeText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 14,
  },
  placeholder: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 32,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 100,
  },
  placeholderText: {
    color: '#999',
    fontSize: 16,
  },
});
