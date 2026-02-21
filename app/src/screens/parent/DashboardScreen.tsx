import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Avatar, Divider, List, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card, EmptyState, QuickActionCard } from '../../components/ui';
import { useChildren } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { useAuthStore } from '../../store/authStore';
import { spacing } from '../../theme/theme';
import { useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { MainTabParamList } from '../../navigation/types';

type NavigationProp = BottomTabNavigationProp<MainTabParamList>;

export const ParentDashboardScreen: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const theme = useTheme();
  const { t } = useI18n();
  const navigation = useNavigation<NavigationProp>();
  const { data: children, isLoading: isChildrenLoading, error: childrenError } = useChildren();

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {/* Welcome Header */}
        <View style={styles.header}>
          <Text variant="headlineMedium" style={{ color: theme.colors.onBackground }}>
            {t('dashboard.welcome')}
          </Text>
          <Text
            variant="headlineMedium"
            style={[styles.userName, { color: theme.colors.primary }]}
          >
            {user?.firstName || t('role.parent')}! 👋
          </Text>
          <Text
            variant="bodyMedium"
            style={[styles.subGreeting, { color: theme.colors.onSurfaceVariant }]}
          >
            {t('dashboard.subtitle')}
          </Text>
        </View>

        {/* Quick Actions */}
        <View style={styles.section}>
          <Text variant="titleLarge" style={[styles.sectionTitle, { color: theme.colors.onBackground }]}>
            {t('dashboard.quick.actions')}
          </Text>

          <QuickActionCard
            title={t('dashboard.approve.tasks')}
            icon="clipboard-check"
            count={3}
            color={theme.colors.tertiary}
            onPress={() => navigation.navigate('Tasks', { screen: 'PendingApproval' })}
          />

          <QuickActionCard
            title={t('dashboard.approve.rewards')}
            icon="gift"
            count={1}
            color={theme.colors.secondary}
            onPress={() => navigation.navigate('Shop', { screen: 'PendingRedemptions' })}
          />

          <QuickActionCard
            title={t('dashboard.evaluate.behavior')}
            icon="star-circle"
            count={2}
            color={theme.colors.primary}
            onPress={() => navigation.navigate('Behavior', { screen: 'BehaviorManage' })}
          />
        </View>

        <Divider style={styles.divider} />

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
                {children.map((child, idx) => (
                  <React.Fragment key={child.id}>
                    <List.Item
                      title={child.firstName}
                      description={`${child.totalPoints} ${t('points.label')}`}
                      left={() => (
                        <Avatar.Text
                          size={40}
                          label={(child.firstName?.charAt(0) || '?').toUpperCase()}
                          style={{ backgroundColor: theme.colors.primaryContainer }}
                          color={theme.colors.onPrimaryContainer}
                        />
                      )}
                    />
                    {idx < children.length - 1 && <Divider />}
                  </React.Fragment>
                ))}
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
            Letzte Aktivitäten
          </Text>

          <Card elevation={1}>
            <EmptyState
              icon="history"
              title="Keine Aktivitäten"
              message="Hier erscheinen die neuesten Aktivitäten deiner Familie"
            />
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
  header: {
    marginBottom: spacing.lg,
    paddingTop: spacing.sm,
  },
  userName: {
    fontWeight: '700',
  },
  subGreeting: {
    marginTop: spacing.xs,
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
});
