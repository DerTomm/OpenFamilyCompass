import { MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Divider, ProgressBar, Surface, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Card, EmptyState } from '../../components/ui';
import { useI18n } from '../../i18n/I18nContext';
import { useAuthStore } from '../../store/authStore';
import { spacing } from '../../theme/theme';

export const ChildDashboardScreen: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const theme = useTheme();
  const { t } = useI18n();

  // Mock data - wird später durch echte Daten ersetzt
  const totalPoints = user?.totalPoints || 0;
  const nextRewardPoints = 100;
  const progress = totalPoints / nextRewardPoints;

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {/* Welcome Header */}
        <View style={styles.header}>
          <Text variant="headlineMedium" style={{ color: theme.colors.onBackground }}>
            {t('child.dashboard.welcome')}
          </Text>
          <Text
            variant="headlineMedium"
            style={[styles.userName, { color: theme.colors.primary }]}
          >
            {user?.firstName || t('role.child')}! 👋
          </Text>
        </View>

        {/* Points Card */}
        <Surface style={styles.pointsCard} elevation={3}>
          <LinearGradient
            colors={[theme.colors.primary, theme.colors.secondary]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientCard}
          >
            <View style={styles.pointsHeader}>
              <MaterialCommunityIcons name="star-circle" size={32} color="#FFF" />
              <Text variant="titleMedium" style={styles.pointsLabel}>
                {t('child.dashboard.points.badge')}
              </Text>
            </View>

            <Text variant="displayMedium" style={styles.pointsValue}>
              {totalPoints}
            </Text>

            {/* Progress to next reward */}
            <View style={styles.progressSection}>
              <View style={styles.progressHeader}>
                <MaterialCommunityIcons name="gift" size={16} color="rgba(255,255,255,0.9)" />
                <Text variant="bodySmall" style={styles.progressText}>
                  {t('points.progress.next_reward', { 0: nextRewardPoints - totalPoints })}
                </Text>
              </View>
              <ProgressBar
                progress={progress}
                color="rgba(255,255,255,0.9)"
                style={styles.progressBar}
              />
            </View>
          </LinearGradient>
        </Surface>

        <Divider style={styles.divider} />

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
            <EmptyState
              icon="timeline-clock"
              title="Noch keine Aktivitäten"
              message="Hier erscheinen deine abgeschlossenen Aufgaben und Belohnungen"
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
    marginBottom: spacing.sm,
  },
  pointsLabel: {
    color: 'rgba(255, 255, 255, 0.9)',
    marginLeft: spacing.sm,
    fontWeight: '600',
  },
  pointsValue: {
    color: '#FFF',
    fontWeight: '900',
    marginBottom: spacing.lg,
  },
  progressSection: {
    marginTop: spacing.md,
  },
  progressHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  progressText: {
    color: 'rgba(255, 255, 255, 0.9)',
    marginLeft: spacing.xs,
  },
  progressBar: {
    height: 8,
    borderRadius: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.3)',
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
});
