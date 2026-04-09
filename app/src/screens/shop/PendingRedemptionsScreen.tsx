import { useMutation, useQueryClient } from '@tanstack/react-query';
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
import { useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { redemptionsApi } from '../../api/services';
import { UserAvatar } from '../../components/ui';
import { usePendingRedemptions } from '../../hooks/useApi';
import { useDialogs } from '../../hooks/useDialogs';
import { useI18n } from '../../i18n/I18nContext';
import { RewardRedemptionResponse } from '../../types/api';

interface RedemptionCardProps {
  redemption: RewardRedemptionResponse;
  onApprove: () => void;
  onReject: () => void;
  onDeliver: () => void;
  isLoading: boolean;
  t: (key: string) => string;
  styles: any;
}

const RedemptionCard: React.FC<RedemptionCardProps> = ({
  redemption,
  onApprove,
  onReject,
  onDeliver,
  isLoading,
  t,
  styles,
}) => {
  const isRequested = redemption.status === 'REQUESTED';
  const isApproved = redemption.status === 'APPROVED';

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.childInfo}>
          <UserAvatar
            avatarType={redemption.user.avatarType}
            avatarIconName={redemption.user.avatarIconName}
            avatarPath={redemption.user.avatarPath}
            firstName={redemption.user.firstName}
            size={36}
          />
          <Text style={styles.childName}>{redemption.user.firstName}</Text>
        </View>
        <Text style={styles.date}>
          {new Date(redemption.requestedAt).toLocaleDateString()}
        </Text>
      </View>

      <View style={styles.rewardInfo}>
        <Text style={styles.rewardTitle}>{redemption.reward.title}</Text>
        <Text style={styles.pointsSpent}>{redemption.pointsSpent} {t('points.label')}</Text>
      </View>

      {Boolean(redemption.reward.description) && (
        <Text style={styles.description}>{redemption.reward.description}</Text>
      )}

      <View style={styles.actions}>
        {isRequested && (
          <>
            <TouchableOpacity
              style={[styles.actionButton, styles.rejectButton]}
              onPress={onReject}
              disabled={isLoading}
            >
              <Text style={styles.rejectButtonText}>{t('tasks.pending.reject')}</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.actionButton, styles.approveButton]}
              onPress={onApprove}
              disabled={isLoading}
            >
              {isLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.approveButtonText}>{t('tasks.pending.approve')}</Text>
              )}
            </TouchableOpacity>
          </>
        )}
        {isApproved && (
          <TouchableOpacity
            style={[styles.actionButton, styles.deliverButton]}
            onPress={onDeliver}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.deliverButtonText}>{t('rewards.mark.delivered')}</Text>
            )}
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

export const PendingRedemptionsScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const { data: redemptions, isLoading, refetch, isRefetching } = usePendingRedemptions();
  const { t } = useI18n();
  const { showConfirm, Dialogs } = useDialogs();
  const theme = useTheme();
  const styles = createStyles(theme);

  const approveRedemption = useMutation({
    mutationFn: (id: number) => redemptionsApi.approve(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['redemptions'] });
      queryClient.invalidateQueries({ queryKey: ['pendingRedemptions'] });
    },
  });

  const rejectRedemption = useMutation({
    mutationFn: ({ id, notes }: { id: number; notes?: string }) =>
      redemptionsApi.reject(id, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['redemptions'] });
      queryClient.invalidateQueries({ queryKey: ['pendingRedemptions'] });
    },
  });

  const deliverRedemption = useMutation({
    mutationFn: (id: number) => redemptionsApi.deliver(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['redemptions'] });
      queryClient.invalidateQueries({ queryKey: ['pendingRedemptions'] });
    },
  });

  const handleApprove = (redemption: RewardRedemptionResponse) => {
    showConfirm({
      title: t('rewards.approve.title'),
      message: t('rewards.approve.message', { 0: redemption.user.firstName, 1: redemption.reward.title }),
      onConfirm: () => approveRedemption.mutate(redemption.id),
      confirmText: t('tasks.pending.approve'),
      cancelText: t('button.cancel'),
    });
  };

  const handleReject = (redemption: RewardRedemptionResponse) => {
    showConfirm({
      title: t('rewards.reject.title'),
      message: t('rewards.reject.message', { 0: redemption.user.firstName }),
      onConfirm: () => rejectRedemption.mutate({ id: redemption.id }),
      confirmText: t('tasks.pending.reject'),
      cancelText: t('button.cancel'),
      destructive: true,
    });
  };

  const handleDeliver = (redemption: RewardRedemptionResponse) => {
    showConfirm({
      title: t('rewards.deliver.title'),
      message: t('rewards.deliver.message', { 0: redemption.reward.title, 1: redemption.user.firstName }),
      onConfirm: () => deliverRedemption.mutate(redemption.id),
      confirmText: t('button.confirm'),
      cancelText: t('button.cancel'),
    });
  };

  const requestedRedemptions = redemptions?.filter(r => r.status === 'REQUESTED') || [];
  const approvedRedemptions = redemptions?.filter(r => r.status === 'APPROVED') || [];

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : (
        <FlatList
          data={[...requestedRedemptions, ...approvedRedemptions]}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <RedemptionCard
              redemption={item}
              onApprove={() => handleApprove(item)}
              onReject={() => handleReject(item)}
              onDeliver={() => handleDeliver(item)}
              isLoading={
                approveRedemption.isPending ||
                rejectRedemption.isPending ||
                deliverRedemption.isPending
              }
              t={t}
              styles={styles}
            />
          )}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListHeaderComponent={
            requestedRedemptions.length > 0 ? (
              <View style={styles.sectionHeader}>
                <Text style={styles.sectionTitle}>
                  {t('rewards.pending.approval')} ({requestedRedemptions.length})
                </Text>
              </View>
            ) : null
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>🎁</Text>
              <Text style={styles.emptyText}>{t('rewards.no.pending')}</Text>
              <Text style={styles.emptySubtext}>
                {t('rewards.pending.description')}
              </Text>
            </View>
          }
        />
      )}
      <Dialogs />
    </SafeAreaView>
  );
};

const createStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
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
  sectionHeader: {
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: theme.colors.onSurfaceVariant,
    textTransform: 'uppercase',
  },
  card: {
    backgroundColor: theme.colors.surface,
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  childInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  childName: {
    fontSize: 16,
    fontWeight: '600',
    color: theme.colors.onSurface,
  },
  date: {
    fontSize: 13,
    color: '#999',
  },
  rewardInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  rewardTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: theme.colors.onSurface,
    flex: 1,
  },
  pointsSpent: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#9C27B0',
  },
  description: {
    color: theme.colors.onSurfaceVariant,
    marginBottom: 12,
  },
  actions: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 8,
  },
  actionButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  approveButton: {
    backgroundColor: '#4CAF50',
  },
  approveButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  rejectButton: {
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#F44336',
  },
  rejectButtonText: {
    color: '#F44336',
    fontWeight: '600',
  },
  deliverButton: {
    backgroundColor: '#2196F3',
  },
  deliverButtonText: {
    color: '#fff',
    fontWeight: '600',
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
  },
});
