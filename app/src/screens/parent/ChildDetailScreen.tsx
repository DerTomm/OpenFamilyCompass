import { MaterialCommunityIcons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { LinearGradient } from 'expo-linear-gradient';
import React, { useState } from 'react';
import {
  Dimensions,
  Modal,
  ScrollView,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { ActivityIndicator, Button, Card, Divider, SegmentedButtons, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { pointsApi } from '../../api/services';
import { ErrorDialog, SuccessDialog } from '../../components/ui';
import { useChildren, usePendingRedemptions, usePendingTasks, usePointTransactions } from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { ActivitiesStackParamList } from '../../navigation/types';
import { spacing } from '../../theme/theme';

type RouteParams = RouteProp<ActivitiesStackParamList, 'ChildDetail'>;
type NavigationProp = NativeStackNavigationProp<ActivitiesStackParamList>;

export const ChildDetailScreen: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  const route = useRoute<RouteParams>();
  const navigation = useNavigation<NavigationProp>();
  const queryClient = useQueryClient();
  const { childId } = route.params;

  const { data: children } = useChildren();
  const child = children?.find((c) => c.id === childId);
  const { data: transactionsData, isLoading } = usePointTransactions({ userId: childId, limit: 500 });
  const { data: pendingTasks = [] } = usePendingTasks();
  const { data: pendingRedemptionsData = [] } = usePendingRedemptions();
  const transactions = transactionsData?.transactions || [];
  const pendingApprovalsCount = pendingTasks.filter((task) => task.assignedUser.id === childId).length;
  const pendingRedemptionsCount = pendingRedemptionsData.filter((r) => r.user.id === childId).length;

  // All useState hooks first
  const [showPointsModal, setShowPointsModal] = useState(false);
  const [pointsAmount, setPointsAmount] = useState('');
  const [pointsDescription, setPointsDescription] = useState('');
  const [successDialogVisible, setSuccessDialogVisible] = useState(false);
  const [errorDialogVisible, setErrorDialogVisible] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [timeRange, setTimeRange] = useState<'week' | 'month' | '6months' | 'all'>('month');
  const [chartMode, setChartMode] = useState<'cumulative' | 'delta'>('cumulative');

  // Derived values after state declarations
  const now = new Date();
  const timeFilters: Record<string, Date> = {
    week: new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000),
    month: new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000),
    '6months': new Date(now.getTime() - 180 * 24 * 60 * 60 * 1000),
    all: new Date(0),
  };
  const filteredTransactions = transactions
    .slice()
    .reverse()
    .filter((tx) => new Date(tx.createdAt) >= timeFilters[timeRange]);

  const addPointsMutation = useMutation({
    mutationFn: (data: { userId: number; points: number; description: string; type: 'BONUS' | 'PENALTY' }) =>
      pointsApi.addPoints(data.userId, data.points, data.type, data.description),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['points', childId] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['children'] });
      setShowPointsModal(false);
      setPointsAmount('');
      setPointsDescription('');
      setSuccessDialogVisible(true);
    },
    onError: () => {
      setErrorMessage(t('common.error'));
      setErrorDialogVisible(true);
    },
  });

  const handleAddPoints = (isBonus: boolean) => {
    const amount = parseInt(pointsAmount, 10);
    if (isNaN(amount) || amount === 0) {
      setErrorMessage(t('points.validation.amount'));
      setErrorDialogVisible(true);
      return;
    }
    if (!pointsDescription.trim()) {
      setErrorMessage(t('points.validation.description'));
      setErrorDialogVisible(true);
      return;
    }

    addPointsMutation.mutate({
      userId: childId,
      points: isBonus ? Math.abs(amount) : -Math.abs(amount),
      description: pointsDescription,
      type: isBonus ? 'BONUS' : 'PENALTY',
    });
  };

  // Calculate chart data (filteredTransactions is already chronologically sorted oldest→newest)
  const chartData = chartMode === 'cumulative'
    ? filteredTransactions.map((t, idx) => {
      const runningBalance = filteredTransactions
        .slice(0, idx + 1)
        .reduce((sum, tx) => sum + tx.points, 0);
      return { date: new Date(t.createdAt), balance: runningBalance };
    })
    : filteredTransactions.map((t) => ({ date: new Date(t.createdAt), balance: t.points }));

  const renderChart = () => {
    return (
      <Card elevation={1} style={{ marginBottom: spacing.md }}>
        <Card.Content>
          <Text variant="titleMedium" style={{ marginBottom: spacing.md, fontWeight: '600' }}>
            Punkteverlauf
          </Text>

          {/* Time Range Selector */}
          <SegmentedButtons
            value={timeRange}
            onValueChange={(value) => setTimeRange(value as 'week' | 'month' | '6months' | 'all')}
            buttons={[
              { value: 'week', label: '1 Wo.' },
              { value: 'month', label: '1 Mon.' },
              { value: '6months', label: '6 Mon.' },
              { value: 'all', label: 'Alle' },
            ]}
            style={{ marginBottom: spacing.sm }}
          />

          {/* Chart Mode Selector */}
          <SegmentedButtons
            value={chartMode}
            onValueChange={(value) => setChartMode(value as 'cumulative' | 'delta')}
            buttons={[
              { value: 'cumulative', label: 'Entwicklung' },
              { value: 'delta', label: 'Plus-Minus' },
            ]}
            style={{ marginBottom: spacing.md }}
          />

          {chartData.length === 0 ? (
            <Text variant="bodyMedium" style={{ textAlign: 'center', color: theme.colors.onSurfaceVariant }}>
              Keine Daten für diesen Zeitraum
            </Text>
          ) : chartMode === 'delta' ? (
            // Bar chart for Plus-Minus mode
            (() => {
              const totalWidth = Dimensions.get('window').width - spacing.md * 4;
              const yAxisWidth = 42;
              const xAxisHeight = 18;
              const chartHeight = 182;
              const drawWidth = totalWidth - yAxisWidth;
              const maxAbs = Math.max(...chartData.map((d) => Math.abs(d.balance)), 1);
              const barAreaHeight = chartHeight / 2;
              const barWidth = Math.max(4, Math.min(20, (drawWidth - 8) / chartData.length - 2));
              const minLabelSpacing = 30;
              const barColWidth = drawWidth / chartData.length;
              const step = Math.max(1, Math.ceil(minLabelSpacing / barColWidth));
              const showLabel = (idx: number) => idx % step === 0 || idx === chartData.length - 1;
              const fmt = (d: Date) =>
                `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;

              return (
                <View style={{ flexDirection: 'row' }}>
                  {/* Y-axis */}
                  <View style={{ width: yAxisWidth, paddingRight: 4 }}>
                    <View style={{ height: chartHeight, justifyContent: 'space-between', alignItems: 'flex-end' }}>
                      <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant }}>+{maxAbs}</Text>
                      <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant }}>0</Text>
                      <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant }}>-{maxAbs}</Text>
                    </View>
                    <View style={{ height: xAxisHeight }} />
                  </View>
                  {/* Chart + X-axis */}
                  <View style={{ flex: 1 }}>
                    <View style={{ height: chartHeight, overflow: 'hidden' }}>
                      {/* Zero line */}
                      <View
                        style={{
                          position: 'absolute',
                          top: barAreaHeight,
                          left: 0,
                          right: 0,
                          height: 1,
                          backgroundColor: theme.colors.onSurfaceVariant,
                          opacity: 0.4,
                        }}
                      />
                      {/* Bars */}
                      <View style={{ flexDirection: 'row', height: chartHeight, paddingHorizontal: 4 }}>
                        {chartData.map((point, idx) => {
                          const isPositive = point.balance >= 0;
                          const barHeight = (Math.abs(point.balance) / maxAbs) * barAreaHeight;
                          return (
                            <View key={idx} style={{ flex: 1, height: chartHeight, alignItems: 'center', marginHorizontal: 1 }}>
                              {isPositive ? (
                                <View style={{ width: '100%', height: chartHeight, alignItems: 'center' }}>
                                  <View style={{ height: barAreaHeight, justifyContent: 'flex-end', width: '100%', alignItems: 'center' }}>
                                    <View style={{ width: barWidth, height: Math.max(2, barHeight), backgroundColor: '#4CAF50', borderRadius: 2 }} />
                                  </View>
                                  <View style={{ height: barAreaHeight }} />
                                </View>
                              ) : (
                                <View style={{ width: '100%', height: chartHeight, alignItems: 'center' }}>
                                  <View style={{ height: barAreaHeight }} />
                                  <View style={{ height: barAreaHeight, justifyContent: 'flex-start', width: '100%', alignItems: 'center' }}>
                                    <View style={{ width: barWidth, height: Math.max(2, barHeight), backgroundColor: '#F44336', borderRadius: 2 }} />
                                  </View>
                                </View>
                              )}
                            </View>
                          );
                        })}
                      </View>
                    </View>
                    {/* X-axis labels */}
                    <View style={{ height: xAxisHeight, flexDirection: 'row', paddingHorizontal: 4 }}>
                      {chartData.map((point, idx) => (
                        <View key={idx} style={{ flex: 1, alignItems: 'center' }}>
                          {showLabel(idx) && (
                            <Text style={{ fontSize: 9, color: theme.colors.onSurfaceVariant }} numberOfLines={1}>
                              {fmt(point.date)}
                            </Text>
                          )}
                        </View>
                      ))}
                    </View>
                  </View>
                </View>
              );
            })()
          ) : (
            // Line chart for cumulative mode
            (() => {
              const totalWidth = Dimensions.get('window').width - spacing.md * 4;
              const yAxisWidth = 42;
              const xAxisHeight = 18;
              const chartHeight = 182;
              const drawWidth = totalWidth - yAxisWidth - spacing.xs;
              const maxBalance = Math.max(...chartData.map((d) => d.balance), 0);
              const minBalance = Math.min(...chartData.map((d) => d.balance), 0);
              const range = maxBalance - minBalance || 1;
              const pointSpacing = chartData.length > 1 ? drawWidth / (chartData.length - 1) : drawWidth;
              const minLabelSpacing = 30;
              const step = Math.max(1, Math.ceil(minLabelSpacing / pointSpacing));
              const showLabel = (idx: number) => idx % step === 0 || idx === chartData.length - 1;
              const fmt = (d: Date) =>
                `${String(d.getDate()).padStart(2, '0')}.${String(d.getMonth() + 1).padStart(2, '0')}`;

              return (
                <View style={{ flexDirection: 'row' }}>
                  {/* Y-axis */}
                  <View style={{ width: yAxisWidth, paddingRight: 4 }}>
                    <View style={{ height: chartHeight, justifyContent: 'space-between', alignItems: 'flex-end' }}>
                      <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant }}>{maxBalance}</Text>
                      <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant }}>{Math.round((maxBalance + minBalance) / 2)}</Text>
                      <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant }}>{minBalance}</Text>
                    </View>
                    <View style={{ height: xAxisHeight }} />
                  </View>
                  {/* Chart + X-axis */}
                  <View style={{ flex: 1 }}>
                    <View style={{ height: chartHeight, overflow: 'hidden' }}>
                      {minBalance < 0 && maxBalance > 0 && (
                        <View style={[styles.zeroLine, { top: (maxBalance / range) * chartHeight, width: drawWidth }]} />
                      )}
                      {chartData.map((point, idx) => {
                        if (idx === 0) return null;
                        const prevPoint = chartData[idx - 1];
                        const x1 = (idx - 1) * pointSpacing;
                        const y1 = ((maxBalance - prevPoint.balance) / range) * chartHeight;
                        const x2 = idx * pointSpacing;
                        const y2 = ((maxBalance - point.balance) / range) * chartHeight;
                        return (
                          <View
                            key={idx}
                            style={{
                              position: 'absolute',
                              left: x1,
                              top: y1,
                              width: Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2),
                              height: 2,
                              backgroundColor: theme.colors.primary,
                              transform: [{ rotate: `${Math.atan2(y2 - y1, x2 - x1)}rad` }],
                              transformOrigin: '0 0',
                            }}
                          />
                        );
                      })}
                      {chartData.map((point, idx) => {
                        const x = idx * pointSpacing;
                        const y = ((maxBalance - point.balance) / range) * chartHeight;
                        return (
                          <View
                            key={`point-${idx}`}
                            style={[
                              styles.dataPoint,
                              {
                                left: x - 4,
                                top: Math.max(0, Math.min(chartHeight - 8, y - 4)),
                                backgroundColor: theme.colors.primary,
                              },
                            ]}
                          />
                        );
                      })}
                    </View>
                    {/* X-axis labels */}
                    <View style={{ height: xAxisHeight }}>
                      {chartData.map((point, idx) => {
                        if (!showLabel(idx)) return null;
                        const x = idx * pointSpacing;
                        return (
                          <Text
                            key={idx}
                            style={{
                              position: 'absolute',
                              left: x - 15,
                              top: 2,
                              fontSize: 9,
                              color: theme.colors.onSurfaceVariant,
                              width: 30,
                              textAlign: 'center',
                            }}
                          >
                            {fmt(point.date)}
                          </Text>
                        );
                      })}
                    </View>
                  </View>
                </View>
              );
            })()
          )}
        </Card.Content>
      </Card>
    );
  };

  if (!child) {
    return (
      <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]}>
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
          <Text variant="bodyLarge">Kind nicht gefunden</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]} edges={[]}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        {/* Header Card with Points */}
        <Card elevation={3} style={{ marginBottom: spacing.md, overflow: 'hidden' }}>
          <LinearGradient colors={[theme.colors.primary, theme.colors.secondary]} style={{ padding: spacing.lg }}>
            <View style={styles.headerContent}>
              <View>
                <Text variant="titleLarge" style={{ color: '#FFF', fontWeight: '700' }}>
                  {child.firstName}
                </Text>
                <Text variant="bodyMedium" style={{ color: 'rgba(255,255,255,0.9)', marginTop: spacing.xs }}>
                  {t('points.label')}
                </Text>
              </View>
              <View style={styles.pointsBadge}>
                <Text variant="displaySmall" style={{ color: '#FFF', fontWeight: '900' }}>
                  {child.totalPoints}
                </Text>
              </View>
            </View>
          </LinearGradient>
        </Card>

        {/* Chart */}
        {isLoading ? (
          <Card elevation={1} style={{ marginBottom: spacing.md }}>
            <Card.Content>
              <ActivityIndicator />
            </Card.Content>
          </Card>
        ) : (
          renderChart()
        )}

        {/* Action Buttons */}
        <Card elevation={1} style={{ marginBottom: spacing.md }}>
          <Card.Content>
            <Text variant="titleMedium" style={{ marginBottom: spacing.md, fontWeight: '600' }}>
              Aktionen
            </Text>

            <Button
              mode="contained"
              icon="star-circle"
              onPress={() => navigation.navigate('BehaviorEvaluate', { childId })}
              style={{ marginBottom: spacing.sm }}
              contentStyle={{ paddingVertical: spacing.xs }}
            >
              Verhalten bewerten
            </Button>

            <Button
              mode="outlined"
              icon="plus-circle"
              onPress={() => setShowPointsModal(true)}
              style={{ marginBottom: (pendingApprovalsCount > 0 || pendingRedemptionsCount > 0) ? spacing.sm : 0 }}
              contentStyle={{ paddingVertical: spacing.xs }}
            >
              Plus-/Minuspunkte vergeben
            </Button>

            {pendingApprovalsCount > 0 && (
              <Button
                mode="contained"
                icon="clipboard-check"
                onPress={() => navigation.navigate('PendingApproval')}
                style={{ marginBottom: pendingRedemptionsCount > 0 ? spacing.sm : 0 }}
                contentStyle={{ paddingVertical: spacing.xs }}
              >
                Aufgaben freigeben ({pendingApprovalsCount})
              </Button>
            )}

            {pendingRedemptionsCount > 0 && (
              <Button
                mode="contained"
                icon="gift"
                onPress={() => navigation.navigate('PendingRedemptions')}
                contentStyle={{ paddingVertical: spacing.xs }}
              >
                Belohnungen freigeben ({pendingRedemptionsCount})
              </Button>
            )}
          </Card.Content>
        </Card>

        {/* Recent Transactions */}
        <Card elevation={1}>
          <Card.Content>
            <Text variant="titleMedium" style={{ marginBottom: spacing.md, fontWeight: '600' }}>
              Letzte Aktivitäten
            </Text>
            {transactions.length > 0 ? (
              transactions.slice(0, 10).map((transaction, idx) => (
                <View key={transaction.id}>
                  <View style={styles.transactionRow}>
                    <View style={styles.transactionInfo}>
                      <Text variant="bodyMedium" style={{ fontWeight: '500' }}>
                        {transaction.description || t(`point.transaction.type.${transaction.type}`)}
                      </Text>
                      <Text variant="bodySmall" style={{ color: theme.colors.outline }}>
                        {new Date(transaction.createdAt).toLocaleDateString('de-DE')}
                      </Text>
                    </View>
                    <Text
                      variant="bodyLarge"
                      style={{
                        fontWeight: '600',
                        color:
                          transaction.points > 0
                            ? '#4CAF50'
                            : transaction.points < 0
                              ? '#F44336'
                              : theme.colors.onSurface,
                      }}
                    >
                      {transaction.points > 0 ? `+${transaction.points}` : transaction.points}
                    </Text>
                  </View>
                  {idx < Math.min(transactions.length, 10) - 1 && <Divider style={{ marginVertical: spacing.xs }} />}
                </View>
              ))
            ) : (
              <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant, textAlign: 'center' }}>
                Noch keine Aktivitäten
              </Text>
            )}
          </Card.Content>
        </Card>
      </ScrollView>

      {/* Points Modal */}
      <Modal visible={showPointsModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.colors.surface }]}>
            <View style={styles.modalHeader}>
              <Text variant="titleLarge" style={{ fontWeight: '600' }}>
                Punkte vergeben
              </Text>
              <TouchableOpacity onPress={() => setShowPointsModal(false)}>
                <MaterialCommunityIcons name="close" size={24} color={theme.colors.onSurface} />
              </TouchableOpacity>
            </View>

            <View style={styles.modalBody}>
              <Text variant="bodyMedium" style={{ marginBottom: spacing.sm }}>
                Betrag (positiv oder negativ):
              </Text>
              <TextInput
                style={[styles.input, { borderColor: theme.colors.outline, color: theme.colors.onSurface }]}
                value={pointsAmount}
                onChangeText={setPointsAmount}
                placeholder="z.B. 10 oder -5"
                keyboardType="numeric"
                placeholderTextColor={theme.colors.onSurfaceVariant}
              />

              <Text variant="bodyMedium" style={{ marginBottom: spacing.sm, marginTop: spacing.md }}>
                Begründung:
              </Text>
              <TextInput
                style={[
                  styles.input,
                  styles.textArea,
                  { borderColor: theme.colors.outline, color: theme.colors.onSurface },
                ]}
                value={pointsDescription}
                onChangeText={setPointsDescription}
                placeholder="z.B. Besonders geholfen im Haushalt"
                multiline
                numberOfLines={3}
                placeholderTextColor={theme.colors.onSurfaceVariant}
              />
            </View>

            <View style={styles.modalFooter}>
              <Button mode="outlined" onPress={() => setShowPointsModal(false)} style={{ flex: 1, marginRight: spacing.xs }}>
                Abbrechen
              </Button>
              <Button
                mode="contained"
                onPress={() => {
                  const amount = parseInt(pointsAmount, 10);
                  handleAddPoints(amount > 0);
                }}
                disabled={addPointsMutation.isPending}
                style={{ flex: 1, marginLeft: spacing.xs }}
              >
                {addPointsMutation.isPending ? 'Speichern...' : 'Speichern'}
              </Button>
            </View>
          </View>
        </View>
      </Modal>

      {/* Success Dialog */}
      <SuccessDialog
        visible={successDialogVisible}
        title={t('common.success')}
        message={t('points.added.success')}
        onDismiss={() => setSuccessDialogVisible(false)}
      />

      {/* Error Dialog */}
      <ErrorDialog
        visible={errorDialogVisible}
        title={t('common.error')}
        message={errorMessage}
        onDismiss={() => setErrorDialogVisible(false)}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.md,
  },
  headerContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  pointsBadge: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    borderRadius: 50,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
  },
  chartContainer: {
    flexDirection: 'row',
    alignItems: 'stretch',
  },
  yAxisLabels: {
    width: 40,
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    paddingRight: spacing.xs,
  },
  chartArea: {
    position: 'relative',
    marginLeft: spacing.xs,
    overflow: 'hidden',
    flexShrink: 1,
  },
  zeroLine: {
    position: 'absolute',
    height: 1,
    backgroundColor: '#999',
    opacity: 0.5,
  },
  dataPoint: {
    position: 'absolute',
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  transactionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  transactionInfo: {
    flex: 1,
    marginRight: spacing.sm,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    width: '90%',
    maxWidth: 500,
    borderRadius: 12,
    padding: spacing.md,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  modalBody: {
    marginBottom: spacing.md,
  },
  modalFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  input: {
    borderWidth: 1,
    borderRadius: 8,
    padding: spacing.sm,
    fontSize: 16,
  },
  textArea: {
    height: 80,
    textAlignVertical: 'top',
  },
});
