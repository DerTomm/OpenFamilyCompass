import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useQueryClient } from '@tanstack/react-query';
import React from 'react';
import { ScrollView, StyleSheet, TouchableOpacity, View } from 'react-native';
import { ActivityIndicator, Badge, Divider, List, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card, EmptyState, UserAvatar } from '../../components/ui';
import { queryKeys, useChildren, usePendingRedemptions, usePendingTasks, usePointTransactions } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { ActivitiesStackParamList } from '../../navigation/types';
import { spacing } from '../../theme/theme';

type NavigationProp = NativeStackNavigationProp<ActivitiesStackParamList>;

export const ParentDashboardScreen: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  const navigation = useNavigation<NavigationProp>();
  const queryClient = useQueryClient();
  const { data: children, isLoading: isChildrenLoading, error: childrenError } = useChildren();
  const { data: transactionsData } = usePointTransactions({ limit: 5 });
  const transactions = transactionsData?.transactions || [];

  const { data: pendingTasks } = usePendingTasks();
  const { data: pendingRedemptions } = usePendingRedemptions();

  // Refresh children data when screen comes into focus
  useFocusEffect(
    React.useCallback(() => {
      queryClient.invalidateQueries({ queryKey: queryKeys.children });
    }, [queryClient])
  );

  // Count pending items per child
  const getChildPendingCounts = (childId: number) => {
    const taskCount = pendingTasks?.filter(task => task.assignedUser.id === childId).length || 0;
    const redemptionCount = pendingRedemptions?.filter(redemption => redemption.user.id === childId).length || 0;
    return { tasks: taskCount, redemptions: redemptionCount };
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const formatPoints = (points: number) => {
    return points > 0 ? `+${points}` : String(points);
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {/* Children Overview */}
        <View style={styles.section}>
          <Text variant="titleLarge" style={[styles.sectionTitle, { color: theme.colors.onBackground }]}>
            {t('dashboard.children.overview')}
          </Text>

          <Card elevation={1}>
            {isChildrenLoading ? (
              <View style={styles.childrenLoading}>
                <ActivityIndicator />
              </View>
            ) : childrenError ? (
              <View style={styles.childrenError}>
                <Text variant="bodyMedium" style={{ color: theme.colors.error }}>
                  {(childrenError as Error).message}
                </Text>
              </View>
            ) : children && children.length > 0 ? (
              <View>
                {children.map((child, idx) => {
                  const pendingCounts = getChildPendingCounts(child.id);

                  return (
                    <React.Fragment key={child.id}>
                      <TouchableOpacity onPress={() => navigation.navigate('ChildDetail', { childId: child.id })}>
                        <List.Item
                          title={child.firstName}
                          description={`${child.totalPoints} ${t('points.label')}`}
                          left={() => (
                            <UserAvatar
                              avatarType={child.avatarType}
                              avatarIconName={child.avatarIconName}
                              avatarPath={child.avatarPath}
                              firstName={child.firstName}
                              size={40}
                            />
                          )}
                          right={() => (
                            <View style={styles.badgeContainer}>
                              {pendingCounts.tasks > 0 && (
                                <View style={styles.badgeWrapper}>
                                  <MaterialCommunityIcons
                                    name="clipboard-check"
                                    size={20}
                                    color={theme.colors.error}
                                  />
                                  <Badge
                                    size={18}
                                    style={[styles.badge, { backgroundColor: theme.colors.error }]}
                                  >
                                    {pendingCounts.tasks}
                                  </Badge>
                                </View>
                              )}
                              {pendingCounts.redemptions > 0 && (
                                <View style={styles.badgeWrapper}>
                                  <MaterialCommunityIcons
                                    name="gift"
                                    size={20}
                                    color={theme.colors.secondary}
                                  />
                                  <Badge
                                    size={18}
                                    style={[styles.badge, { backgroundColor: theme.colors.secondary }]}
                                  >
                                    {pendingCounts.redemptions}
                                  </Badge>
                                </View>
                              )}
                            </View>
                          )}
                        />
                      </TouchableOpacity>
                      {idx < children.length - 1 && <Divider />}
                    </React.Fragment>
                  );
                })}
              </View>
            ) : (
              <EmptyState
                icon="account-child"
                title={t('empty.no_children')}
                message={t('empty.no_children.desc')}
              />
            )}
          </Card>
        </View>

        {/* Recent Activity */}
        <View style={styles.section}>
          <Text variant="titleLarge" style={[styles.sectionTitle, { color: theme.colors.onBackground }]}>
            {t('dashboard.activities')}
          </Text>

          <Card elevation={1}>
            {transactions.length > 0 ? (
              <View style={{ padding: 12 }}>
                {transactions.map((transaction, index) => {
                  const child = children?.find(c => c.id === transaction.userId);

                  return (
                    <View key={transaction.id}>
                      <View style={[styles.transactionRow, transaction.status === 'CANCELLED' && styles.transactionCancelled]}>
                        <View style={styles.transactionLeft}>
                          {child && (
                            <View style={styles.childBadge}>
                              <UserAvatar
                                avatarType={child.avatarType}
                                avatarIconName={child.avatarIconName}
                                avatarPath={child.avatarPath}
                                firstName={child.firstName}
                                size={48}
                              />
                            </View>
                          )}
                          <View style={styles.transactionInfo}>
                            <View style={styles.typeBadgeRow}>
                              {child && (
                                <View style={[styles.typeBadge, { backgroundColor: theme.colors.primaryContainer, marginRight: spacing.xs }]}>
                                  <Text variant="bodySmall" style={{ color: theme.colors.onPrimaryContainer, fontWeight: '600' }}>
                                    {child.firstName}
                                  </Text>
                                </View>
                              )}
                              <View style={[styles.typeBadge, { backgroundColor: '#E0E0E0', marginRight: spacing.xs }]}>
                                <Text variant="bodySmall" style={{ color: '#000', fontWeight: '600' }}>
                                  {t(`point.transaction.type.${transaction.type}`)}
                                </Text>
                              </View>
                              {transaction.status === 'PENDING' && (
                                <MaterialCommunityIcons
                                  name="timer-sand"
                                  size={16}
                                  color={theme.colors.outline}
                                  style={{ marginLeft: spacing.xs }}
                                />
                              )}
                              {transaction.status === 'CANCELLED' && (
                                <MaterialCommunityIcons
                                  name="cancel"
                                  size={16}
                                  color={theme.colors.outline}
                                  style={{ marginLeft: spacing.xs }}
                                />
                              )}
                            </View>
                            {transaction.description && (
                              <Text variant="bodyMedium" style={[{ fontWeight: '500', marginTop: spacing.xs }, transaction.status === 'CANCELLED' && { textDecorationLine: 'line-through' }]}>
                                {transaction.description}
                              </Text>
                            )}
                            <Text variant="bodySmall" style={{ color: theme.colors.outline }}>
                              {formatDate(transaction.createdAt)}
                            </Text>
                          </View>
                        </View>
                        {transaction.status !== 'CANCELLED' && (
                          <Text
                            variant="bodyLarge"
                            style={{
                              fontWeight: '600',
                              color: transaction.points > 0 ? '#4CAF50' : transaction.points < 0 ? '#F44336' : theme.colors.onSurface,
                            }}
                          >
                            {formatPoints(transaction.points)}
                          </Text>
                        )}
                      </View>
                      {index < transactions.length - 1 && <Divider style={{ marginVertical: 8 }} />}
                    </View>
                  );
                })}
              </View>
            ) : (
              <EmptyState
                icon="history"
                title={t('empty.no_activities')}
                message={t('empty.no_activities.desc')}
              />
            )}
          </Card>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xl,
  },
  section: {
    marginBottom: spacing.lg,
  },
  sectionTitle: {
    fontWeight: '600',
    marginBottom: spacing.md,
  },
  divider: {
    marginVertical: spacing.md,
  },
  childrenLoading: {
    paddingVertical: spacing.lg,
  },
  childrenError: {
    paddingVertical: spacing.md,
  },
  transactionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  transactionLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: spacing.sm,
  },
  childBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: spacing.sm,
    paddingVertical: 4,
  },
  childName: {
    marginLeft: spacing.xs,
    fontWeight: '500',
  },
  transactionInfo: {
    flex: 1,
  },
  typeBadge: {
    alignSelf: 'flex-start',
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
    borderRadius: 4,
  },
  typeBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  transactionCancelled: {
    opacity: 0.4,
  },
  badgeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: spacing.sm,
  },
  badgeWrapper: {
    position: 'relative',
    marginRight: spacing.sm,
  },
  badge: {
    position: 'absolute',
    top: -6,
    right: -8,
  },
});
