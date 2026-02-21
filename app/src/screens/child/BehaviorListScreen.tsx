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
import { SafeAreaView } from 'react-native-safe-area-context';
import { MaterialIcons } from '@expo/vector-icons';
import { useQuery } from '@tanstack/react-query';
import { evaluationsApi } from '../../api/services';
import { useI18n } from '../../i18n/I18nContext';
import { BehaviorEvaluationResponse } from '../../types/api';
import { useAuthStore } from '../../store/authStore';

interface GuidelineModalProps {
  visible: boolean;
  title: string;
  guideline: string;
  onClose: () => void;
}

const GuidelineModal: React.FC<GuidelineModalProps> = ({ visible, title, guideline, onClose }) => {
  const { t } = useI18n();
  
  return (
    <Modal visible={visible} transparent animationType="fade">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>{title}</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <MaterialIcons name="close" size={24} color="#666" />
            </TouchableOpacity>
          </View>
          <View style={styles.modalBody}>
            <Text style={styles.guidelineText}>{guideline}</Text>
          </View>
          <View style={styles.modalFooter}>
            <TouchableOpacity style={styles.modalButton} onPress={onClose}>
              <Text style={styles.modalButtonText}>{t('button.close')}</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

interface RemarksModalProps {
  visible: boolean;
  title: string;
  remarks: string;
  onClose: () => void;
}

const RemarksModal: React.FC<RemarksModalProps> = ({ visible, title, remarks, onClose }) => {
  const { t } = useI18n();
  
  return (
    <Modal visible={visible} transparent animationType="fade">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>{title}</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <MaterialIcons name="close" size={24} color="#666" />
            </TouchableOpacity>
          </View>
          <View style={styles.modalBody}>
            <Text style={styles.remarksLabel}>{t('behavior.remarks')}:</Text>
            <Text style={styles.remarksText}>{remarks}</Text>
          </View>
          <View style={styles.modalFooter}>
            <TouchableOpacity style={styles.modalButton} onPress={onClose}>
              <Text style={styles.modalButtonText}>{t('button.close')}</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

interface BehaviorCardProps {
  evaluation: BehaviorEvaluationResponse;
  isDesktop: boolean;
  onShowGuideline: () => void;
  onShowRemarks: () => void;
}

const BehaviorCard: React.FC<BehaviorCardProps> = ({ evaluation, isDesktop, onShowGuideline, onShowRemarks }) => {
  const { t } = useI18n();
  const totalRange = evaluation.behavior.plusPoints + evaluation.behavior.minusPoints;
  const progressPercentage =
    totalRange === 0 ? 50 : ((evaluation.currentPoints + evaluation.behavior.minusPoints) / totalRange) * 100;

  const formatSigned = (value: number) => (value > 0 ? `+${value}` : String(value));
  
  const getProgressColor = () => {
    if (evaluation.currentPoints < 0) return '#dc3545';
    if (evaluation.currentPoints === 0) return '#6c757d';
    return '#198754';
  };

  if (isDesktop) {
    return (
      <View style={styles.tableRow}>
        <View style={[styles.tableCell, { flex: 2 }]}>
          <TouchableOpacity onPress={onShowGuideline}>
            <Text style={styles.behaviorTitle}>{evaluation.behavior.title}</Text>
          </TouchableOpacity>
        </View>
        <View style={[styles.tableCell, { flex: 1, alignItems: 'center' }]}>
          <View style={styles.progressContainer}>
            <View style={styles.progressBar}>
              <View 
                style={[
                  styles.progressFill, 
                  { width: `${progressPercentage}%`, backgroundColor: getProgressColor() }
                ]} 
              />
              <Text style={styles.progressText}>
                {formatSigned(evaluation.currentPoints)} ({`-${evaluation.behavior.minusPoints}..+${evaluation.behavior.plusPoints}`})
              </Text>
            </View>
          </View>
        </View>
        <View style={[styles.tableCell, { width: 60, alignItems: 'center' }]}>
          {evaluation.remarks && (
            <TouchableOpacity onPress={onShowRemarks} style={styles.iconButton}>
              <MaterialIcons name="chat-bubble-outline" size={24} color="#667eea" />
            </TouchableOpacity>
          )}
        </View>
      </View>
    );
  }

  return (
    <View style={styles.card}>
      <TouchableOpacity onPress={onShowGuideline} style={styles.cardHeader}>
        <Text style={styles.cardTitle}>{evaluation.behavior.title}</Text>
      </TouchableOpacity>
      
      <View style={styles.cardContent}>
        <Text style={styles.cardLabel}>{t('behavior.points')}:</Text>
        <View style={styles.progressContainer}>
          <View style={styles.progressBar}>
            <View 
              style={[
                styles.progressFill, 
                { width: `${progressPercentage}%`, backgroundColor: getProgressColor() }
              ]} 
            />
            <Text style={styles.progressText}>
              {formatSigned(evaluation.currentPoints)} ({`-${evaluation.behavior.minusPoints}..+${evaluation.behavior.plusPoints}`})
            </Text>
          </View>
        </View>
      </View>

      {evaluation.remarks && (
        <TouchableOpacity style={styles.remarksButton} onPress={onShowRemarks}>
          <MaterialIcons name="chat-bubble-outline" size={20} color="#667eea" />
          <Text style={styles.remarksButtonText}>{t('behavior.remarks')}</Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

export const BehaviorListScreen: React.FC = () => {
  const { t } = useI18n();
  const { user } = useAuthStore();
  const { width } = useWindowDimensions();
  const isDesktop = width >= 768;

  const [guidelineModal, setGuidelineModal] = useState<{ visible: boolean; title: string; guideline: string }>({
    visible: false,
    title: '',
    guideline: '',
  });

  const [remarksModal, setRemarksModal] = useState<{ visible: boolean; title: string; remarks: string }>({
    visible: false,
    title: '',
    remarks: '',
  });

  const { data: evaluations, isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['behavior-evaluations', user?.id],
    queryFn: () => evaluationsApi.list({ userId: user?.id, committed: false }),
    enabled: !!user,
  });

  const totalPoints = evaluations?.reduce((sum, item) => sum + item.currentPoints, 0) || 0;

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <Text style={styles.title}>
            <MaterialIcons name="favorite" size={24} color="#667eea" /> {t('child.behaviors.title')}
          </Text>
          <Text style={styles.subtitle}>{t('child.behaviors.subtitle')}</Text>
        </View>
      </View>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#667eea" />
        </View>
      ) : evaluations && evaluations.length > 0 ? (
        <ScrollView
          style={styles.content}
          refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} />}
        >
          {isDesktop && (
            <View style={styles.table}>
              <View style={styles.tableHeader}>
                <View style={[styles.tableHeaderCell, { flex: 2 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.rule')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { flex: 1 }]}>
                  <Text style={styles.tableHeaderText}>{t('behavior.progress')}</Text>
                </View>
                <View style={[styles.tableHeaderCell, { width: 60 }]}>
                  <Text style={styles.tableHeaderText}></Text>
                </View>
              </View>
              {evaluations.map((evaluation) => (
                <BehaviorCard
                  key={evaluation.id}
                  evaluation={evaluation}
                  isDesktop={isDesktop}
                  onShowGuideline={() =>
                    setGuidelineModal({
                      visible: true,
                      title: evaluation.behavior.title,
                      guideline: evaluation.behavior.guideline,
                    })
                  }
                  onShowRemarks={() =>
                    setRemarksModal({
                      visible: true,
                      title: evaluation.behavior.title,
                      remarks: evaluation.remarks || '',
                    })
                  }
                />
              ))}
            </View>
          )}

          {!isDesktop && (
            <View style={styles.cardList}>
              {evaluations.map((evaluation) => (
                <BehaviorCard
                  key={evaluation.id}
                  evaluation={evaluation}
                  isDesktop={isDesktop}
                  onShowGuideline={() =>
                    setGuidelineModal({
                      visible: true,
                      title: evaluation.behavior.title,
                      guideline: evaluation.behavior.guideline,
                    })
                  }
                  onShowRemarks={() =>
                    setRemarksModal({
                      visible: true,
                      title: evaluation.behavior.title,
                      remarks: evaluation.remarks || '',
                    })
                  }
                />
              ))}
            </View>
          )}

          <View style={styles.totalContainer}>
            <Text style={styles.totalText}>
              {t('child.behaviors.points.total')}: <Text style={styles.totalValue}>{totalPoints}</Text>{' '}
              {t('behavior.points')}
            </Text>
          </View>
        </ScrollView>
      ) : (
        <View style={styles.emptyContainer}>
          <MaterialIcons name="favorite-border" size={64} color="#ccc" />
          <Text style={styles.emptyTitle}>{t('child.behaviors.no.behaviors')}</Text>
          <Text style={styles.emptyText}>{t('child.behaviors.no.behaviors.desc')}</Text>
        </View>
      )}

      <GuidelineModal
        visible={guidelineModal.visible}
        title={guidelineModal.title}
        guideline={guidelineModal.guideline}
        onClose={() => setGuidelineModal({ visible: false, title: '', guideline: '' })}
      />

      <RemarksModal
        visible={remarksModal.visible}
        title={remarksModal.title}
        remarks={remarksModal.remarks}
        onClose={() => setRemarksModal({ visible: false, title: '', remarks: '' })}
      />
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
  // Desktop Table
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
  behaviorTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#667eea',
  },
  // Mobile Cards
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
    color: '#667eea',
  },
  cardContent: {
    gap: 8,
  },
  cardLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
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
  },
  remarksButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 12,
    padding: 12,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
  },
  remarksButtonText: {
    fontSize: 14,
    color: '#667eea',
    fontWeight: '500',
  },
  totalContainer: {
    backgroundColor: '#fff',
    margin: 16,
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
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
  totalText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
  },
  totalValue: {
    fontSize: 24,
    fontWeight: '700',
    color: '#667eea',
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
    color: '#666',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
  },
  // Modals
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  modalContent: {
    backgroundColor: '#fff',
    borderRadius: 16,
    width: '100%',
    maxWidth: 500,
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
  guidelineText: {
    fontSize: 16,
    color: '#333',
    lineHeight: 24,
  },
  remarksLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
    marginBottom: 8,
  },
  remarksText: {
    fontSize: 16,
    color: '#333',
    lineHeight: 24,
  },
  modalFooter: {
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
  },
  modalButton: {
    backgroundColor: '#667eea',
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
