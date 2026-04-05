import { MaterialIcons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Slider from '@react-native-community/slider';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useState } from 'react';
import {
  ActivityIndicator,
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
import { useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { behaviorsApi, evaluationsApi, usersApi } from '../../api/services';
import { useDialogs } from '../../hooks/useDialogs';
import { useI18n } from '../../i18n/I18nContext';
import { BehaviorEvaluationResponse, BehaviorResponse } from '../../types/api';

interface EvaluationModalProps {
  visible: boolean;
  behavior: BehaviorResponse | null;
  existingEvaluation: BehaviorEvaluationResponse | null;
  childId: number;
  onClose: () => void;
  onSave: (data: { behaviorId: number; userId: number; currentPoints: number; remarks?: string }) => void;
  onDraftChange?: (data: { behaviorId: number; currentPoints: number; remarks?: string }) => void;
  isLoading: boolean;
  t: (key: string) => string;
  styles: any;
}

const EvaluationModal: React.FC<EvaluationModalProps> = ({
  visible,
  behavior,
  existingEvaluation,
  childId,
  onClose,
  onSave,
  onDraftChange,
  isLoading,
  t,
  styles: modalStyles,
}) => {
  const styles = modalStyles;
  const [points, setPoints] = useState(0);
  const [remarks, setRemarks] = useState('');

  // Only initialize when the modal opens or the behavior changes — NOT when existingEvaluation
  // changes reference (which happens on every draft update and would cause an infinite loop).
  React.useEffect(() => {
    if (!behavior || !visible) return;
    if (existingEvaluation) {
      setPoints(existingEvaluation.currentPoints);
      setRemarks(existingEvaluation.remarks || '');
    } else {
      setPoints(0);
      setRemarks('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [behavior?.id, visible]);

  // Keep a stable ref so we can call the latest onDraftChange without adding it to deps
  // (adding it would re-trigger the effect every render because updateDraftItem may change reference).
  const onDraftChangeRef = React.useRef(onDraftChange);
  React.useEffect(() => {
    onDraftChangeRef.current = onDraftChange;
  });

  React.useEffect(() => {
    if (!visible || !behavior) return;
    onDraftChangeRef.current?.({
      behaviorId: behavior.id,
      currentPoints: points,
      remarks: remarks || undefined,
    });
  }, [behavior?.id, points, remarks, visible]);

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

  const totalRange = behavior.plusPoints + behavior.minusPoints;
  const progressPercentage = totalRange === 0 ? 50 : ((points + behavior.minusPoints) / totalRange) * 100;
  const getProgressColor = () => {
    if (points < 0) return '#dc3545';
    if (points === 0) return '#6c757d';
    return '#198754';
  };

  const formatSigned = (value: number) => (value > 0 ? `+${value}` : String(value));

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
                {t('behavior.points')}: <Text style={styles.sliderValue}>{formatSigned(points)}</Text> ({`-${behavior.minusPoints}..+${behavior.plusPoints}`})
              </Text>
              <Slider
                style={styles.slider}
                minimumValue={-behavior.minusPoints}
                maximumValue={behavior.plusPoints}
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
  styles: any;
}

const BehaviorEvalRow: React.FC<BehaviorEvalRowProps> = ({
  behavior,
  evaluation,
  isDesktop,
  onEvaluate,
  onShowGuideline,
  t,
  styles,
}) => {
  const currentPoints = evaluation?.currentPoints ?? 0;
  const totalRange = behavior.plusPoints + behavior.minusPoints;
  const progressPercentage = totalRange === 0 ? 50 : ((currentPoints + behavior.minusPoints) / totalRange) * 100;

  const getProgressColor = () => {
    if (currentPoints < 0) return '#dc3545'; // Red for negative values
    if (currentPoints > 0) return '#198754'; // Green for positive values
    return '#ffc107'; // Yellow for 0
  };

  const formatSigned = (value: number) => (value > 0 ? `+${value}` : String(value));

  if (isDesktop) {
    return (
      <View style={styles.tableRow}>
        <View style={[styles.tableCell, { flex: 2 }]}>
          <Text style={styles.behaviorTitle}>{behavior.title}</Text>
        </View>
        <View style={[styles.tableCell, { flex: 1, alignItems: 'center' }]}>
          <View style={styles.progressContainer}>
            <View style={styles.progressBarWithLabels}>
              <Text style={styles.progressLabelLeft}>-{behavior.minusPoints}</Text>
              <View style={styles.progressBar}>
                <View
                  style={[styles.progressFill, { width: `${progressPercentage}%`, backgroundColor: getProgressColor() }]}
                />
                <Text style={styles.progressText}>
                  {formatSigned(currentPoints)}
                </Text>
              </View>
              <Text style={styles.progressLabelRight}>+{behavior.plusPoints}</Text>
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
          <View style={styles.progressBarWithLabels}>
            <Text style={styles.progressLabelLeft}>-{behavior.minusPoints}</Text>
            <View style={styles.progressBar}>
              <View
                style={[styles.progressFill, { width: `${progressPercentage}%`, backgroundColor: getProgressColor() }]}
              />
              <Text style={styles.progressText}>
                {formatSigned(currentPoints)}
              </Text>
            </View>
            <Text style={styles.progressLabelRight}>+{behavior.plusPoints}</Text>
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
  const { showSuccess, showError, showConfirm, Dialogs } = useDialogs();
  const theme = useTheme();
  const styles = createStyles(theme);

  const weekStartKey = React.useMemo(() => {
    const now = new Date();
    const day = now.getDay(); // 0=Sun .. 6=Sat
    const diffToMonday = day === 0 ? -6 : 1 - day;
    const monday = new Date(now);
    monday.setDate(now.getDate() + diffToMonday);
    monday.setHours(0, 0, 0, 0);

    const yyyy = monday.getFullYear();
    const mm = String(monday.getMonth() + 1).padStart(2, '0');
    const dd = String(monday.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }, []);

  const draftStorageKey = React.useMemo(
    () => `@ofc:behaviorDraft:${childId}:${weekStartKey}`,
    [childId, weekStartKey]
  );

  const [draft, setDraft] = useState<{
    childId: number;
    weekStart: string;
    updatedAt: number;
    items: Record<string, { currentPoints: number; remarks?: string }>;
  } | null>(null);

  const draftSaveTimeout = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  React.useEffect(() => {
    let isMounted = true;
    const loadDraft = async () => {
      try {
        const raw = await AsyncStorage.getItem(draftStorageKey);
        if (!raw) return;
        const parsed = JSON.parse(raw);
        if (isMounted) setDraft(parsed);
      } catch {
        // ignore corrupt drafts
      }
    };
    loadDraft();
    return () => {
      isMounted = false;
    };
  }, [draftStorageKey]);

  const [showEvalModal, setShowEvalModal] = useState(false);
  const [selectedBehavior, setSelectedBehavior] = useState<BehaviorResponse | null>(null);
  const [lastSavedBehaviorId, setLastSavedBehaviorId] = useState<number | null>(null);
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

      if (lastSavedBehaviorId && draft?.items?.[String(lastSavedBehaviorId)]) {
        const { [String(lastSavedBehaviorId)]: _, ...rest } = draft.items;
        const nextDraft = Object.keys(rest).length
          ? { ...draft, items: rest, updatedAt: Date.now() }
          : null;
        if (nextDraft) {
          AsyncStorage.setItem(draftStorageKey, JSON.stringify(nextDraft)).catch(() => { });
        } else {
          AsyncStorage.removeItem(draftStorageKey).catch(() => { });
        }
        setDraft(nextDraft);
      }

      setShowEvalModal(false);
      setSelectedBehavior(null);
      setLastSavedBehaviorId(null);
    },
    onError: () => {
      showError(t('behavior.save.error'), t('common.error'));
    },
  });

  const commitMutation = useMutation({
    mutationFn: () => evaluationsApi.commit(childId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['evaluations', childId] });
      queryClient.invalidateQueries({ queryKey: ['child', childId] });
      queryClient.invalidateQueries({ queryKey: ['children'] });
      queryClient.invalidateQueries({ queryKey: ['points', childId] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      const message = `${data.evaluationsCommitted} ${t('behavior.finalize.success.evaluations')}, ${data.totalPointsAwarded} ${t('behavior.finalize.success.points')}`;
      showSuccess(message, t('common.success'));
    },
    onError: () => {
      showError(t('behavior.finalize.error'), t('common.error'));
    },
  });

  const handleEvaluate = (behavior: BehaviorResponse) => {
    setSelectedBehavior(behavior);
    setShowEvalModal(true);
  };

  const persistDraft = React.useCallback(
    (next: {
      childId: number;
      weekStart: string;
      updatedAt: number;
      items: Record<string, { currentPoints: number; remarks?: string }>;
    }) => {
      setDraft(next);

      if (draftSaveTimeout.current) {
        clearTimeout(draftSaveTimeout.current);
      }

      draftSaveTimeout.current = setTimeout(() => {
        AsyncStorage.setItem(draftStorageKey, JSON.stringify(next)).catch(() => {
          // ignore
        });
      }, 250);
    },
    [draftStorageKey]
  );

  const updateDraftItem = React.useCallback(
    (data: { behaviorId: number; currentPoints: number; remarks?: string }) => {
      setDraft((prevDraft) => {
        const next = {
          childId,
          weekStart: weekStartKey,
          updatedAt: Date.now(),
          items: {
            ...(prevDraft?.items ?? {}),
            [String(data.behaviorId)]: {
              currentPoints: data.currentPoints,
              remarks: data.remarks,
            },
          },
        };

        // Persist to AsyncStorage (debounced)
        if (draftSaveTimeout.current) {
          clearTimeout(draftSaveTimeout.current);
        }
        draftSaveTimeout.current = setTimeout(() => {
          AsyncStorage.setItem(draftStorageKey, JSON.stringify(next)).catch(() => { });
        }, 250);

        return next;
      });
    },
    [childId, weekStartKey, draftStorageKey]
  );

  const discardDraft = React.useCallback(async () => {
    setDraft(null);
    try {
      await AsyncStorage.removeItem(draftStorageKey);
    } catch {
      // ignore
    }
  }, [draftStorageKey]);

  const saveDraftAndCommit = async () => {
    try {
      if (draft) {
        await Promise.all(
          Object.entries(draft.items).map(([behaviorId, value]) =>
            evaluationsApi.save({
              behaviorId: Number(behaviorId),
              userId: childId,
              currentPoints: value.currentPoints,
              remarks: value.remarks,
            })
          )
        );
        await discardDraft();
      }
      commitMutation.mutate();
    } catch {
      showError(t('behavior.save.error'), t('common.error'));
    }
  };

  const handleCommit = () => {
    const hasDraft = !!draft && Object.keys(draft.items).length > 0;
    const message = t('behavior.finalize.confirm');

    if (!hasDraft) {
      showConfirm({
        title: t('behavior.finalize'),
        message,
        onConfirm: () => commitMutation.mutate(),
        confirmText: t('behavior.finalize.button'),
        cancelText: t('button.cancel'),
      });
    } else {
      // For drafts, we'll use a simple confirm to save & commit
      showConfirm({
        title: t('behavior.finalize'),
        message: `${t('behavior.draft.unsaved')}\n\n${message}`,
        onConfirm: saveDraftAndCommit,
        confirmText: t('behavior.draft.save_and_commit'),
        cancelText: t('button.cancel'),
      });
    }
  };

  const effectiveEvaluationFor = (behavior: BehaviorResponse): BehaviorEvaluationResponse | undefined => {
    const serverEval = evaluations?.find((e) => e.behavior.id === behavior.id);
    const draftItem = draft?.items?.[String(behavior.id)];
    if (!draftItem) return serverEval;

    if (serverEval) {
      return {
        ...serverEval,
        currentPoints: draftItem.currentPoints,
        remarks: draftItem.remarks,
      };
    }

    return {
      behavior,
      user: child as any,
      currentPoints: draftItem.currentPoints,
      remarks: draftItem.remarks,
    } as any;
  };

  const weeklyTotal =
    behaviors?.reduce((sum, behavior) => sum + (effectiveEvaluationFor(behavior)?.currentPoints ?? 0), 0) ?? 0;

  const existingEvaluation = selectedBehavior ? effectiveEvaluationFor(selectedBehavior) : null;

  return (
    <SafeAreaView style={styles.container} edges={[]}>
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.title}>
            <MaterialIcons name="star" size={24} color="#ffc107" />{' '}
            {child ? t('behavior.evaluate.title', { childName: child.firstName }) : t('behavior.evaluate')}
          </Text>
          <Text style={styles.subtitle}>{t('behavior.evaluate.subtitle')}</Text>
          {draft?.updatedAt && (
            <Text style={styles.draftHint}>
              {t('behavior.draft.saved')} ({new Date(draft.updatedAt).toLocaleTimeString()})
            </Text>
          )}
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
                  evaluation={effectiveEvaluationFor(behavior)}
                  isDesktop={isDesktop}
                  onEvaluate={() => handleEvaluate(behavior)}
                  onShowGuideline={() =>
                    setGuidelineModal({ visible: true, title: behavior.title, guideline: behavior.guideline })
                  }
                  t={t}
                  styles={styles}
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
                  evaluation={effectiveEvaluationFor(behavior)}
                  isDesktop={isDesktop}
                  onEvaluate={() => handleEvaluate(behavior)}
                  onShowGuideline={() =>
                    setGuidelineModal({ visible: true, title: behavior.title, guideline: behavior.guideline })
                  }
                  t={t}
                  styles={styles}
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
                  {child ? t('behavior.finalize.desc', { firstName: child.firstName }) : ''}
                </Text>
              </View>
            </View>

            {!!draft && Object.keys(draft.items).length > 0 && (
              <TouchableOpacity
                style={styles.discardDraftButton}
                onPress={() => {
                  showConfirm({
                    title: t('behavior.draft.discard'),
                    message: t('behavior.draft.discard.confirm'),
                    onConfirm: discardDraft,
                    confirmText: t('button.delete'),
                    cancelText: t('button.cancel'),
                    destructive: true,
                  });
                }}
              >
                <Text style={styles.discardDraftText}>{t('behavior.draft.discard')}</Text>
              </TouchableOpacity>
            )}

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
        onSave={(data) => {
          setLastSavedBehaviorId(data.behaviorId);
          saveMutation.mutate(data);
        }}
        onDraftChange={updateDraftItem}
        isLoading={saveMutation.isPending}
        t={t}
        styles={styles}
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

      <Dialogs />
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
    borderBottomColor: theme.colors.surfaceVariant,
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
  draftHint: {
    marginTop: 4,
    fontSize: 12,
    color: '#2e7d32',
    fontWeight: '600',
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
    backgroundColor: theme.colors.surface,
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
  discardDraftButton: {
    marginTop: 12,
    marginBottom: 8,
    alignSelf: 'flex-start',
  },
  discardDraftText: {
    color: '#d32f2f',
    fontSize: 14,
    fontWeight: '600',
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
  progressBarWithLabels: {
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
  },
  progressLabelLeft: {
    fontSize: 12,
    color: theme.colors.onSurfaceVariant,
    marginRight: 8,
    minWidth: 20,
    textAlign: 'right',
  },
  progressLabelRight: {
    fontSize: 12,
    color: theme.colors.onSurfaceVariant,
    marginLeft: 8,
    minWidth: 20,
    textAlign: 'left',
  },
  progressBar: {
    flex: 1,
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
    color: theme.colors.onSurface,
  },
  summaryValue: {
    fontSize: 20,
    fontWeight: '700',
    color: theme.colors.primary,
  },
  cardList: {
    padding: 16,
  },
  card: {
    backgroundColor: theme.colors.surface,
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
    color: theme.colors.onSurface,
  },
  cardContent: {
    gap: 8,
    marginBottom: 12,
  },
  cardLabel: {
    fontSize: 14,
    color: theme.colors.onSurfaceVariant,
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
    backgroundColor: theme.colors.surfaceVariant,
  },
  primaryAction: {
    backgroundColor: theme.colors.primary,
  },
  primaryActionText: {
    color: theme.colors.onPrimary,
    fontWeight: '600',
    fontSize: 14,
  },
  totalContainer: {
    backgroundColor: theme.colors.surface,
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 8,
  },
  totalText: {
    fontSize: 18,
    fontWeight: '600',
    color: theme.colors.onSurface,
  },
  totalValue: {
    fontSize: 24,
    fontWeight: '700',
    color: theme.colors.primary,
  },
  finalizeCard: {
    backgroundColor: theme.colors.surface,
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
    color: theme.colors.onSurface,
    marginBottom: 4,
  },
  finalizeDesc: {
    fontSize: 14,
    color: theme.colors.onSurfaceVariant,
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
    color: theme.colors.onPrimary,
    fontSize: 16,
    fontWeight: '600',
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
    maxHeight: '90%',
  },
  guidelineModalContent: {
    backgroundColor: theme.colors.surface,
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
    borderBottomColor: theme.colors.surfaceVariant,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: theme.colors.onSurface,
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
    color: theme.colors.onSurface,
    marginBottom: 20,
  },
  sliderContainer: {
    marginBottom: 20,
  },
  sliderLabel: {
    fontSize: 16,
    color: theme.colors.onSurface,
    marginBottom: 12,
  },
  sliderValue: {
    fontSize: 24,
    fontWeight: '700',
    color: theme.colors.primary,
  },
  slider: {
    width: '100%',
    height: 40,
  },
  progressBarPreview: {
    height: 8,
    backgroundColor: theme.colors.surfaceVariant,
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
    color: theme.colors.onSurface,
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: theme.colors.outline,
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
