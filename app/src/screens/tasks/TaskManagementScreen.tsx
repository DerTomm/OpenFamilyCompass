import React, { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View,
  useWindowDimensions,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { Button, Chip, DataTable, IconButton, Searchbar, Text, useTheme } from 'react-native-paper';
import { useTaskDefinitions, useChildren, useDeleteTaskDefinition } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { TaskDefinitionResponse } from '../../types/api';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { ManageStackParamList } from '../../navigation/types';
import { useDialogs } from '../../hooks/useDialogs';
import { UserAvatar } from '../../components/ui';

type NavigationProp = NativeStackNavigationProp<ManageStackParamList>;

export const TaskManagementScreen: React.FC = () => {
  const { t } = useI18n();
  const navigation = useNavigation<NavigationProp>();
  const { width } = useWindowDimensions();
  const isDesktop = width >= 768;
  const { showConfirm, Dialogs } = useDialogs();
  const theme = useTheme();
  const styles = createStyles(theme);

  const { data: tasks, isLoading, refetch, isRefetching } = useTaskDefinitions();
  const { data: children } = useChildren();
  const deleteMutation = useDeleteTaskDefinition();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedChildId, setSelectedChildId] = useState<number | null>(null);

  const filteredTasks = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();
    return (tasks ?? []).filter((task) => {
      const title = task.title.toLowerCase();
      const description = task.description?.toLowerCase() ?? '';
      const matchesSearch =
        normalizedQuery.length === 0 ||
        title.includes(normalizedQuery) ||
        description.includes(normalizedQuery);
      const matchesChild = selectedChildId
        ? (task.assignedUsers ?? []).some((user) => user.id === selectedChildId)
        : true;

      return matchesSearch && matchesChild;
    });
  }, [tasks, searchQuery, selectedChildId]);

  const handleCreate = () => {
    navigation.navigate('TaskEdit', { taskId: undefined });
  };

  const handleEdit = (task: TaskDefinitionResponse) => {
    navigation.navigate('TaskEdit', { taskId: task.id });
  };

  const handleDelete = (task: TaskDefinitionResponse) => {
    showConfirm({
      title: t('tasks.delete.title'),
      message: t('tasks.delete.confirm'),
      onConfirm: () => deleteMutation.mutate(task.id),
      confirmText: t('button.delete'),
      cancelText: t('button.cancel'),
      destructive: true,
    });
  };

  const columnFlex = {
    title: 2,
    recurrence: 1,
    points: 1,
    assigned: 2,
    actions: 1,
  };

  const totalFlex =
    columnFlex.title +
    columnFlex.recurrence +
    columnFlex.points +
    columnFlex.assigned +
    columnFlex.actions;

  const renderRow = ({ item }: { item: TaskDefinitionResponse }) => {
    const assignedNames =
      item.assignedUsers && item.assignedUsers.length > 0
        ? item.assignedUsers.map((user) => user.firstName).join(', ')
        : '-';

    return (
      <DataTable.Row>
        <DataTable.Cell style={{ flex: columnFlex.title }}>
          <View>
            <Text variant="bodyMedium" style={styles.cellTitle}>
              {item.title}
            </Text>
            {item.description ? (
              <Text variant="bodySmall" style={styles.cellSubtitle} numberOfLines={1}>
                {item.description}
              </Text>
            ) : null}
          </View>
        </DataTable.Cell>
        <DataTable.Cell style={{ flex: columnFlex.recurrence }}>
          {t(`recurrence.${item.recurrenceType}`)}
        </DataTable.Cell>
        <DataTable.Cell numeric style={{ flex: columnFlex.points }}>
          {item.basePoints}
        </DataTable.Cell>
        <DataTable.Cell style={{ flex: columnFlex.assigned }}>{assignedNames}</DataTable.Cell>
        <DataTable.Cell numeric style={{ flex: columnFlex.actions }}>
          <View style={styles.actionButtons}>
            <IconButton
              icon="pencil"
              size={18}
              onPress={() => handleEdit(item)}
              disabled={deleteMutation.isPending}
            />
            <IconButton
              icon="delete"
              size={18}
              iconColor="#F44336"
              onPress={() => handleDelete(item)}
              disabled={deleteMutation.isPending}
            />
          </View>
        </DataTable.Cell>
      </DataTable.Row>
    );
  };

  const renderEmpty = () => (
    <DataTable.Row>
      <DataTable.Cell style={{ flex: totalFlex }}>
        <Text style={styles.emptyText}>{t('tasks.definitions.empty')}</Text>
      </DataTable.Cell>
    </DataTable.Row>
  );

  const table = (
    <DataTable style={[styles.table, !isDesktop && styles.tableMobile]}>
      <DataTable.Header>
        <DataTable.Title style={{ flex: columnFlex.title }}>{t('tasks.name')}</DataTable.Title>
        <DataTable.Title style={{ flex: columnFlex.recurrence }}>{t('tasks.recurrence')}</DataTable.Title>
        <DataTable.Title numeric style={{ flex: columnFlex.points }}>{t('tasks.points')}</DataTable.Title>
        <DataTable.Title style={{ flex: columnFlex.assigned }}>{t('tasks.assigned_to')}</DataTable.Title>
        <DataTable.Title numeric style={{ flex: columnFlex.actions }}>{t('tasks.actions')}</DataTable.Title>
      </DataTable.Header>
      <FlatList
        data={filteredTasks}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderRow}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} />}
        ListEmptyComponent={renderEmpty}
      />
    </DataTable>
  );

  return (
    <View style={styles.container}>
      <View style={styles.filterSection}>
        <View style={[styles.toolbar, !isDesktop && styles.toolbarMobile]}>
          <Searchbar
            placeholder={t('tasks.search')}
            onChangeText={setSearchQuery}
            value={searchQuery}
            style={styles.searchBar}
          />
          <Button
            mode="contained"
            icon="plus"
            onPress={handleCreate}
            style={[styles.createButton, !isDesktop && styles.createButtonMobile]}
            contentStyle={styles.createButtonContent}
          >
            {t('tasks.create')}
          </Button>
        </View>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.chipScroll}
        >
          <Chip
            selected={selectedChildId === null}
            onPress={() => setSelectedChildId(null)}
            style={styles.filterChip}
            showSelectedOverlay
          >
            {t('tasks.filter.all')}
          </Chip>
          {children?.map((child) => (
            <Chip
              key={child.id}
              selected={selectedChildId === child.id}
              onPress={() => setSelectedChildId(child.id)}
              style={styles.filterChip}
              showSelectedOverlay

            >
              {child.firstName}
            </Chip>
          ))}
        </ScrollView>
      </View>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : isDesktop ? (
        <View style={styles.tableContainer}>{table}</View>
      ) : (
        <ScrollView horizontal showsHorizontalScrollIndicator contentContainerStyle={styles.tableScroll}>
          <View style={styles.tableContainer}>{table}</View>
        </ScrollView>
      )}

      <Dialogs />
    </View>
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
  filterSection: {
    backgroundColor: theme.colors.surface,
    padding: 16,
    gap: 12,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.surfaceVariant,
  },
  toolbar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    flexWrap: 'wrap',
  },
  toolbarMobile: {
    alignItems: 'stretch',
  },
  searchBar: {
    elevation: 0,
    backgroundColor: theme.colors.surfaceVariant,
    height: 40,
    flex: 1,
    minWidth: 220,
  },
  createButton: {
    borderRadius: 8,
  },
  createButtonMobile: {
    alignSelf: 'stretch',
  },
  createButtonContent: {
    height: 40,
    paddingHorizontal: 12,
  },
  chipScroll: {
    gap: 8,
    alignItems: 'center',
    paddingVertical: 4,
  },
  filterChip: {
    marginRight: 8,
  },
  tableContainer: {
    flex: 1,
    backgroundColor: theme.colors.surface,
    margin: 16,
    borderRadius: 8,
    elevation: 2,
    overflow: 'hidden',
  },
  table: {
    backgroundColor: theme.colors.surface,
  },
  tableMobile: {
    minWidth: 900,
  },
  tableScroll: {
    paddingRight: 16,
  },
  cellTitle: {
    fontWeight: '600',
  },
  cellSubtitle: {
    color: '#666',
  },
  actionButtons: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  emptyText: {
    color: '#999',
    paddingVertical: 8,
  },
});
