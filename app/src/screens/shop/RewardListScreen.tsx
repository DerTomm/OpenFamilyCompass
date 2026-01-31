import React from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  RefreshControl,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRequestRedemption, useRewards } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { selectIsChild, useAuthStore } from '../../store/authStore';
import { RewardResponse } from '../../types/api';

interface RewardCardProps {
  reward: RewardResponse;
  userPoints: number;
  onRedeem: () => void;
  isRedeeming: boolean;
  isChild: boolean;
  t: (key: string) => string;
}

const RewardCard: React.FC<RewardCardProps> = ({
  reward,
  userPoints,
  onRedeem,
  isRedeeming,
  isChild,
  t,
}) => {
  const canAfford = userPoints >= reward.pointsCost;

  return (
    <View style={styles.card}>
      <View style={styles.cardContent}>
        <View style={styles.rewardIcon}>
          <Text style={styles.rewardEmoji}>🎁</Text>
        </View>
        <View style={styles.rewardInfo}>
          <Text style={styles.rewardTitle}>{reward.title}</Text>
          {reward.description && (
            <Text style={styles.rewardDescription} numberOfLines={2}>
              {reward.description}
            </Text>
          )}
          <View style={styles.costContainer}>
            <Text style={[styles.costValue, !canAfford && styles.costInsufficient]}>
              {reward.pointsCost}
            </Text>
            <Text style={styles.costLabel}> {t('points.label')}</Text>
          </View>
        </View>
      </View>

      {isChild && (
        <TouchableOpacity
          style={[
            styles.redeemButton,
            !canAfford && styles.redeemButtonDisabled,
          ]}
          onPress={onRedeem}
          disabled={!canAfford || isRedeeming}
        >
          {isRedeeming ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={styles.redeemButtonText}>
              {canAfford ? t('rewards.redeem') : t('rewards.not.enough.points')}
            </Text>
          )}
        </TouchableOpacity>
      )}
    </View>
  );
};

export const RewardListScreen: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const isChild = useAuthStore(selectIsChild);
  const { t } = useI18n();

  const { data: rewards, isLoading, refetch, isRefetching } = useRewards(true);
  const requestRedemption = useRequestRedemption();

  const handleRedeem = (reward: RewardResponse) => {
    Alert.alert(
      t('rewards.redeem.confirm.title'),
      t('rewards.redeem.confirm.message', { 0: reward.title, 1: reward.pointsCost }),
      [
        { text: t('button.cancel'), style: 'cancel' },
        {
          text: t('rewards.redeem'),
          onPress: () => requestRedemption.mutate(reward.id),
        },
      ]
    );
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      {isChild && (
        <View style={styles.balanceHeader}>
          <Text style={styles.balanceLabel}>{t('child.dashboard.points.balance')}</Text>
          <Text style={styles.balanceValue}>{user?.totalPoints || 0} {t('points.label')}</Text>
        </View>
      )}

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : (
        <FlatList
          data={rewards}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <RewardCard
              reward={item}
              userPoints={user?.totalPoints || 0}
              onRedeem={() => handleRedeem(item)}
              isRedeeming={requestRedemption.isPending && requestRedemption.variables === item.id}
              isChild={isChild}
              t={t}
            />
          )}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>🏪</Text>
              <Text style={styles.emptyText}>{t('empty.no_rewards')}</Text>
              <Text style={styles.emptySubtext}>{t('rewards.check.later')}</Text>
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
  balanceHeader: {
    backgroundColor: '#9C27B0',
    padding: 20,
    alignItems: 'center',
  },
  balanceLabel: {
    color: 'rgba(255,255,255,0.8)',
    fontSize: 14,
  },
  balanceValue: {
    color: '#fff',
    fontSize: 28,
    fontWeight: 'bold',
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
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  cardContent: {
    flexDirection: 'row',
  },
  rewardIcon: {
    width: 60,
    height: 60,
    borderRadius: 12,
    backgroundColor: '#f5f5f5',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  rewardEmoji: {
    fontSize: 32,
  },
  rewardInfo: {
    flex: 1,
  },
  rewardTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  rewardDescription: {
    color: '#666',
    fontSize: 14,
    marginBottom: 8,
  },
  costContainer: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  costValue: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#9C27B0',
  },
  costInsufficient: {
    color: '#999',
  },
  costLabel: {
    color: '#666',
    fontSize: 14,
  },
  redeemButton: {
    backgroundColor: '#9C27B0',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 12,
  },
  redeemButtonDisabled: {
    backgroundColor: '#ccc',
  },
  redeemButtonText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
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
    marginBottom: 4,
  },
  emptySubtext: {
    color: '#666',
  },
});
