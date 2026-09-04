import React from 'react';
import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  StyleSheet,
  View,
} from 'react-native';
import { Divider, Surface, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { EmptyState } from '../../components/ui';
import { usePointTransactions } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { useAuthStore } from '../../store/authStore';
import { spacing } from '../../theme/theme';
import { PointTransactionResponse } from '../../types/api';

export const PointsHistoryScreen: React.FC = () => {
  const theme = useTheme();
  const { t, language } = useI18n();
  const user = useAuthStore((state) => state.user);
  const {
    data,
    isLoading,
    isError,
    refetch,
    isRefetching,
  } = usePointTransactions({ userId: user?.id, limit: 500 });

  const transactions = data?.transactions ?? [];
  const locale = language === 'de' ? 'de-DE' : 'en-US';

  const renderTransaction = ({ item }: { item: PointTransactionResponse }) => (
    <View>
      <View style={styles.transaction}>
        <View style={styles.transactionContent}>
          <Text variant="bodyLarge" style={{ color: theme.colors.onSurface }}>
            {item.description || t(`point.transaction.type.${item.type}`)}
          </Text>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {new Date(item.createdAt).toLocaleString(locale)}
          </Text>
        </View>
        <View style={styles.values}>
          <Text
            variant="titleMedium"
            style={{
              color: item.points >= 0 ? '#4CAF50' : theme.colors.error,
              fontWeight: '700',
            }}
          >
            {item.points > 0 ? `+${item.points}` : item.points}
          </Text>
          {item.balanceAfter !== undefined && (
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
              {t('history.balance')}: {item.balanceAfter}
            </Text>
          )}
        </View>
      </View>
      <Divider />
    </View>
  );

  return (
    <SafeAreaView
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      edges={['bottom']}
    >
      <Surface style={styles.balanceCard} elevation={1}>
        <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
          {t('history.current.balance')}
        </Text>
        <Text variant="headlineMedium" style={{ color: theme.colors.primary, fontWeight: '700' }}>
          {user?.totalPoints ?? 0}
        </Text>
      </Surface>

      {isLoading ? (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={theme.colors.primary} />
        </View>
      ) : isError ? (
        <EmptyState
          icon="alert-circle-outline"
          title={t('common.error')}
          message={t('setup.server.connection.error')}
        />
      ) : (
        <FlatList
          data={transactions}
          keyExtractor={(item) => item.id.toString()}
          renderItem={renderTransaction}
          contentContainerStyle={transactions.length === 0 ? styles.emptyList : styles.list}
          refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} />}
          ListEmptyComponent={
            <EmptyState
              icon="history"
              title={t('history.points.history')}
              message={t('history.no.activities')}
            />
          }
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  balanceCard: {
    margin: spacing.md,
    marginBottom: 0,
    padding: spacing.md,
    borderRadius: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  list: {
    padding: spacing.md,
  },
  emptyList: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  transaction: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.md,
  },
  transactionContent: {
    flex: 1,
    gap: spacing.xs,
  },
  values: {
    alignItems: 'flex-end',
    marginLeft: spacing.md,
    gap: spacing.xs,
  },
});
