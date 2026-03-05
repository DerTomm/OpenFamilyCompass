import { MaterialIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
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
  TouchableOpacity,
  useWindowDimensions,
  View,
} from 'react-native';
import { useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { behaviorsApi } from '../../api/services';
import { useDialogs } from '../../hooks/useDialogs';
import { useI18n } from '../../i18n/I18nContext';
import { ManageStackParamList } from '../../navigation/types';
import { BehaviorResponse } from '../../types/api';

type NavigationProp = NativeStackNavigationProp<ManageStackParamList>;

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
  const navigation = useNavigation<NavigationProp>();
  const { showConfirm, Dialogs } = useDialogs();

  const [guidelineModal, setGuidelineModal] = useState<{ visible: boolean; guideline: string }>({
    visible: false,
    guideline: '',
  });

  const { data: behaviors, isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['behaviors'],
    queryFn: () => behaviorsApi.list(),
  });

  const deleteMutation = useMutation({
    mutationFn: behaviorsApi.deactivate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['behaviors'] });
    },
  });

  const handleEdit = (behavior: BehaviorResponse) => {
    navigation.navigate('BehaviorEdit', { behaviorId: behavior.id });
  };

  const handleCreate = () => {
    navigation.navigate('BehaviorCreate');
  };

  const handleDelete = (behavior: BehaviorResponse) => {
    showConfirm({
      title: t('behavior.delete.title'),
      message: t('behavior.delete.confirm'),
      onConfirm: () => deleteMutation.mutate(behavior.id),
      confirmText: t('behavior.delete'),
      cancelText: t('button.cancel'),
      destructive: true,
    });
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.title}>
            <MaterialIcons name="star" size={24} color="#ffc107" /> {t('behavior.manage.title')}
          </Text>
        </View>
      </View>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#ffc107" />
        </View>
      ) : behaviors && behaviors.length > 0 ? (
        <ScrollView
          style={styles.content}
          contentContainerStyle={styles.scrollContent}
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
        </View>
      )}

      <TouchableOpacity style={styles.fab} onPress={handleCreate}>
        <MaterialIcons name="add" size={24} color="#fff" />
      </TouchableOpacity>

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
    borderBottomColor: theme.colors.outlineVariant,
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
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 20,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#2196F3',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 6,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
  },
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 80,
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
    backgroundColor: theme.colors.surfaceVariant,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderBottomWidth: 2,
    borderBottomColor: theme.colors.outlineVariant,
  },
  tableHeaderCell: {
    justifyContent: 'center',
  },
  tableHeaderText: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.colors.onSurfaceVariant,
    textTransform: 'uppercase',
  },
  tableRow: {
    flexDirection: 'row',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.outlineVariant,
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
    backgroundColor: theme.colors.surfaceVariant,
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
    backgroundColor: theme.colors.surfaceVariant,
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
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
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
    borderBottomColor: theme.colors.outlineVariant,
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
  modalFooter: {
    flexDirection: 'row',
    padding: 16,
    gap: 8,
    borderTopWidth: 1,
    borderTopColor: theme.colors.outlineVariant,
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
