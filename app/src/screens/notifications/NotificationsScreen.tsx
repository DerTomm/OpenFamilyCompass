import React from 'react';
import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTheme } from 'react-native-paper';
import { useMarkAsRead, useNotifications } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
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
  t: (key: string, params?: Record<string, string>) => string;
  styles: any;
}

const NotificationCard: React.FC<NotificationCardProps> = ({ notification, onPress, t, styles }) => {
  const timeAgo = getTimeAgo(new Date(notification.createdAt), t);

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

function getTimeAgo(date: Date, t: (key: string, params?: Record<string, string>) => string): string {
  const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000);

  if (seconds < 60) return t('notifications.time.just_now');
  if (seconds < 3600) return t('notifications.time.minutes_ago', { 0: Math.floor(seconds / 60).toString() });
  if (seconds < 86400) return t('notifications.time.hours_ago', { 0: Math.floor(seconds / 3600).toString() });
  if (seconds < 604800) return t('notifications.time.days_ago', { 0: Math.floor(seconds / 86400).toString() });
  return date.toLocaleDateString();
}

export const NotificationsScreen: React.FC = () => {
  const { data: notifications, isLoading, refetch, isRefetching } = useNotifications({ limit: 50 });
  const markAsRead = useMarkAsRead();
  const { t } = useI18n();
  const theme = useTheme();
  const styles = createStyles(theme);

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
            {t('notifications.unread.count', { 0: unreadCount.toString() })}
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
              t={t}
              styles={styles}
            />
          )}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>🔔</Text>
              <Text style={styles.emptyText}>{t('notifications.empty.title')}</Text>
              <Text style={styles.emptySubtext}>
                {t('notifications.empty.subtitle')}
              </Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
};

const createStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
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
    backgroundColor: theme.colors.surface,
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
    color: theme.colors.onSurface,
    marginBottom: 4,
  },
  titleUnread: {
    fontWeight: '600',
  },
  message: {
    fontSize: 14,
    color: theme.colors.onSurfaceVariant,
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
    color: theme.colors.onSurface,
    marginBottom: 8,
  },
  emptySubtext: {
    color: theme.colors.onSurfaceVariant,
    textAlign: 'center',
    lineHeight: 22,
  },
});
