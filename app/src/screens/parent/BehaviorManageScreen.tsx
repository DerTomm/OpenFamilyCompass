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
import { useTheme } from 'react-native-paper';
import { MaterialIcons } from '@expo/vector-icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { behaviorsApi, usersApi } from '../../api/services';
import { useI18n } from '../../i18n/I18nContext';
import { BehaviorResponse, ChildResponse } from '../../types/api';

interface BehaviorModalProps {
  visible: boolean;
  behavior: BehaviorResponse | null;
  children: ChildResponse[];
  onClose: () => void;
  onSubmit: (data: { title: string; guideline: string; plusPoints: number; minusPoints: number; userId?: number }) => void;
  isLoading: boolean;
  t: (key: string) => string;
  styles: any;
}

const BehaviorModal: React.FC<BehaviorModalProps> = ({ visible, behavior, children, onClose, onSubmit, isLoading, t, styles }) => {
  const [title, setTitle] = useState('');
  const [guideline, setGuideline] = useState('');
  const [plusPoints, setPlusPoints] = useState('');
  const [minusPoints, setMinusPoints] = useState('');
  const [userId, setUserId] = useState<number | undefined>(undefined);

  React.useEffect(() => {
    if (behavior) {
      setTitle(behavior.title);
      setGuideline(behavior.guideline);
      setPlusPoints(behavior.plusPoints.toString());
      setMinusPoints(behavior.minusPoints.toString());
      setUserId(behavior.user?.id);
    } else {
      setTitle('');
      setGuideline('');
      setPlusPoints('');
      setMinusPoints('');
      setUserId(undefined);
    }
  }, [behavior, visible]);

  const handleSubmit = () => {
    if (!title || !guideline) {
      Alert.alert(t('common.error'), t('behavior.error.fields'));
      return;
    }

    const parsedPlus = plusPoints.trim().length ? parseInt(plusPoints, 10) : 0;
    const parsedMinus = minusPoints.trim().length ? parseInt(minusPoints, 10) : 0;

    if (Number.isNaN(parsedPlus) || Number.isNaN(parsedMinus) || parsedPlus < 0 || parsedMinus < 0) {
      Alert.alert(t('common.error'), t('behavior.error.fields'));
      return;
    }

    if (parsedPlus === 0 && parsedMinus === 0) {
      Alert.alert(t('common.error'), t('behavior.error.fields'));
      return;
    }

    onSubmit({
      title,
      guideline,
      plusPoints: parsedPlus,
      minusPoints: parsedMinus,
      userId,
    });
  };

  return (
    <Modal visible={visible} transparent animationType="slide">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>
              {behavior ? t('behavior.edit.title') : t('behavior.create.title')}
            </Text>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <MaterialIcons name="close" size={24} color="#666" />
            </TouchableOpacity>
          </View>
          
          <ScrollView style={styles.modalBody}>
            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>{t('behavior.title.label')} *</Text>
              <TextInput
                style={styles.input}
                value={title}
                onChangeText={setTitle}
                placeholder={t('behavior.title.hint')}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>{t('behavior.guidelines.label')} *</Text>
              <TextInput
                style={[styles.input, styles.textArea]}
                value={guideline}
                onChangeText={setGuideline}
                placeholder={t('behavior.guidelines.hint')}
                multiline
                numberOfLines={4}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>{t('behavior.plusPoints.label')} *</Text>
              <TextInput
                style={styles.input}
                value={plusPoints}
                onChangeText={setPlusPoints}
                placeholder={t('behavior.plusPoints.hint')}
                keyboardType="number-pad"
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>{t('behavior.minusPoints.label')} *</Text>
              <TextInput
                style={styles.input}
                value={minusPoints}
                onChangeText={setMinusPoints}
                placeholder={t('behavior.minusPoints.hint')}
                keyboardType="number-pad"
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.inputLabel}>{t('behavior.child.label')}</Text>
              <View style={styles.childSelector}>
                <TouchableOpacity
                  style={[styles.childOption, userId === undefined && styles.childOptionActive]}
                  onPress={() => setUserId(undefined)}
                >
                  <Text style={[styles.childOptionText, userId === undefined && styles.childOptionTextActive]}>
                    {t('children.all')}
                  </Text>
                </TouchableOpacity>
                {children.map((child) => (
                  <TouchableOpacity
                    key={child.id}
                    style={[styles.childOption, userId === child.id && styles.childOptionActive]}
                    onPress={() => setUserId(child.id)}
                  >
                    <Text style={[styles.childOptionText, userId === child.id && styles.childOptionTextActive]}>
                      {child.firstName}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          </ScrollView>

          <View style={styles.modalFooter}>
            <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
              <Text style={styles.cancelButtonText}>{t('button.cancel')}</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.submitButton} onPress={handleSubmit} disabled={isLoading}>
              {isLoading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.submitButtonText}>
                  {behavior ? t('button.save') : t('behavior.create.button')}
                </Text>
              )}
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

interface BehaviorRowProps {
  behavior: BehaviorResponse;
  isDesktop: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onShowGuideline: () => void;
  t: (key: string) => string;
  styles: any;
}

const BehaviorRow: React.FC<BehaviorRowProps> = ({ behavior, isDesktop, onEdit, onDelete, onShowGuideline, t, styles }) => {
  const pointsRange = `+${behavior.plusPoints} / -${behavior.minusPoints}`;

  if (isDesktop) {
    return (
      <View style={styles.tableRow}>
        <View style={[styles.tableCell, { flex: 2 }]}>
          <Text style={styles.behaviorTitle}>{behavior.title}</Text>
        </View>
        <View style={[styles.tableCell, { flex: 1, alignItems: 'center' }]}>
          <TouchableOpacity onPress={onShowGuideline}>
            <MaterialIcons name="info-outline" size={24} color="#2196F3" />
          </TouchableOpacity>
        </View>
        <View style={[styles.tableCell, { flex: 1 }]}>
          <Text style={styles.tableCellText}>{pointsRange}</Text>
        </View>
        <View style={[styles.tableCell, { flex: 1 }]}>
          <Text style={styles.tableCellText}>
            {behavior.user ? behavior.user.firstName : t('children.all')}
          </Text>
        </View>
        <View style={[styles.tableCell, { width: 100, flexDirection: 'row', gap: 8 }]}>
          <TouchableOpacity style={styles.iconButton} onPress={onEdit}>
            <MaterialIcons name="edit" size={20} color="#2196F3" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconButton} onPress={onDelete}>
            <MaterialIcons name="delete" size={20} color="#F44336" />
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
      
      <View style={styles.cardRow}>
        <Text style={styles.cardLabel}>{t('behavior.guidelines.label')}:</Text>
        <TouchableOpacity onPress={onShowGuideline}>
          <MaterialIcons name="info-outline" size={24} color="#2196F3" />
        </TouchableOpacity>
      </View>

      <View style={styles.cardRow}>
        <Text style={styles.cardLabel}>{t('behavior.points')}:</Text>
        <Text style={styles.cardValue}>{pointsRange}</Text>
      </View>

      <View style={styles.cardRow}>
        <Text style={styles.cardLabel}>{t('behavior.child.label')}:</Text>
        <Text style={styles.cardValue}>
          {behavior.user ? behavior.user.firstName : t('children.all')}
        </Text>
      </View>

      <View style={styles.cardActions}>
        <TouchableOpacity style={styles.actionButton} onPress={onEdit}>
          <MaterialIcons name="edit" size={20} color="#2196F3" />
          <Text style={[styles.actionButtonText, { color: '#2196F3' }]}>
            {t('button.edit')}
          </Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionButton} onPress={onDelete}>
          <MaterialIcons name="delete" size={20} color="#F44336" />
          <Text style={[styles.actionButtonText, { color: '#F44336' }]}>
            {t('behavior.delete')}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

export const BehaviorManageScreen: React.FC = () => {
  const { t } = useI18n();
  const { width } = useWindowDimensions();
  const isDesktop = width >= 768;
  const queryClient = useQueryClient();
  const theme = useTheme();
  const styles = createStyles(theme);

  const [showModal, setShowModal] = useState(false);
  const [editingBehavior, setEditingBehavior] = useState<BehaviorResponse | null>(null);
  const [guidelineModal, setGuidelineModal] = useState<{ visible: boolean; guideline: string }>({
    visible: false,
    guideline: '',
  });

  const { data: behaviors, isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['behaviors'],
    queryFn: () => behaviorsApi.list(),
  });

  const { data: children } = useQuery({
    queryKey: ['children'],
    queryFn: () => usersApi.listChildren(),
  });

  const createMutation = useMutation({
    mutationFn: behaviorsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['behaviors'] });
      setShowModal(false);
      if (Platform.OS === 'web') {
        window.alert(t('behavior.create.success'));
      } else {
        Alert.alert(t('common.success'), t('behavior.create.success'));
      }
    },
    onError: () => {
      Alert.alert(t('common.error'), t('behavior.create.error'));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) => behaviorsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['behaviors'] });
      setShowModal(false);
      setEditingBehavior(null);
      if (Platform.OS === 'web') {
        window.alert(t('behavior.edit.success'));
      } else {
        Alert.alert(t('common.success'), t('behavior.edit.success'));
      }
    },
    onError: () => {
      Alert.alert(t('common.error'), t('behavior.edit.error'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: behaviorsApi.deactivate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['behaviors'] });
    },
  });

  const handleEdit = (behavior: BehaviorResponse) => {
    setEditingBehavior(behavior);
    setShowModal(true);
  };

  const handleCreate = () => {
    setEditingBehavior(null);
    setShowModal(true);
  };

  const handleDelete = (behavior: BehaviorResponse) => {
    const message = t('behavior.delete.confirm');
    if (Platform.OS === 'web') {
      if (window.confirm(message)) {
        deleteMutation.mutate(behavior.id);
      }
    } else {
      Alert.alert(t('behavior.delete.title'), message, [
        { text: t('button.cancel'), style: 'cancel' },
        {
          text: t('behavior.delete'),
          style: 'destructive',
          onPress: () => deleteMutation.mutate(behavior.id),
        },
      ]);
    }
  };

  const handleSubmit = (data: { title: string; guideline: string; plusPoints: number; minusPoints: number; userId?: number }) => {
    if (editingBehavior) {
      updateMutation.mutate({ id: editingBehavior.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.title}>
            <MaterialIcons name="star" size={24} color="#ffc107" /> {t('behavior.manage.title')}
          </Text>
        </View>
        <TouchableOpacity style={styles.createButton} onPress={handleCreate}>
          <MaterialIcons name="add" size={24} color="#fff" />
          <Text style={styles.createButtonText}>{t('behavior.create')}</Text>
        </TouchableOpacity>
      </View>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#ffc107" />
        </View>
      ) : behaviors && behaviors.length > 0 ? (
        <ScrollView
          style={styles.content}
          refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} />}
        >
          {isDesktop ? (
            <View style={styles.table}>
              <View style={styles.tableHeader}>
                <View style={[styles.tableHeaderCell, { flex: 2 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.title.label')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { flex: 1 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.guidelines.label')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { flex: 1 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.points')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { flex: 1 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.child.label')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { width: 100 }]}>
                  <Text style={styles.tableHeaderText}>{t('common.actions')}</Text>
                </View>
              </View>
              {behaviors.map((behavior) => (
                <BehaviorRow
                  key={behavior.id}
                  behavior={behavior}
                  isDesktop={isDesktop}
                  onEdit={() => handleEdit(behavior)}
                  onDelete={() => handleDelete(behavior)}
                  onShowGuideline={() => setGuidelineModal({ visible: true, guideline: behavior.guideline })}
                  t={t}
                  styles={styles}
                />
              ))}
            </View>
          ) : (
            <View style={styles.cardList}>
              {behaviors.map((behavior) => (
                <BehaviorRow
                  key={behavior.id}
                  behavior={behavior}
                  isDesktop={isDesktop}
                  onEdit={() => handleEdit(behavior)}
                  onDelete={() => handleDelete(behavior)}
                  onShowGuideline={() => setGuidelineModal({ visible: true, guideline: behavior.guideline })}
                  t={t}
                  styles={styles}
                />
              ))}
            </View>
          )}
        </ScrollView>
      ) : (
        <View style={styles.emptyContainer}>
          <MaterialIcons name="star-border" size={64} color="#ccc" />
          <Text style={styles.emptyTitle}>{t('behavior.none')}</Text>
          <TouchableOpacity style={styles.emptyButton} onPress={handleCreate}>
            <MaterialIcons name="add" size={20} color="#fff" />
            <Text style={styles.emptyButtonText}>{t('behavior.create')}</Text>
          </TouchableOpacity>
        </View>
      )}

      <BehaviorModal
        visible={showModal}
        behavior={editingBehavior}
        children={children || []}
        onClose={() => {
          setShowModal(false);
          setEditingBehavior(null);
        }}
        onSubmit={handleSubmit}
        isLoading={createMutation.isPending || updateMutation.isPending}
        t={t}
        styles={styles}
      />

      <Modal visible={guidelineModal.visible} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.guidelineModalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>{t('behavior.guidelines.label')}</Text>
              <TouchableOpacity
                onPress={() => setGuidelineModal({ visible: false, guideline: '' })}
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
                onPress={() => setGuidelineModal({ visible: false, guideline: '' })}
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

const createStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: theme.colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    gap: 12,
  },
  headerContent: {
    gap: 4,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: theme.colors.onSurface,
  },
  createButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: '#ffc107',
    padding: 12,
    borderRadius: 8,
  },
  createButtonText: {
    color: '#fff',
    fontSize: 16,
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
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
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
  tableCellText: {
    fontSize: 15,
    color: theme.colors.onSurface,
  },
  behaviorTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: theme.colors.onSurface,
  },
  iconButton: {
    padding: 8,
    borderRadius: 6,
    backgroundColor: '#f5f5f5',
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
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
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
  cardRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  cardLabel: {
    fontSize: 14,
    color: theme.colors.onSurfaceVariant,
  },
  cardValue: {
    fontSize: 14,
    fontWeight: '600',
    color: theme.colors.onSurface,
  },
  cardActions: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 12,
  },
  actionButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    padding: 10,
    borderRadius: 8,
    backgroundColor: '#f5f5f5',
  },
  actionButtonText: {
    fontWeight: '500',
    fontSize: 13,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: theme.colors.onSurfaceVariant,
    marginTop: 16,
    marginBottom: 16,
  },
  emptyButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#ffc107',
    padding: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  emptyButtonText: {
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
    borderBottomColor: '#e0e0e0',
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
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
  textArea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  childSelector: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  childOption: {
    padding: 10,
    borderRadius: 8,
    backgroundColor: '#f0f0f0',
    minWidth: 80,
    alignItems: 'center',
  },
  childOptionActive: {
    backgroundColor: '#ffc107',
  },
  childOptionText: {
    fontWeight: '500',
    color: theme.colors.onSurfaceVariant,
  },
  childOptionTextActive: {
    color: '#fff',
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
    color: theme.colors.onSurfaceVariant,
  },
  submitButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    backgroundColor: '#ffc107',
  },
  submitButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  guidelineText: {
    fontSize: 16,
    color: theme.colors.onSurface,
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
