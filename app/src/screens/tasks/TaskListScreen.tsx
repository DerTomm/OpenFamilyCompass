import React, { useState } from 'react';
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
import { useCompleteTask, useTaskInstances } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { selectIsChild, useAuthStore } from '../../store/authStore';
import { TaskInstanceResponse, TaskStatus } from '../../types/api';

const STATUS_COLORS: Record<TaskStatus, string> = {
  PENDING: '#FF9800',
  IN_PROGRESS: '#2196F3',
  CHILD_COMPLETED: '#9C27B0',
  APPROVED: '#4CAF50',
  REJECTED: '#F44336',
  EXPIRED: '#9E9E9E',
};

const getStatusLabel = (status: TaskStatus, t: (key: string) => string): string => {
  return t(`task.status.${status}`);
};

interface TaskCardProps {
  task: TaskInstanceResponse;
  onComplete: () => void;
  isCompleting: boolean;
  isChild: boolean;
  t: (key: string) => string;
}

const TaskCard: React.FC<TaskCardProps> = ({ task, onComplete, isCompleting, isChild, t }) => {
  const canComplete = isChild && (task.status === 'PENDING' || task.status === 'IN_PROGRESS');

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.taskTitle}>{task.taskDefinition.title}</Text>
        <View style={[styles.statusBadge, { backgroundColor: STATUS_COLORS[task.status] }]}>
          <Text style={styles.statusText}>{getStatusLabel(task.status, t)}</Text>
        </View>
      </View>

      {task.taskDefinition.description && (
        <Text style={styles.description}>{task.taskDefinition.description}</Text>
      )}

      <View style={styles.cardFooter}>
        <View style={styles.pointsContainer}>
          <Text style={styles.pointsLabel}>{t('tasks.points')}:</Text>
          <Text style={styles.pointsValue}>
            {task.awardedPoints ?? task.taskDefinition.basePoints}
          </Text>
        </View>

        {task.dueDate && (
          <Text style={styles.dueDate}>
            {new Date(task.dueDate).toLocaleDateString()}
          </Text>
        )}
      </View>

      {canComplete && (
        <TouchableOpacity
          style={styles.completeButton}
          onPress={onComplete}
          disabled={isCompleting}
        >
          {isCompleting ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <Text style={styles.completeButtonText}>{t('tasks.complete')}</Text>
          )}
        </TouchableOpacity>
      )}
    </View>
  );
};

type FilterStatus = 'all' | 'active' | 'completed';

export const TaskListScreen: React.FC = () => {
  const [filter, setFilter] = useState<FilterStatus>('active');
  const user = useAuthStore((state) => state.user);
  const isChild = useAuthStore(selectIsChild);
  const { t } = useI18n();

  const { data: tasks, isLoading, refetch, isRefetching } = useTaskInstances(
    isChild ? { assignedUserId: user?.id } : undefined
  );

  const completeTask = useCompleteTask();

  const filteredTasks = tasks?.filter((task) => {
    if (filter === 'active') {
      return ['PENDING', 'IN_PROGRESS', 'CHILD_COMPLETED'].includes(task.status);
    }
    if (filter === 'completed') {
      return ['APPROVED', 'REJECTED', 'EXPIRED'].includes(task.status);
    }
    return true;
  });

  const handleComplete = (taskId: number) => {
    completeTask.mutate(taskId);
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.filterContainer}>
        {(['active', 'completed', 'all'] as FilterStatus[]).map((f) => (
          <TouchableOpacity
            key={f}
            style={[styles.filterButton, filter === f && styles.filterButtonActive]}
            onPress={() => setFilter(f)}
          >
            <Text style={[styles.filterText, filter === f && styles.filterTextActive]}>
              {f === 'active' ? t('status.active') : f === 'completed' ? t('tasks.completed') : t('children.all')}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : (
        <FlatList
          data={filteredTasks}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <TaskCard
              task={item}
              onComplete={() => handleComplete(item.id)}
              isCompleting={completeTask.isPending && completeTask.variables === item.id}
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
              <Text style={styles.emptyText}>{t('empty.no_tasks')}</Text>
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
  filterContainer: {
    flexDirection: 'row',
    padding: 12,
    gap: 8,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  filterButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    backgroundColor: '#f0f0f0',
  },
  filterButtonActive: {
    backgroundColor: '#2196F3',
  },
  filterText: {
    color: '#666',
    fontWeight: '500',
  },
  filterTextActive: {
    color: '#fff',
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
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  taskTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    flex: 1,
    marginRight: 8,
  },
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  description: {
    color: '#666',
    marginBottom: 12,
    lineHeight: 20,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  pointsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  pointsLabel: {
    color: '#666',
    marginRight: 4,
  },
  pointsValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#4CAF50',
  },
  dueDate: {
    color: '#999',
    fontSize: 13,
  },
  completeButton: {
    backgroundColor: '#4CAF50',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 12,
  },
  completeButtonText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
  },
  emptyContainer: {
    padding: 32,
    alignItems: 'center',
  },
  emptyText: {
    color: '#999',
    fontSize: 16,
  },
});
