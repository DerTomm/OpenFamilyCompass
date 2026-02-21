import React, { useState } from 'react';
import {
  ScrollView,
  StyleSheet,
  View,
  Dimensions,
  Modal,
  TextInput,
  TouchableOpacity,
  Alert,
  Platform,
} from 'react-native';
import { ActivityIndicator, Button, Card, Text, useTheme, Divider } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useI18n } from '../../i18n/I18nContext';
import { spacing } from '../../theme/theme';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { ActivitiesStackParamList } from '../../navigation/types';
import { useChildren, usePointTransactions } from '../../hooks/useApi';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { pointsApi } from '../../api/services';
import { LinearGradient } from 'expo-linear-gradient';

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
  const { data: transactionsData, isLoading } = usePointTransactions({ userId: childId, limit: 30 });
  const transactions = transactionsData?.transactions || [];

  const [showPointsModal, setShowPointsModal] = useState(false);
  const [pointsAmount, setPointsAmount] = useState('');
  const [pointsDescription, setPointsDescription] = useState('');

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
      if (Platform.OS === 'web') {
        window.alert(t('common.success'));
      } else {
        Alert.alert(t('common.success'));
      }
    },
    onError: () => {
      Alert.alert(t('common.error'));
    },
  });

  const handleAddPoints = (isBonus: boolean) => {
    const amount = parseInt(pointsAmount, 10);
    if (isNaN(amount) || amount === 0) {
      Alert.alert(t('common.error'), 'Bitte gültigen Betrag eingeben');
      return;
    }
    if (!pointsDescription.trim()) {
      Alert.alert(t('common.error'), 'Bitte Begründung eingeben');
      return;
    }

    addPointsMutation.mutate({
      userId: childId,
      points: isBonus ? Math.abs(amount) : -Math.abs(amount),
      description: pointsDescription,
      type: isBonus ? 'BONUS' : 'PENALTY',
    });
  };

  // Calculate chart data
  const chartData = transactions
    .slice()
    .reverse()
    .map((t, idx) => {
      const runningBalance =
        transactions
          .slice()
          .reverse()
          .slice(0, idx + 1)
          .reduce((sum, tx) => sum + tx.points, 0);
      return { date: new Date(t.createdAt), balance: runningBalance };
    });

  const renderChart = () => {
    if (chartData.length === 0) {
      return (
        <Card elevation={1} style={{ marginBottom: spacing.md }}>
          <Card.Content>
            <Text variant="bodyMedium" style={{ textAlign: 'center', color: theme.colors.onSurfaceVariant }}>
              Noch keine Punktehistorie vorhanden
            </Text>
          </Card.Content>
        </Card>
      );
    }

    const maxBalance = Math.max(...chartData.map((d) => d.balance), 0);
    const minBalance = Math.min(...chartData.map((d) => d.balance), 0);
    const range = maxBalance - minBalance || 1;
    const chartHeight = 200;
    const chartWidth = Dimensions.get('window').width - spacing.md * 4;
    const pointSpacing = chartWidth / Math.max(chartData.length - 1, 1);

    return (
      <Card elevation={1} style={{ marginBottom: spacing.md }}>
        <Card.Content>
          <Text variant="titleMedium" style={{ marginBottom: spacing.md, fontWeight: '600' }}>
            Punkteverlauf (letzte 30 Transaktionen)
          </Text>
          <View style={[styles.chartContainer, { height: chartHeight }]}>
            {/* Y-axis labels */}
            <View style={styles.yAxisLabels}>
              <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {maxBalance}
              </Text>
              <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {Math.round((maxBalance + minBalance) / 2)}
              </Text>
              <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {minBalance}
              </Text>
            </View>

            {/* Chart area */}
            <View style={[styles.chartArea, { width: chartWidth }]}>
              {/* Zero line */}
              {minBalance < 0 && maxBalance > 0 && (
                <View
                  style={[
                    styles.zeroLine,
                    {
                      top: ((maxBalance - 0) / range) * chartHeight,
                      width: chartWidth,
                    },
                  ]}
                />
              )}

              {/* Line path */}
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

              {/* Data points */}
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
                        top: y - 4,
                        backgroundColor: theme.colors.primary,
                      },
                    ]}
                  />
                );
              })}
            </View>
          </View>
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
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]}>
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
              contentStyle={{ paddingVertical: spacing.xs }}
            >
              Plus-/Minuspunkte vergeben
            </Button>
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
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xl,
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
