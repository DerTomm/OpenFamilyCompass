import { MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, Surface, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card, EmptyState, QuickActionCard, UserAvatar } from '../../components/ui';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useI18n } from '../../i18n/I18nContext';
import { useAuthStore } from '../../store/authStore';
import { ActivitiesStackParamList } from '../../navigation/types';
import { spacing } from '../../theme/theme';
import { usePointTransactions } from '../../hooks/useApi';
import { PointTransactionResponse } from '../../types/api';

type NavigationProp = NativeStackNavigationProp<ActivitiesStackParamList>;

export const ChildDashboardScreen: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const theme = useTheme();
  const { t } = useI18n();
  const navigation = useNavigation<NavigationProp>();
  const styles = createStyles(theme);

  // Load recent transactions
  const { data: transactionsData } = usePointTransactions({ userId: user?.id, limit: 5 });
  const transactions = transactionsData?.transactions || [];

  // Total points
  const totalPoints = user?.totalPoints || 0;

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
        {/* Points Card */}
        <Surface style={styles.pointsCard} elevation={3}>
          <LinearGradient
            colors={[theme.colors.primary, theme.colors.secondary]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientCard}
          >
            <View style={styles.pointsHeader}>
              <UserAvatar
                avatarType={user?.avatarType}
                avatarIconName={user?.avatarIconName}
                avatarPath={user?.avatarPath}
                firstName={user?.firstName}
                size={48}
              />
              <View style={styles.pointsInfo}>
                <Text variant="titleMedium" style={styles.pointsLabel}>
                  {user?.firstName}
                </Text>
                <Text variant="displayMedium" style={styles.pointsValue}>
                  {totalPoints} {t('child.dashboard.points.badge')}
                </Text>
              </View>
            </View>
          </LinearGradient>
        </Surface>

        <Divider style={styles.divider} />

        {/* Quick Access */}
        <View style={styles.section}>
          <Text
            variant="titleLarge"
            style={[styles.sectionTitle, { color: theme.colors.onBackground, marginLeft: 0 }]}
          >
            {t('dashboard.quick.actions')}
          </Text>

          <QuickActionCard
            title={t('tasks.my.title')}
            icon="clipboard-list"
            onPress={() => navigation.navigate('TaskList')}
          />
          <QuickActionCard
            title={t('shop.title.page')}
            icon="gift"
            onPress={() => navigation.navigate('RewardList')}
            color={theme.colors.secondary}
          />
          <QuickActionCard
            title={t('child.behaviors.title')}
            icon="star-circle"
            onPress={() => navigation.navigate('ChildBehaviorList')}
            color={theme.colors.tertiary}
          />
        </View>

        {/* Today's Tasks */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <MaterialCommunityIcons
              name="clipboard-list"
              size={24}
              color={theme.colors.primary}
            />
            <Text
              variant="titleLarge"
              style={[styles.sectionTitle, { color: theme.colors.onBackground }]}
            >
              {t('child.dashboard.tasks.available')}
            </Text>
          </View>

          <Card elevation={1}>
            <EmptyState
              icon="clipboard-check"
              title={t('empty.no_tasks')}
              message={t('empty.no_tasks.desc')}
            />
          </Card>
        </View>

        {/* Recent Activity */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <MaterialCommunityIcons
              name="history"
              size={24}
              color={theme.colors.secondary}
            />
            <Text
              variant="titleLarge"
              style={[styles.sectionTitle, { color: theme.colors.onBackground }]}
            >
              Letzte Aktivitäten
            </Text>
          </View>

          <Card elevation={1}>
            {transactions.length > 0 ? (
              <View style={{ padding: 12 }}>
                {transactions.map((transaction, index) => (
                  <View key={transaction.id}>
                    <View style={styles.transactionRow}>
                      <View style={styles.transactionInfo}>
                        <Text variant="bodyMedium" style={{ fontWeight: '500' }}>
                          {transaction.description || t(`point.transaction.type.${transaction.type}`)}
                        </Text>
                        <Text variant="bodySmall" style={{ color: theme.colors.outline }}>
                          {formatDate(transaction.createdAt)}
                        </Text>
                      </View>
                      <Text
                        variant="bodyLarge"
                        style={{
                          fontWeight: '600',
                          color: transaction.points > 0 ? '#4CAF50' : transaction.points < 0 ? '#F44336' : theme.colors.onSurface,
                        }}
                      >
                        {formatPoints(transaction.points)}
                      </Text>
                    </View>
                    {index < transactions.length - 1 && <Divider style={{ marginVertical: 8 }} />}
                  </View>
                ))}
              </View>
            ) : (
              <EmptyState
                icon="timeline-clock"
                title="Noch keine Aktivitäten"
                message="Hier erscheinen deine abgeschlossenen Aufgaben und Belohnungen"
              />
            )}
          </Card>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const createStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xl,
  },
  header: {
    marginBottom: spacing.lg,
    paddingTop: spacing.sm,
  },
  pointsCard: {
    borderRadius: 20,
    overflow: 'hidden',
    marginBottom: spacing.lg,
  },
  gradientCard: {
    padding: spacing.lg,
  },
  pointsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  pointsInfo: {
    flex: 1,
  },
  pointsLabel: {
    color: 'rgba(255, 255, 255, 0.9)',
    fontWeight: '600',
    marginBottom: spacing.xs,
  },
  pointsValue: {
    color: theme.colors.onPrimary,
    fontWeight: '900',
  },
  divider: {
    marginVertical: spacing.md,
  },
  section: {
    marginBottom: spacing.lg,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  sectionTitle: {
    fontWeight: '600',
    marginLeft: spacing.sm,
  },
  transactionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  transactionInfo: {
    flex: 1,
    marginRight: spacing.sm,
  },
});
