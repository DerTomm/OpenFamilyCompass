import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { usePendingRedemptions } from '../../hooks/useApi';
import { redemptionsApi } from '../../api/services';
import { RewardRedemptionResponse } from '../../types/api';

interface RedemptionCardProps {
  redemption: RewardRedemptionResponse;
  onApprove: () => void;
  onReject: () => void;
  onDeliver: () => void;
  isLoading: boolean;
}

const RedemptionCard: React.FC<RedemptionCardProps> = ({
  redemption,
  onApprove,
  onReject,
  onDeliver,
  isLoading,
}) => {
  const isRequested = redemption.status === 'REQUESTED';
  const isApproved = redemption.status === 'APPROVED';

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.childInfo}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {redemption.user.firstName.charAt(0)}
            </Text>
          </View>
          <Text style={styles.childName}>{redemption.user.firstName}</Text>
        </View>
        <Text style={styles.date}>
          {new Date(redemption.requestedAt).toLocaleDateString()}
        </Text>
      </View>

      <View style={styles.rewardInfo}>
        <Text style={styles.rewardTitle}>{redemption.reward.title}</Text>
        <Text style={styles.pointsSpent}>{redemption.pointsSpent} pts</Text>
      </View>

      {redemption.reward.description && (
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
              <Text style={styles.rejectButtonText}>Reject</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.actionButton, styles.approveButton]}
              onPress={onApprove}
              disabled={isLoading}
            >
              {isLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.approveButtonText}>Approve</Text>
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
              <Text style={styles.deliverButtonText}>Mark as Delivered</Text>
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
    Alert.alert(
      'Approve Redemption',
      `Approve ${redemption.user.firstName}'s request for "${redemption.reward.title}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Approve', onPress: () => approveRedemption.mutate(redemption.id) },
      ]
    );
  };

  const handleReject = (redemption: RewardRedemptionResponse) => {
    Alert.alert(
      'Reject Redemption',
      `Reject ${redemption.user.firstName}'s request? Points will be refunded.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Reject',
          style: 'destructive',
          onPress: () => rejectRedemption.mutate({ id: redemption.id }),
        },
      ]
    );
  };

  const handleDeliver = (redemption: RewardRedemptionResponse) => {
    Alert.alert(
      'Mark as Delivered',
      `Confirm that "${redemption.reward.title}" has been delivered to ${redemption.user.firstName}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Confirm', onPress: () => deliverRedemption.mutate(redemption.id) },
      ]
    );
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
                  Pending Approval ({requestedRedemptions.length})
                </Text>
              </View>
            ) : null
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>🎁</Text>
              <Text style={styles.emptyText}>No pending redemptions</Text>
              <Text style={styles.emptySubtext}>
                Reward requests from children will appear here
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
    color: '#666',
    textTransform: 'uppercase',
  },
  card: {
    backgroundColor: '#fff',
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
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#9C27B0',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 10,
  },
  avatarText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  childName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
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
    color: '#333',
    flex: 1,
  },
  pointsSpent: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#9C27B0',
  },
  description: {
    color: '#666',
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
    color: '#333',
    marginBottom: 8,
  },
  emptySubtext: {
    color: '#666',
    textAlign: 'center',
  },
});
