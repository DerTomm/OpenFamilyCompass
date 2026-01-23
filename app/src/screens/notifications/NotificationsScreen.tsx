import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNotifications, useMarkAsRead } from '../../hooks/useApi';
import { NotificationResponse, NotificationType } from '../../types/api';

const NOTIFICATION_ICONS: Record<NotificationType, string> = {
  TASK_COMPLETED: '✅',
  TASK_APPROVED: '🎉',
  TASK_REJECTED: '❌',
  TASK_EXPIRED: '⏰',
  REWARD_REQUESTED: '🎁',
  REWARD_APPROVED: '🏆',
  REWARD_REJECTED: '😔',
  POINTS_EARNED: '⭐',
};

const NOTIFICATION_COLORS: Record<NotificationType, string> = {
  TASK_COMPLETED: '#9C27B0',
  TASK_APPROVED: '#4CAF50',
  TASK_REJECTED: '#F44336',
  TASK_EXPIRED: '#FF9800',
  REWARD_REQUESTED: '#2196F3',
  REWARD_APPROVED: '#4CAF50',
  REWARD_REJECTED: '#F44336',
  POINTS_EARNED: '#FFC107',
};

interface NotificationCardProps {
  notification: NotificationResponse;
  onPress: () => void;
}

const NotificationCard: React.FC<NotificationCardProps> = ({ notification, onPress }) => {
  const timeAgo = getTimeAgo(new Date(notification.createdAt));
  
  return (
    <TouchableOpacity 
      style={[styles.card, !notification.read && styles.cardUnread]} 
      onPress={onPress}
    >
      <View style={[styles.iconContainer, { backgroundColor: NOTIFICATION_COLORS[notification.type] + '20' }]}>
        <Text style={styles.icon}>{NOTIFICATION_ICONS[notification.type]}</Text>
      </View>
      <View style={styles.content}>
        <Text style={[styles.title, !notification.read && styles.titleUnread]}>
          {notification.title}
        </Text>
        <Text style={styles.message} numberOfLines={2}>
          {notification.message}
        </Text>
        <Text style={styles.time}>{timeAgo}</Text>
      </View>
      {!notification.read && <View style={styles.unreadDot} />}
    </TouchableOpacity>
  );
};

function getTimeAgo(date: Date): string {
  const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000);
  
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;
  return date.toLocaleDateString();
}

export const NotificationsScreen: React.FC = () => {
  const { data: notifications, isLoading, refetch, isRefetching } = useNotifications({ limit: 50 });
  const markAsRead = useMarkAsRead();

  const handlePress = (notification: NotificationResponse) => {
    if (!notification.read) {
      markAsRead.mutate(notification.id);
    }
    // TODO: Navigate to relevant screen based on notification type
  };

  const unreadCount = notifications?.filter(n => !n.read).length || 0;

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      {unreadCount > 0 && (
        <View style={styles.header}>
          <Text style={styles.headerText}>
            {unreadCount} unread notification{unreadCount !== 1 ? 's' : ''}
          </Text>
        </View>
      )}

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : (
        <FlatList
          data={notifications}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <NotificationCard
              notification={item}
              onPress={() => handlePress(item)}
            />
          )}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>🔔</Text>
              <Text style={styles.emptyText}>No notifications yet</Text>
              <Text style={styles.emptySubtext}>
                You'll see updates about tasks, rewards, and more here
              </Text>
            </View>
          }
        />
      )}
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
    padding: 12,
    alignItems: 'center',
  },
  headerText: {
    color: '#fff',
    fontWeight: '600',
  },
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  listContent: {
    padding: 16,
    flexGrow: 1,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  cardUnread: {
    backgroundColor: '#e3f2fd',
  },
  iconContainer: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  icon: {
    fontSize: 20,
  },
  content: {
    flex: 1,
  },
  title: {
    fontSize: 16,
    color: '#333',
    marginBottom: 4,
  },
  titleUnread: {
    fontWeight: '600',
  },
  message: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
    marginBottom: 4,
  },
  time: {
    fontSize: 12,
    color: '#999',
  },
  unreadDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#2196F3',
    marginLeft: 8,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  emptyEmoji: {
    fontSize: 64,
    marginBottom: 16,
  },
  emptyText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  emptySubtext: {
    color: '#666',
    textAlign: 'center',
    lineHeight: 22,
  },
});
