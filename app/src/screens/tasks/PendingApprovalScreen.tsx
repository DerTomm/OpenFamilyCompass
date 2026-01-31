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
import { SafeAreaView } from 'react-native-safe-area-context';
import { useApproveTask, usePendingTasks, useRejectTask } from '../../hooks/useApi';
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
}

const ApprovalModal: React.FC<ApprovalModalProps> = ({
  visible,
  task,
  onClose,
  onApprove,
  onReject,
  isLoading,
  t,
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
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('tasks.pending.parent.comment')}</Text>
            <TextInput
              style={[styles.input, styles.textArea]}
              value={notes}
              onChangeText={setNotes}
              placeholder={t('behavior.modal.notes.placeholder')}
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
  const { data: tasks, isLoading, refetch, isRefetching } = usePendingTasks();
  const approveTask = useApproveTask();
  const rejectTask = useRejectTask();
  const { t } = useI18n();

  const [selectedTask, setSelectedTask] = useState<TaskInstanceResponse | null>(null);

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

  const renderTask = ({ item }: { item: TaskInstanceResponse }) => (
    <TouchableOpacity style={styles.card} onPress={() => setSelectedTask(item)}>
      <View style={styles.cardHeader}>
        <Text style={styles.childName}>{item.assignedUser.firstName}</Text>
        <Text style={styles.completedDate}>
          {item.completedAt && new Date(item.completedAt).toLocaleDateString()}
        </Text>
      </View>
      <Text style={styles.taskTitle}>{item.taskDefinition.title}</Text>
      <View style={styles.cardFooter}>
        <Text style={styles.basePoints}>
          {t('tasks.base')}: {item.taskDefinition.basePoints} {t('points.label')}
        </Text>
        <Text style={styles.tapToReview}>{t('tasks.pending.tap.review')} →</Text>
      </View>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : (
        <FlatList
          data={tasks}
          keyExtractor={(item) => item.id.toString()}
          renderItem={renderTask}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>✅</Text>
              <Text style={styles.emptyText}>{t('tasks.pending.empty.title')}</Text>
              <Text style={styles.emptySubtext}>{t('tasks.pending.empty.subtitle')}</Text>
            </View>
          }
        />
      )}

      <ApprovalModal
        visible={!!selectedTask}
        task={selectedTask}
        onClose={() => setSelectedTask(null)}
        onApprove={handleApprove}
        onReject={handleReject}
        isLoading={approveTask.isPending || rejectTask.isPending}
        t={t}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
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
    backgroundColor: '#fff',
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
    color: '#333',
    marginBottom: 8,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  basePoints: {
    color: '#666',
  },
  tapToReview: {
    color: '#2196F3',
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
    color: '#333',
    marginBottom: 4,
  },
  emptySubtext: {
    color: '#666',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  modalTaskTitle: {
    fontSize: 16,
    color: '#666',
    marginBottom: 20,
  },
  inputGroup: {
    marginBottom: 16,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
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
    backgroundColor: '#fff',
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
    color: '#666',
    fontSize: 16,
  },
});
