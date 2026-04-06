import React, { useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useApproveTask, useRejectTask, useTaskInstances } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { TaskInstanceResponse } from '../../types/api';

interface ApprovalModalProps {
  visible: boolean;
  task: TaskInstanceResponse | null;
  onClose: () => void;
  onApprove: (points: number, notes: string) => void;
  onReject: (notes: string) => void;
  isLoading: boolean;
  t: (key: string) => string;
  styles: any;
}

const ApprovalModal: React.FC<ApprovalModalProps> = ({
  visible,
  task,
  onClose,
  onApprove,
  onReject,
  isLoading,
  t,
  styles,
}) => {
  const [points, setPoints] = useState(task?.taskDefinition.basePoints.toString() || '0');
  const [notes, setNotes] = useState('');

  React.useEffect(() => {
    if (task) {
      setPoints(task.taskDefinition.basePoints.toString());
      setNotes('');
    }
  }, [task]);

  return (
    <Modal visible={visible} transparent animationType="slide">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>{t('tasks.pending.title')}</Text>
          <Text style={styles.modalTaskTitle}>{task?.taskDefinition.title}</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('tasks.pending.award.points')}</Text>
            <TextInput
              style={styles.input}
              value={points}
              onChangeText={setPoints}
              keyboardType="numeric"
              placeholder={t('tasks.points')}
              placeholderTextColor={styles.placeholder.color}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('tasks.pending.parent.comment')}</Text>
            <TextInput
              style={[styles.input, styles.textArea]}
              value={notes}
              onChangeText={setNotes}
              placeholder={t('behavior.modal.notes.placeholder')}
              placeholderTextColor={styles.placeholder.color}
              multiline
              numberOfLines={3}
            />
          </View>

          <View style={styles.modalButtons}>
            <TouchableOpacity
              style={[styles.modalButton, styles.rejectButton]}
              onPress={() => onReject(notes)}
              disabled={isLoading}
            >
              <Text style={styles.rejectButtonText}>{t('tasks.pending.reject')}</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.modalButton, styles.approveButton]}
              onPress={() => onApprove(parseInt(points, 10), notes)}
              disabled={isLoading}
            >
              {isLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.approveButtonText}>{t('tasks.pending.approve')}</Text>
              )}
            </TouchableOpacity>
          </View>

          <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
            <Text style={styles.cancelButtonText}>{t('button.cancel')}</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

export const PendingApprovalScreen: React.FC = () => {
  const [filter, setFilter] = useState<'active' | 'completed'>('active');
  const { data: tasks, isLoading, refetch, isRefetching } = useTaskInstances();
  const approveTask = useApproveTask();
  const rejectTask = useRejectTask();
  const { t } = useI18n();
  const theme = useTheme();
  const styles = createStyles(theme);

  const [selectedTask, setSelectedTask] = useState<TaskInstanceResponse | null>(null);

  const filteredTasks = tasks?.filter((task) => {
    if (filter === 'active') {
      return ['PENDING', 'IN_PROGRESS', 'CHILD_COMPLETED'].includes(task.status);
    }
    if (filter === 'completed') {
      return ['APPROVED', 'REJECTED', 'EXPIRED'].includes(task.status);
    }
    return true;
  });

  const handleApprove = (points: number, notes: string) => {
    if (selectedTask) {
      approveTask.mutate(
        { taskId: selectedTask.id, data: { awardedPoints: points, notes } },
        { onSuccess: () => setSelectedTask(null) }
      );
    }
  };

  const handleReject = (notes: string) => {
    if (selectedTask) {
      rejectTask.mutate(
        { taskId: selectedTask.id, notes },
        { onSuccess: () => setSelectedTask(null) }
      );
    }
  };

  const renderTask = ({ item }: { item: TaskInstanceResponse }) => {
    const isCompleted = ['APPROVED', 'REJECTED', 'EXPIRED'].includes(item.status);
    const canApprove = item.status === 'CHILD_COMPLETED';

    return (
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.childName}>{item.assignedUser.firstName}</Text>
          <Text style={styles.completedDate}>
            {item.completedAt && new Date(item.completedAt).toLocaleDateString()}
          </Text>
        </View>
        <Text style={styles.taskTitle}>{item.taskDefinition.title}</Text>
        <View style={styles.cardFooter}>
          <View>
            <Text style={styles.basePoints}>
              {t('tasks.base')}: {item.taskDefinition.basePoints} {t('points.label')}
            </Text>
            {item.awardedPoints !== undefined && (
              <Text style={styles.awardedPoints}>
                {t('tasks.pending.award.points')}: {item.awardedPoints} {t('points.label')}
              </Text>
            )}
          </View>
          {canApprove ? (
            <TouchableOpacity onPress={() => setSelectedTask(item)}>
              <Text style={styles.tapToReview}>{t('tasks.pending.tap.review')} →</Text>
            </TouchableOpacity>
          ) : (
            <Text style={styles.statusText}>
              {t(`task.status.${item.status}`)}
            </Text>
          )}
        </View>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.filterContainer}>
        {(['active', 'completed'] as const).map((f) => (
          <TouchableOpacity
            key={f}
            style={[styles.filterButton, filter === f && styles.filterButtonActive]}
            onPress={() => setFilter(f)}
          >
            <Text style={[styles.filterText, filter === f && styles.filterTextActive]}>
              {f === 'active' ? t('status.active') : t('tasks.completed')}
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
          renderItem={renderTask}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>✅</Text>
              <Text style={styles.emptyText}>
                {filter === 'active' ? t('tasks.pending.empty.title') : 'Keine abgeschlossenen Aufgaben'}
              </Text>
              <Text style={styles.emptySubtext}>
                {filter === 'active' ? t('tasks.pending.empty.subtitle') : 'Hier erscheinen genehmigte oder abgelehnte Aufgaben'}
              </Text>
            </View>
          }
        />
      )}

      <ApprovalModal
        visible={!!selectedTask && selectedTask.status === 'CHILD_COMPLETED'}
        task={selectedTask}
        onClose={() => setSelectedTask(null)}
        onApprove={handleApprove}
        onReject={handleReject}
        isLoading={approveTask.isPending || rejectTask.isPending}
        t={t}
        styles={styles}
      />
    </SafeAreaView>
  );
};

const createStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
  },
  filterContainer: {
    flexDirection: 'row',
    padding: 12,
    gap: 8,
    backgroundColor: theme.colors.surface,
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
    color: theme.colors.onSurfaceVariant,
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
    flexGrow: 1,
  },
  card: {
    backgroundColor: theme.colors.surface,
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#9C27B0',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  childName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#9C27B0',
  },
  completedDate: {
    fontSize: 13,
    color: '#999',
  },
  taskTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: theme.colors.onSurface,
    marginBottom: 8,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  basePoints: {
    color: theme.colors.onSurfaceVariant,
  },
  tapToReview: {
    color: '#2196F3',
    fontWeight: '500',
  },
  awardedPoints: {
    color: theme.colors.onSurfaceVariant,
    fontSize: 12,
    marginTop: 2,
  },
  statusText: {
    color: theme.colors.onSurfaceVariant,
    fontWeight: '500',
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
    color: theme.colors.onSurface,
    marginBottom: 4,
  },
  emptySubtext: {
    color: theme.colors.onSurfaceVariant,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: theme.colors.surface,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: theme.colors.onSurface,
    marginBottom: 4,
  },
  modalTaskTitle: {
    fontSize: 16,
    color: theme.colors.onSurfaceVariant,
    marginBottom: 20,
  },
  inputGroup: {
    marginBottom: 16,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: theme.colors.onSurface,
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: theme.colors.outline,
    backgroundColor: theme.colors.surface,
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    color: theme.colors.onSurface,
  },
  textArea: {
    height: 80,
    textAlignVertical: 'top',
  },
  modalButtons: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 12,
  },
  modalButton: {
    flex: 1,
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  approveButton: {
    backgroundColor: '#4CAF50',
  },
  approveButtonText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
  },
  rejectButton: {
    backgroundColor: theme.colors.surface,
    borderWidth: 2,
    borderColor: '#F44336',
  },
  rejectButtonText: {
    color: '#F44336',
    fontWeight: '600',
    fontSize: 16,
  },
  cancelButton: {
    padding: 12,
    alignItems: 'center',
  },
  cancelButtonText: {
    color: theme.colors.onSurfaceVariant,
    fontSize: 16,
  },
  placeholder: {
    color: theme.colors.onSurfaceVariant,
  },
});
