import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { QuickActionCard } from '../../components/ui';
import { useI18n } from '../../i18n/I18nContext';
import { selectIsAdmin, useAuthStore } from '../../store/authStore';
import { ManageStackParamList } from '../../navigation/types';
import { spacing } from '../../theme/theme';

type NavigationProp = NativeStackNavigationProp<ManageStackParamList>;

export const ManageHomeScreen: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  const navigation = useNavigation<NavigationProp>();
  const isAdmin = useAuthStore(selectIsAdmin);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text variant="headlineSmall" style={{ color: theme.colors.onBackground }}>
            {t('nav.manage')}
          </Text>
          <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
            {t('manage.subtitle')}
          </Text>
        </View>

        <QuickActionCard
          title={t('tasks.title')}
          icon="clipboard-list"
          onPress={() => navigation.navigate('TaskManagement')}
          color={theme.colors.primary}
        />

        <QuickActionCard
          title={t('rewards.title')}
          icon="gift"
          onPress={() => navigation.navigate('RewardList')}
          color={theme.colors.secondary}
        />

        <QuickActionCard
          title={t('behavior.manage.title')}
          icon="star-circle"
          onPress={() => navigation.navigate('BehaviorManage')}
          color={theme.colors.tertiary}
        />

        {isAdmin && (
          <QuickActionCard
            title={t('admin.user.management')}
            icon="shield-account"
            onPress={() => navigation.navigate('AdminUserList')}
            color={theme.colors.error}
          />
        )}
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
    gap: spacing.xs,
  },
});
