import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { 
  useTaskDefinition, 
  useCreateTaskDefinition, 
  useUpdateTaskDefinition, 
  useDeleteTaskDefinition,
  useChildren 
} from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { ManageStackParamList } from '../../navigation/types';
import { MonthlyRecurrenceMode, RecurrenceType } from '../../types/api';
import { useDialogs } from '../../hooks/useDialogs';

type TaskEditRouteProp = RouteProp<ManageStackParamList, 'TaskEdit'>;

export const TaskDefinitionEditScreen: React.FC = () => {
  const navigation = useNavigation();
  const route = useRoute<TaskEditRouteProp>();
  const { t } = useI18n();
  const { showError, showSuccess, showConfirm, Dialogs } = useDialogs();
  const theme = useTheme();
  const styles = createStyles(theme);
  const taskId = route.params?.taskId;
  const isEditing = !!taskId;

  const { data: task, isLoading: isLoadingTask } = useTaskDefinition(taskId!);
  const { data: children } = useChildren();

  const createMutation = useCreateTaskDefinition();
  const updateMutation = useUpdateTaskDefinition();
  const deleteMutation = useDeleteTaskDefinition();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [basePoints, setBasePoints] = useState('10');
  const [recurrenceType, setRecurrenceType] = useState<RecurrenceType>('ONCE');
  const [deadline, setDeadline] = useState('');
  const [weeklyDays, setWeeklyDays] = useState<string[]>([]);
  const [monthlyMode, setMonthlyMode] = useState<MonthlyRecurrenceMode>('WEEKDAY_PATTERN');
  const [monthlyWeekNumber, setMonthlyWeekNumber] = useState('2');
  const [monthlyDayOfMonth, setMonthlyDayOfMonth] = useState('1');
  const [monthlyAdjustToLastDay, setMonthlyAdjustToLastDay] = useState(false);
  const [assignedUserIds, setAssignedUserIds] = useState<number[]>([]);
  const [showRecurrenceOptions, setShowRecurrenceOptions] = useState(false);

  useEffect(() => {
    if (task) {
      setTitle(task.title);
      setDescription(task.description || '');
      setBasePoints(task.basePoints.toString());
      setRecurrenceType(task.recurrenceType);
      setDeadline(task.endAt ? task.endAt.slice(0, 16) : (task.endDate ? `${task.endDate}T23:59` : ''));
      setWeeklyDays(task.weeklyDays || []);
      setMonthlyMode(task.monthlyMode || 'WEEKDAY_PATTERN');
      setMonthlyWeekNumber(task.monthlyWeekNumber?.toString() || '2');
      setMonthlyDayOfMonth(task.monthlyDayOfMonth?.toString() || '1');
      setMonthlyAdjustToLastDay(Boolean(task.monthlyAdjustToLastDay));
      setAssignedUserIds(task.assignedUsers.map(u => u.id));
    }
  }, [task]);

  const dayOptions = [
    { value: 'MONDAY', label: t('day.monday.short') },
    { value: 'TUESDAY', label: t('day.tuesday.short') },
    { value: 'WEDNESDAY', label: t('day.wednesday.short') },
    { value: 'THURSDAY', label: t('day.thursday.short') },
    { value: 'FRIDAY', label: t('day.friday.short') },
    { value: 'SATURDAY', label: t('day.saturday.short') },
    { value: 'SUNDAY', label: t('day.sunday.short') },
  ];

  const monthWeekOptions = [1, 2, 3, 4, 5];

  const toggleWeeklyDay = (day: string) => {
    setWeeklyDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    );
  };

  const formatDate = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const formatDateTimeForInput = (date: Date) => {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${formatDate(date)}T${hours}:${minutes}`;
  };

  const formatDateTimeForApi = (value: string) => {
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return undefined;
    }
    const hours = String(parsed.getHours()).padStart(2, '0');
    const minutes = String(parsed.getMinutes()).padStart(2, '0');
    return `${formatDate(parsed)}T${hours}:${minutes}:00`;
  };

  const parseDateTime = (value: string) => {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
  };

  const isValidDateTimeInput = (value: string) => !Number.isNaN(new Date(value).getTime());

  const formatDateTimeDisplay = (value: string) => {
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }
    return `${formatDate(parsed)} ${String(parsed.getHours()).padStart(2, '0')}:${String(parsed.getMinutes()).padStart(2, '0')}`;
  };

  const openDeadlinePicker = () => {
    if (Platform.OS === 'android') {
      const initial = deadline ? parseDateTime(deadline) : new Date();
      DateTimePickerAndroid.open({
        value: initial,
        mode: 'date',
        is24Hour: true,
        onChange: (event, selectedDate) => {
          if (event.type === 'set' && selectedDate) {
            DateTimePickerAndroid.open({
              value: initial,
              mode: 'time',
              is24Hour: true,
              onChange: (timeEvent, selectedTime) => {
                if (timeEvent.type === 'set' && selectedTime) {
                  const merged = new Date(selectedDate);
                  merged.setHours(selectedTime.getHours(), selectedTime.getMinutes(), 0, 0);
                  setDeadline(formatDateTimeForInput(merged));
                }
              },
            });
          }
        },
      });
      return;
    }

    if (Platform.OS === 'web') {
      const webDocument = (globalThis as any).document;
      if (!webDocument) {
        return;
      }

      const input = webDocument.createElement('input');
      input.type = 'datetime-local';
      input.value = deadline || formatDateTimeForInput(new Date());
      input.onchange = () => {
        if (input.value) {
          setDeadline(input.value);
        }
      };
      input.click();
    }
  };

  const handleSave = () => {
    if (!title) {
      showError(t('tasks.error.title_required'), t('error.title'));
      return;
    }
    if (assignedUserIds.length === 0) {
      showError(t('tasks.error.user_required'), t('error.title'));
      return;
    }

    if (recurrenceType === 'ONCE' && deadline && !isValidDateTimeInput(deadline)) {
      showError(t('tasks.error.deadline_invalid'), t('error.title'));
      return;
    }

    const endAt = recurrenceType === 'ONCE' && deadline ? formatDateTimeForApi(deadline) : undefined;

    if ((recurrenceType === 'WEEKLY' || (recurrenceType === 'MONTHLY' && monthlyMode === 'WEEKDAY_PATTERN')) && weeklyDays.length === 0) {
      showError(t('tasks.error.weekly_days_required'), t('error.title'));
      return;
    }

    if (recurrenceType === 'MONTHLY' && monthlyMode === 'WEEKDAY_PATTERN' && !monthlyWeekNumber) {
      showError(t('tasks.error.monthly_week_required'), t('error.title'));
      return;
    }

    if (recurrenceType === 'MONTHLY' && monthlyMode === 'DAY_OF_MONTH') {
      const parsedDay = parseInt(monthlyDayOfMonth, 10);
      if (Number.isNaN(parsedDay) || parsedDay < 1 || parsedDay > 31) {
        showError(t('tasks.error.monthly_day_required'), t('error.title'));
        return;
      }
    }

    const taskData = {
      title,
      description,
      basePoints: parseInt(basePoints, 10),
      recurrenceType,
      assignedUserIds,
      endDate: recurrenceType === 'ONCE' && endAt ? endAt.split('T')[0] : undefined,
      endAt,
      weeklyDays:
        recurrenceType === 'ONCE' || (recurrenceType === 'MONTHLY' && monthlyMode === 'DAY_OF_MONTH')
          ? undefined
          : weeklyDays,
      monthlyMode: recurrenceType === 'MONTHLY' ? monthlyMode : undefined,
      monthlyWeekNumber:
        recurrenceType === 'MONTHLY' && monthlyMode === 'WEEKDAY_PATTERN'
          ? parseInt(monthlyWeekNumber, 10)
          : undefined,
      monthlyDayOfMonth:
        recurrenceType === 'MONTHLY' && monthlyMode === 'DAY_OF_MONTH'
          ? parseInt(monthlyDayOfMonth, 10)
          : undefined,
      monthlyAdjustToLastDay:
        recurrenceType === 'MONTHLY' && monthlyMode === 'DAY_OF_MONTH'
          ? monthlyAdjustToLastDay
          : undefined,
    };

    if (isEditing) {
      updateMutation.mutate(
        { id: taskId, data: taskData },
        { onSuccess: () => navigation.goBack() }
      );
    } else {
      createMutation.mutate(taskData, { onSuccess: () => navigation.goBack() });
    }
  };

  const handleDelete = () => {
    showConfirm({
      title: t('tasks.delete.title'),
      message: t('tasks.delete.confirm'),
      onConfirm: () => deleteMutation.mutate(taskId!, { onSuccess: () => navigation.goBack() }),
      confirmText: t('button.delete'),
      cancelText: t('button.cancel'),
      destructive: true,
    });
  };

  const toggleUserSelection = (userId: number) => {
    setAssignedUserIds(prev => 
      prev.includes(userId) 
        ? prev.filter(id => id !== userId)
        : [...prev, userId]
    );
  };

  const isLoading = isLoadingTask || createMutation.isPending || updateMutation.isPending || deleteMutation.isPending;

  if (isEditing && isLoadingTask) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#2196F3" />
      </View>
    );
  }

  const recurrenceOptions: RecurrenceType[] = ['ONCE', 'WEEKLY', 'MONTHLY'];

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.section}>
          <Text style={styles.label}>{t('task.name')}</Text>
          <TextInput
            style={styles.input}
            value={title}
            onChangeText={setTitle}
            placeholder={t('tasks.title.placeholder')}
            placeholderTextColor={theme.colors.onSurfaceVariant}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>{t('tasks.description')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={description}
            onChangeText={setDescription}
            placeholder={t('tasks.description.placeholder')}
            placeholderTextColor={theme.colors.onSurfaceVariant}
            multiline
            numberOfLines={3}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>{t('tasks.points')}</Text>
          <TextInput
            style={styles.input}
            value={basePoints}
            onChangeText={setBasePoints}
            placeholderTextColor={theme.colors.onSurfaceVariant}
            keyboardType="numeric"
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>{t('tasks.recurrence')}</Text>
          <TouchableOpacity 
            style={styles.selectButton} 
            onPress={() => setShowRecurrenceOptions(!showRecurrenceOptions)}
          >
            <Text style={styles.selectButtonText}>{t(`recurrence.${recurrenceType}`)}</Text>
            <MaterialCommunityIcons name="chevron-down" size={24} color="#666" />
          </TouchableOpacity>
          
          {showRecurrenceOptions && (
            <View style={styles.optionsContainer}>
              {recurrenceOptions.map(type => (
                <TouchableOpacity
                  key={type}
                  style={[
                    styles.optionItem,
                    recurrenceType === type && styles.optionItemSelected
                  ]}
                  onPress={() => {
                    setRecurrenceType(type);
                    setShowRecurrenceOptions(false);
                  }}
                >
                  <Text style={[
                    styles.optionText,
                    recurrenceType === type && styles.optionTextSelected
                  ]}>
                    {t(`recurrence.${type}`)}
                  </Text>
                  {recurrenceType === type && (
                    <MaterialCommunityIcons name="check" size={20} color="#2196F3" />
                  )}
                </TouchableOpacity>
              ))}
            </View>
          )}
        </View>

        {recurrenceType === 'ONCE' && (
          <View style={styles.section}>
            <Text style={styles.label}>{t('tasks.deadline.optional')}</Text>
            {(Platform.OS === 'android' || Platform.OS === 'web') ? (
              <TouchableOpacity style={styles.selectButton} onPress={openDeadlinePicker}>
                <Text style={styles.selectButtonText}>
                  {deadline ? formatDateTimeDisplay(deadline) : t('tasks.deadline.placeholder')}
                </Text>
                <MaterialCommunityIcons name="calendar" size={20} color={theme.colors.onSurfaceVariant} />
              </TouchableOpacity>
            ) : (
              <TextInput
                style={styles.input}
                value={deadline}
                onChangeText={setDeadline}
                placeholder={t('tasks.deadline.placeholder')}
                placeholderTextColor={theme.colors.onSurfaceVariant}
                autoCapitalize="none"
              />
            )}
          </View>
        )}

        {recurrenceType === 'WEEKLY' && (
          <View style={styles.section}>
            <Text style={styles.label}>{t('tasks.weekly.days')}</Text>
            <View style={styles.daySelectionGrid}>
              {dayOptions.map((day) => (
                <TouchableOpacity
                  key={day.value}
                  style={[
                    styles.dayChip,
                    weeklyDays.includes(day.value) && styles.dayChipSelected,
                  ]}
                  onPress={() => toggleWeeklyDay(day.value)}
                >
                  <Text
                    style={[
                      styles.dayChipText,
                      weeklyDays.includes(day.value) && styles.dayChipTextSelected,
                    ]}
                  >
                    {day.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        )}

        {recurrenceType === 'MONTHLY' && (
          <View style={styles.section}>
            <Text style={styles.label}>{t('tasks.monthly.mode')}</Text>
            <View style={styles.daySelectionGrid}>
              <TouchableOpacity
                style={[styles.dayChip, monthlyMode === 'DAY_OF_MONTH' && styles.dayChipSelected]}
                onPress={() => setMonthlyMode('DAY_OF_MONTH')}
              >
                <Text style={[styles.dayChipText, monthlyMode === 'DAY_OF_MONTH' && styles.dayChipTextSelected]}>
                  {t('tasks.monthly.mode.day_of_month')}
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.dayChip, monthlyMode === 'WEEKDAY_PATTERN' && styles.dayChipSelected]}
                onPress={() => setMonthlyMode('WEEKDAY_PATTERN')}
              >
                <Text style={[styles.dayChipText, monthlyMode === 'WEEKDAY_PATTERN' && styles.dayChipTextSelected]}>
                  {t('tasks.monthly.mode.weekday_pattern')}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        {recurrenceType === 'MONTHLY' && monthlyMode === 'DAY_OF_MONTH' && (
          <>
            <View style={styles.section}>
              <Text style={styles.label}>{t('tasks.monthly.day_of_month')}</Text>
              <TextInput
                style={styles.input}
                value={monthlyDayOfMonth}
                onChangeText={setMonthlyDayOfMonth}
                placeholder={t('tasks.monthly.day_of_month.placeholder')}
                placeholderTextColor={theme.colors.onSurfaceVariant}
                keyboardType="numeric"
              />
            </View>

            <View style={styles.section}>
              <Text style={styles.label}>{t('tasks.monthly.day_missing_behavior')}</Text>
              <View style={styles.daySelectionGrid}>
                <TouchableOpacity
                  style={[styles.dayChip, !monthlyAdjustToLastDay && styles.dayChipSelected]}
                  onPress={() => setMonthlyAdjustToLastDay(false)}
                >
                  <Text style={[styles.dayChipText, !monthlyAdjustToLastDay && styles.dayChipTextSelected]}>
                    {t('tasks.monthly.day_missing.skip')}
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.dayChip, monthlyAdjustToLastDay && styles.dayChipSelected]}
                  onPress={() => setMonthlyAdjustToLastDay(true)}
                >
                  <Text style={[styles.dayChipText, monthlyAdjustToLastDay && styles.dayChipTextSelected]}>
                    {t('tasks.monthly.day_missing.last_day')}
                  </Text>
                </TouchableOpacity>
              </View>
            </View>
          </>
        )}

        {recurrenceType === 'MONTHLY' && monthlyMode === 'WEEKDAY_PATTERN' && (
          <>
            <View style={styles.section}>
              <Text style={styles.label}>{t('tasks.weekly.days')}</Text>
              <View style={styles.daySelectionGrid}>
                {dayOptions.map((day) => (
                  <TouchableOpacity
                    key={day.value}
                    style={[
                      styles.dayChip,
                      weeklyDays.includes(day.value) && styles.dayChipSelected,
                    ]}
                    onPress={() => toggleWeeklyDay(day.value)}
                  >
                    <Text
                      style={[
                        styles.dayChipText,
                        weeklyDays.includes(day.value) && styles.dayChipTextSelected,
                      ]}
                    >
                      {day.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>

            <View style={styles.section}>
              <Text style={styles.label}>{t('tasks.monthly.week_number')}</Text>
              <View style={styles.daySelectionGrid}>
                {monthWeekOptions.map((week) => (
                  <TouchableOpacity
                    key={week}
                    style={[
                      styles.dayChip,
                      monthlyWeekNumber === week.toString() && styles.dayChipSelected,
                    ]}
                    onPress={() => setMonthlyWeekNumber(week.toString())}
                  >
                    <Text
                      style={[
                        styles.dayChipText,
                        monthlyWeekNumber === week.toString() && styles.dayChipTextSelected,
                      ]}
                    >
                      {t('tasks.monthly.week_number.value', { 0: week.toString() })}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          </>
        )}

        <View style={styles.section}>
          <Text style={styles.label}>{t('tasks.assigned_to')}</Text>
          <View style={styles.userList}>
            {children?.map(child => (
              <TouchableOpacity
                key={child.id}
                style={[
                  styles.userChip,
                  assignedUserIds.includes(child.id) && styles.userChipSelected
                ]}
                onPress={() => toggleUserSelection(child.id)}
              >
                <Text style={[
                  styles.userChipText,
                  assignedUserIds.includes(child.id) && styles.userChipTextSelected
                ]}>
                  {child.firstName}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.buttonContainer}>
          <TouchableOpacity
            style={styles.saveButton}
            onPress={handleSave}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.saveButtonText}>{t('button.save')}</Text>
            )}
          </TouchableOpacity>

          {isEditing && (
            <TouchableOpacity
              style={styles.deleteButton}
              onPress={handleDelete}
              disabled={isLoading}
            >
              <Text style={styles.deleteButtonText}>{t('button.delete')}</Text>
            </TouchableOpacity>
          )}
        </View>
      </ScrollView>

      <Dialogs />
    </SafeAreaView>
  );
};

const createStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    padding: 20,
  },
  section: {
    marginBottom: 20,
  },
  label: {
    fontSize: 16,
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
    color: theme.colors.onSurface,
    backgroundColor: theme.colors.surface,
  },
  textArea: {
    height: 100,
    textAlignVertical: 'top',
  },
  selectButton: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: theme.colors.outline,
    borderRadius: 8,
    padding: 12,
    backgroundColor: theme.colors.surface,
  },
  selectButtonText: {
    fontSize: 16,
    color: theme.colors.onSurface,
  },
  optionsContainer: {
    marginTop: 4,
    borderWidth: 1,
    borderColor: theme.colors.outline,
    borderRadius: 8,
    backgroundColor: theme.colors.surface,
    overflow: 'hidden',
  },
  optionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.outlineVariant,
  },
  optionItemSelected: {
    backgroundColor: '#e3f2fd',
  },
  optionText: {
    fontSize: 16,
    color: theme.colors.onSurface,
  },
  optionTextSelected: {
    color: '#2196F3',
    fontWeight: '600',
  },
  userList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  userChip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: 'transparent',
  },
  userChipSelected: {
    backgroundColor: '#e3f2fd',
    borderColor: '#2196F3',
  },
  userChipText: {
    color: theme.colors.onSurfaceVariant,
    fontWeight: '500',
  },
  userChipTextSelected: {
    color: '#2196F3',
    fontWeight: '600',
  },
  daySelectionGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  dayChip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    backgroundColor: theme.colors.surfaceVariant,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  dayChipSelected: {
    backgroundColor: '#e3f2fd',
    borderColor: '#2196F3',
  },
  dayChipText: {
    color: theme.colors.onSurfaceVariant,
    fontWeight: '500',
    fontSize: 13,
  },
  dayChipTextSelected: {
    color: '#2196F3',
    fontWeight: '600',
  },
  buttonContainer: {
    marginTop: 20,
    gap: 12,
  },
  saveButton: {
    backgroundColor: '#2196F3',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  deleteButton: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#F44336',
  },
  deleteButtonText: {
    color: '#F44336',
    fontSize: 16,
    fontWeight: '600',
  },
});
