import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Modal,
  Platform,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  useWindowDimensions,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { MaterialIcons } from '@expo/vector-icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Slider from '@react-native-community/slider';
import { behaviorsApi, evaluationsApi, usersApi } from '../../api/services';
import { useI18n } from '../../i18n/I18nContext';
import { BehaviorResponse, BehaviorEvaluationResponse, ChildResponse } from '../../types/api';

interface EvaluationModalProps {
  visible: boolean;
  behavior: BehaviorResponse | null;
  existingEvaluation: BehaviorEvaluationResponse | null;
  childId: number;
  onClose: () => void;
  onSave: (data: { behaviorId: number; userId: number; currentPoints: number; remarks?: string }) => void;
  isLoading: boolean;
  t: (key: string) => string;
}

const EvaluationModal: React.FC<EvaluationModalProps> = ({
  visible,
  behavior,
  existingEvaluation,
  childId,
  onClose,
  onSave,
  isLoading,
  t,
}) => {
  const [points, setPoints] = useState(0);
  const [remarks, setRemarks] = useState('');

  React.useEffect(() => {
    if (behavior) {
      if (existingEvaluation) {
        setPoints(existingEvaluation.currentPoints);
        setRemarks(existingEvaluation.remarks || '');
      } else {
        setPoints(behavior.points);
        setRemarks('');
      }
    }
  }, [behavior, existingEvaluation, visible]);

  const handleSave = () => {
    if (!behavior) return;
    onSave({
      behaviorId: behavior.id,
      userId: childId,
      currentPoints: points,
      remarks: remarks || undefined,
    });
  };

  if (!behavior) return null;

  const progressPercentage = (points / behavior.points) * 100;
  const getProgressColor = () => {
    if (progressPercentage < 20) return '#dc3545';
    if (progressPercentage < 80) return '#ffc107';
    return '#198754';
  };

  return (
    <Modal visible={visible} transparent animationType="slide">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>{t('behavior.evaluate.rule')}</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <MaterialIcons name="close" size={24} color="#666" />
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalBody}>
            <Text style={styles.behaviorName}>{behavior.title}</Text>

            <View style={styles.sliderContainer}>
              <Text style={styles.sliderLabel}>
                {t('tasks.points')}: <Text style={styles.sliderValue}>{points}</Text> / {behavior.points}
              </Text>
              <Slider
                style={styles.slider}
                minimumValue={0}
                maximumValue={behavior.points}
                step={1}
                value={points}
                onValueChange={setPoints}
                minimumTrackTintColor={getProgressColor()}
                maximumTrackTintColor="#e0e0e0"
                thumbTintColor={getProgressColor()}
              />
              <View style={styles.progressBarPreview}>
                <View
                  style={[
                    styles.progressBarFill,
                    { width: `${progressPercentage}%`, backgroundColor: getProgressColor() },
                  ]}
                />
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>{t('behavior.modal.notes')} ({t('admin.users.edit.password_optional')})</Text>
              <TextInput
                style={[styles.input, styles.textArea]}
                value={remarks}
                onChangeText={setRemarks}
                placeholder={t('behavior.modal.notes.placeholder')}
                multiline
                numberOfLines={5}
              />
            </View>
          </ScrollView>

          <View style={styles.modalFooter}>
            <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
              <Text style={styles.cancelButtonText}>{t('button.cancel')}</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.submitButton} onPress={handleSave} disabled={isLoading}>
              {isLoading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.submitButtonText}>{t('button.save')}</Text>
              )}
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

interface BehaviorEvalRowProps {
  behavior: BehaviorResponse;
  evaluation: BehaviorEvaluationResponse | undefined;
  isDesktop: boolean;
  onEvaluate: () => void;
  onShowGuideline: () => void;
  t: (key: string) => string;
}

const BehaviorEvalRow: React.FC<BehaviorEvalRowProps> = ({
  behavior,
  evaluation,
  isDesktop,
  onEvaluate,
  onShowGuideline,
  t,
}) => {
  const currentPoints = evaluation?.currentPoints ?? behavior.points;
  const progressPercentage = (currentPoints / behavior.points) * 100;

  const getProgressColor = () => {
    if (progressPercentage < 20) return '#dc3545';
    if (progressPercentage < 80) return '#ffc107';
    return '#198754';
  };

  if (isDesktop) {
    return (
      <View style={styles.tableRow}>
        <View style={[styles.tableCell, { flex: 2 }]}>
          <Text style={styles.behaviorTitle}>{behavior.title}</Text>
        </View>
        <View style={[styles.tableCell, { flex: 1, alignItems: 'center' }]}>
          <View style={styles.progressContainer}>
            <View style={styles.progressBar}>
              <View
                style={[styles.progressFill, { width: `${progressPercentage}%`, backgroundColor: getProgressColor() }]}
              />
              <Text style={styles.progressText}>
                {currentPoints} / {behavior.points}
              </Text>
            </View>
          </View>
        </View>
        <View style={[styles.tableCell, { width: 180, flexDirection: 'row', gap: 8, justifyContent: 'center' }]}>
          <TouchableOpacity style={styles.iconButton} onPress={onShowGuideline}>
            <MaterialIcons name="info-outline" size={20} color="#2196F3" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.evaluateButton} onPress={onEvaluate}>
            <MaterialIcons name="edit" size={18} color="#fff" />
            <Text style={styles.evaluateButtonText}>{t('behavior.evaluate.rule')}</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>{behavior.title}</Text>
      </View>

      <View style={styles.cardContent}>
        <Text style={styles.cardLabel}>{t('behavior.points')}:</Text>
        <View style={styles.progressContainer}>
          <View style={styles.progressBar}>
            <View
              style={[styles.progressFill, { width: `${progressPercentage}%`, backgroundColor: getProgressColor() }]}
            />
            <Text style={styles.progressText}>
              {currentPoints} / {behavior.points}
            </Text>
          </View>
        </View>
      </View>

      <View style={styles.cardActions}>
        <TouchableOpacity style={[styles.actionButton, { flex: 0.3 }]} onPress={onShowGuideline}>
          <MaterialIcons name="info-outline" size={20} color="#2196F3" />
        </TouchableOpacity>
        <TouchableOpacity style={[styles.actionButton, styles.primaryAction, { flex: 0.7 }]} onPress={onEvaluate}>
          <MaterialIcons name="edit" size={20} color="#fff" />
          <Text style={styles.primaryActionText}>{t('behavior.evaluate.rule')}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

export const BehaviorEvaluateScreen: React.FC<{ route: any }> = ({ route }) => {
  const { childId } = route.params;
  const { t } = useI18n();
  const { width } = useWindowDimensions();
  const isDesktop = width >= 768;
  const queryClient = useQueryClient();

  const [showEvalModal, setShowEvalModal] = useState(false);
  const [selectedBehavior, setSelectedBehavior] = useState<BehaviorResponse | null>(null);
  const [guidelineModal, setGuidelineModal] = useState<{ visible: boolean; title: string; guideline: string }>({
    visible: false,
    title: '',
    guideline: '',
  });

  const { data: child } = useQuery({
    queryKey: ['child', childId],
    queryFn: () => usersApi.getById(childId),
  });

  const { data: behaviors, isLoading } = useQuery({
    queryKey: ['behaviors', childId],
    queryFn: () => behaviorsApi.list({ userId: childId }),
  });

  const { data: evaluations, refetch } = useQuery({
    queryKey: ['evaluations', childId],
    queryFn: () => evaluationsApi.list({ userId: childId, committed: false }),
  });

  const saveMutation = useMutation({
    mutationFn: evaluationsApi.save,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['evaluations', childId] });
      setShowEvalModal(false);
      setSelectedBehavior(null);
    },
    onError: () => {
      Alert.alert(t('common.error'), t('behavior.save.error'));
    },
  });

  const commitMutation = useMutation({
    mutationFn: () => evaluationsApi.commit(childId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['evaluations', childId] });
      queryClient.invalidateQueries({ queryKey: ['child', childId] });
      const message = `${data.evaluationsCommitted} ${t('behavior.finalize.success.evaluations')}, ${data.totalPointsAwarded} ${t('behavior.finalize.success.points')}`;
      if (Platform.OS === 'web') {
        window.alert(message);
      } else {
        Alert.alert(t('common.success'), message);
      }
    },
    onError: () => {
      Alert.alert(t('common.error'), t('behavior.finalize.error'));
    },
  });

  const handleEvaluate = (behavior: BehaviorResponse) => {
    setSelectedBehavior(behavior);
    setShowEvalModal(true);
  };

  const handleCommit = () => {
    const message = t('behavior.finalize.confirm');
    if (Platform.OS === 'web') {
      if (window.confirm(message)) {
        commitMutation.mutate();
      }
    } else {
      Alert.alert(t('behavior.finalize'), message, [
        { text: t('button.cancel'), style: 'cancel' },
        {
          text: t('behavior.finalize.button'),
          onPress: () => commitMutation.mutate(),
        },
      ]);
    }
  };

  const weeklyTotal = evaluations?.reduce((sum, item) => sum + item.currentPoints, 0) || 0;
  const existingEvaluation = selectedBehavior
    ? evaluations?.find((e) => e.behavior.id === selectedBehavior.id)
    : null;

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.title}>
            <MaterialIcons name="star" size={24} color="#ffc107" />{' '}
            {child ? t('behavior.evaluate.title', { 0: child.firstName }) : t('behavior.evaluate')}
          </Text>
          <Text style={styles.subtitle}>{t('behavior.evaluate.subtitle')}</Text>
        </View>
      </View>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#ffc107" />
        </View>
      ) : (
        <ScrollView style={styles.content} refreshControl={<RefreshControl refreshing={false} onRefresh={refetch} />}>
          {isDesktop ? (
            <View style={styles.table}>
              <View style={styles.tableHeader}>
                <View style={[styles.tableHeaderCell, { flex: 2 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.rule')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { flex: 1 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.points')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { width: 180 }]}>
                  <Text style={styles.tableHeaderText}>{t('common.actions')}</Text>
                </View>
              </View>
              {behaviors?.map((behavior) => (
                <BehaviorEvalRow
                  key={behavior.id}
                  behavior={behavior}
                  evaluation={evaluations?.find((e) => e.behavior.id === behavior.id)}
                  isDesktop={isDesktop}
                  onEvaluate={() => handleEvaluate(behavior)}
                  onShowGuideline={() =>
                    setGuidelineModal({ visible: true, title: behavior.title, guideline: behavior.guideline })
                  }
                  t={t}
                />
              ))}
              <View style={styles.tableSummary}>
                <Text style={styles.summaryLabel}>{t('behavior.weekly.total')}:</Text>
                <Text style={styles.summaryValue}>{weeklyTotal}</Text>
              </View>
            </View>
          ) : (
            <View style={styles.cardList}>
              {behaviors?.map((behavior) => (
                <BehaviorEvalRow
                  key={behavior.id}
                  behavior={behavior}
                  evaluation={evaluations?.find((e) => e.behavior.id === behavior.id)}
                  isDesktop={isDesktop}
                  onEvaluate={() => handleEvaluate(behavior)}
                  onShowGuideline={() =>
                    setGuidelineModal({ visible: true, title: behavior.title, guideline: behavior.guideline })
                  }
                  t={t}
                />
              ))}
              <View style={styles.totalContainer}>
                <Text style={styles.totalText}>
                  {t('behavior.weekly.total')}: <Text style={styles.totalValue}>{weeklyTotal}</Text>
                </Text>
              </View>
            </View>
          )}

          <View style={styles.finalizeCard}>
            <View style={styles.finalizeContent}>
              <MaterialIcons name="check-circle" size={32} color="#4CAF50" />
              <View style={styles.finalizeText}>
                <Text style={styles.finalizeTitle}>{t('behavior.finalize')}</Text>
                <Text style={styles.finalizeDesc}>
                  {child ? t('behavior.finalize.desc', { 0: child.firstName }) : ''}
                </Text>
              </View>
            </View>
            <TouchableOpacity
              style={styles.finalizeButton}
              onPress={handleCommit}
              disabled={commitMutation.isPending}
            >
              {commitMutation.isPending ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <>
                  <MaterialIcons name="save" size={20} color="#fff" />
                  <Text style={styles.finalizeButtonText}>{t('behavior.finalize.button')}</Text>
                </>
              )}
            </TouchableOpacity>
          </View>
        </ScrollView>
      )}

      <EvaluationModal
        visible={showEvalModal}
        behavior={selectedBehavior}
        existingEvaluation={existingEvaluation || null}
        childId={childId}
        onClose={() => {
          setShowEvalModal(false);
          setSelectedBehavior(null);
        }}
        onSave={(data) => saveMutation.mutate(data)}
        isLoading={saveMutation.isPending}
        t={t}
      />

      <Modal visible={guidelineModal.visible} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.guidelineModalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>{guidelineModal.title}</Text>
              <TouchableOpacity
                onPress={() => setGuidelineModal({ visible: false, title: '', guideline: '' })}
                style={styles.closeButton}
              >
                <MaterialIcons name="close" size={24} color="#666" />
              </TouchableOpacity>
            </View>
            <View style={styles.modalBody}>
              <Text style={styles.guidelineText}>{guidelineModal.guideline}</Text>
            </View>
            <View style={styles.modalFooter}>
              <TouchableOpacity
                style={styles.modalButton}
                onPress={() => setGuidelineModal({ visible: false, title: '', guideline: '' })}
              >
                <Text style={styles.modalButtonText}>{t('button.close')}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#fff',
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
    color: '#333',
  },
  subtitle: {
    fontSize: 14,
    color: '#666',
  },
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    flex: 1,
  },
  table: {
    backgroundColor: '#fff',
    margin: 16,
    borderRadius: 12,
    overflow: 'hidden',
    ...Platform.select({
      web: {
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      },
      default: {
        elevation: 2,
      },
    }),
  },
  tableHeader: {
    flexDirection: 'row',
    backgroundColor: '#f8f9fa',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderBottomWidth: 2,
    borderBottomColor: '#e0e0e0',
  },
  tableHeaderCell: {
    justifyContent: 'center',
  },
  tableHeaderText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#424242',
    textTransform: 'uppercase',
  },
  tableRow: {
    flexDirection: 'row',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
    alignItems: 'center',
  },
  tableCell: {
    justifyContent: 'center',
  },
  behaviorTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  progressContainer: {
    width: '100%',
  },
  progressBar: {
    height: 30,
    backgroundColor: '#e9ecef',
    borderRadius: 15,
    overflow: 'hidden',
    position: 'relative',
    justifyContent: 'center',
    alignItems: 'center',
  },
  progressFill: {
    position: 'absolute',
    left: 0,
    top: 0,
    bottom: 0,
    borderRadius: 15,
  },
  progressText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#000',
    zIndex: 2,
  },
  iconButton: {
    padding: 8,
    borderRadius: 6,
    backgroundColor: '#f5f5f5',
  },
  evaluateButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 6,
    backgroundColor: '#2196F3',
  },
  evaluateButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  tableSummary: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    padding: 16,
    gap: 8,
    backgroundColor: '#f8f9fa',
    borderTopWidth: 2,
    borderTopColor: '#e0e0e0',
  },
  summaryLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  summaryValue: {
    fontSize: 20,
    fontWeight: '700',
    color: '#ffc107',
  },
  cardList: {
    padding: 16,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    ...Platform.select({
      web: {
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
      },
      default: {
        elevation: 2,
      },
    }),
  },
  cardHeader: {
    marginBottom: 12,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
  },
  cardContent: {
    gap: 8,
    marginBottom: 12,
  },
  cardLabel: {
    fontSize: 14,
    color: '#666',
  },
  cardActions: {
    flexDirection: 'row',
    gap: 8,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    padding: 10,
    borderRadius: 8,
    backgroundColor: '#f5f5f5',
  },
  primaryAction: {
    backgroundColor: '#2196F3',
  },
  primaryActionText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 14,
  },
  totalContainer: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 8,
  },
  totalText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
  },
  totalValue: {
    fontSize: 24,
    fontWeight: '700',
    color: '#ffc107',
  },
  finalizeCard: {
    backgroundColor: '#fff',
    margin: 16,
    padding: 16,
    borderRadius: 12,
    ...Platform.select({
      web: {
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      },
      default: {
        elevation: 2,
      },
    }),
  },
  finalizeContent: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 16,
  },
  finalizeText: {
    flex: 1,
  },
  finalizeTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  finalizeDesc: {
    fontSize: 14,
    color: '#666',
  },
  finalizeButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: '#4CAF50',
    padding: 14,
    borderRadius: 8,
  },
  finalizeButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
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
    maxHeight: '90%',
  },
  guidelineModalContent: {
    backgroundColor: '#fff',
    borderRadius: 16,
    margin: 16,
    maxHeight: '80%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  closeButton: {
    padding: 4,
  },
  modalBody: {
    padding: 16,
  },
  behaviorName: {
    fontSize: 20,
    fontWeight: '600',
    color: '#333',
    marginBottom: 20,
  },
  sliderContainer: {
    marginBottom: 20,
  },
  sliderLabel: {
    fontSize: 16,
    color: '#333',
    marginBottom: 12,
  },
  sliderValue: {
    fontSize: 24,
    fontWeight: '700',
    color: '#2196F3',
  },
  slider: {
    width: '100%',
    height: 40,
  },
  progressBarPreview: {
    height: 8,
    backgroundColor: '#e0e0e0',
    borderRadius: 4,
    overflow: 'hidden',
    marginTop: 8,
  },
  progressBarFill: {
    height: '100%',
    borderRadius: 4,
  },
  inputGroup: {
    marginBottom: 16,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '600',
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
    minHeight: 120,
    textAlignVertical: 'top',
  },
  modalFooter: {
    flexDirection: 'row',
    padding: 16,
    gap: 8,
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
  },
  cancelButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  cancelButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#666',
  },
  submitButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    backgroundColor: '#2196F3',
  },
  submitButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  guidelineText: {
    fontSize: 16,
    color: '#333',
    lineHeight: 24,
  },
  modalButton: {
    backgroundColor: '#2196F3',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  modalButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
