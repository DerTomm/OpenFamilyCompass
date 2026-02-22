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
import { useNavigation } from '@react-navigation/native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useChildren } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { ChildResponse } from '../../types/api';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { ActivitiesStackParamList } from '../../navigation/types';

type NavigationProp = NativeStackNavigationProp<ActivitiesStackParamList>;

interface ChildCardProps {
  child: ChildResponse;
  onPress: () => void;
  t: (key: string) => string;
  styles: any;
}

const ChildCard: React.FC<ChildCardProps> = ({ child, onPress, t, styles }) => (
  <TouchableOpacity style={styles.card} onPress={onPress}>
    <View style={styles.avatarContainer}>
      <Text style={styles.avatarText}>{child.firstName.charAt(0)}</Text>
    </View>
    <View style={styles.infoContainer}>
      <Text style={styles.name}>{child.firstName}</Text>
      <Text style={styles.points}>
        {child.totalPoints} {t('points.label')}
      </Text>
    </View>
    <MaterialCommunityIcons name="chevron-right" size={24} color="#ccc" />
  </TouchableOpacity>
);

export const BehaviorOverviewScreen: React.FC = () => {
  const { t } = useI18n();
  const navigation = useNavigation<NavigationProp>();
  const { data: children, isLoading, refetch, isRefetching } = useChildren();
  const theme = useTheme();
  const styles = createStyles(theme);

  const handleChildSelect = (child: ChildResponse) => {
    navigation.navigate('BehaviorEvaluate', { childId: child.id });
  };

  const handleManage = () => {
    // Stammdaten-Verwaltung ist im "Verwalten"-Bereich
    const parent = navigation.getParent<any>();
    parent?.navigate('Manage', { screen: 'BehaviorManage' });
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.title}>
            <MaterialCommunityIcons name="star" size={24} color="#ffc107" /> {t('behavior.evaluate.title')}
          </Text>
          <Text style={styles.subtitle}>{t('behavior.subtitle')}</Text>
        </View>
      </View>

      <TouchableOpacity style={styles.manageButton} onPress={handleManage}>
        <MaterialCommunityIcons name="cog" size={20} color="#2196F3" />
        <Text style={styles.manageButtonText}>{t('behavior.manage.title')}</Text>
      </TouchableOpacity>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#ffc107" />
        </View>
      ) : (
        <FlatList
          data={children}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <ChildCard
              child={item}
              onPress={() => handleChildSelect(item)}
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
              <MaterialCommunityIcons name="account-group" size={64} color="#ccc" />
              <Text style={styles.emptyText}>{t('admin.users.list.empty')}</Text>
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
    backgroundColor: theme.colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  headerContent: {
    gap: 4,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: theme.colors.onSurface,
  },
  subtitle: {
    fontSize: 14,
    color: theme.colors.onSurfaceVariant,
  },
  manageButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: theme.colors.surface,
    margin: 16,
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  manageButtonText: {
    color: '#2196F3',
    fontSize: 16,
    fontWeight: '600',
  },
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  listContent: {
    padding: 16,
    gap: 12,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.surface,
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  avatarContainer: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#ffc107',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  avatarText: {
    color: '#fff',
    fontSize: 24,
    fontWeight: 'bold',
  },
  infoContainer: {
    flex: 1,
  },
  name: {
    fontSize: 18,
    fontWeight: '600',
    color: theme.colors.onSurface,
  },
  points: {
    fontSize: 14,
    color: theme.colors.onSurfaceVariant,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
    marginTop: 32,
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    marginTop: 16,
  },
});
