import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { 
  useTaskDefinition, 
  useCreateTaskDefinition, 
  useUpdateTaskDefinition, 
  useDeleteTaskDefinition,
  useChildren 
} from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';
import { TasksStackParamList } from '../../navigation/types';
import { RecurrenceType } from '../../types/api';

type TaskEditRouteProp = RouteProp<TasksStackParamList, 'TaskEdit'>;

export const TaskDefinitionEditScreen: React.FC = () => {
  const navigation = useNavigation();
  const route = useRoute<TaskEditRouteProp>();
  const { t } = useI18n();
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
  const [assignedUserIds, setAssignedUserIds] = useState<number[]>([]);
  const [showRecurrenceOptions, setShowRecurrenceOptions] = useState(false);

  useEffect(() => {
    if (task) {
      setTitle(task.title);
      setDescription(task.description || '');
      setBasePoints(task.basePoints.toString());
      setRecurrenceType(task.recurrenceType);
      setAssignedUserIds(task.assignedUsers.map(u => u.id));
    }
  }, [task]);

  const handleSave = () => {
    if (!title) {
      Alert.alert(t('error.title'), t('tasks.error.title_required'));
      return;
    }
    if (assignedUserIds.length === 0) {
      Alert.alert(t('error.title'), t('tasks.error.user_required'));
      return;
    }

    const taskData = {
      title,
      description,
      basePoints: parseInt(basePoints, 10),
      recurrenceType,
      assignedUserIds,
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
    Alert.alert(
      t('tasks.delete.title'),
      t('tasks.delete.confirm'),
      [
        { text: t('button.cancel'), style: 'cancel' },
        {
          text: t('button.delete'),
          style: 'destructive',
          onPress: () => {
            deleteMutation.mutate(taskId!, { onSuccess: () => navigation.goBack() });
          },
        },
      ]
    );
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

  const recurrenceOptions: RecurrenceType[] = ['ONCE', 'DAILY', 'WEEKLY', 'MONTHLY'];

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.section}>
          <Text style={styles.label}>{t('tasks.title')}</Text>
          <TextInput
            style={styles.input}
            value={title}
            onChangeText={setTitle}
            placeholder={t('tasks.title.placeholder')}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>{t('tasks.description')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={description}
            onChangeText={setDescription}
            placeholder={t('tasks.description.placeholder')}
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
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
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
    color: '#333',
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#f9f9f9',
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
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    backgroundColor: '#f9f9f9',
  },
  selectButtonText: {
    fontSize: 16,
    color: '#333',
  },
  optionsContainer: {
    marginTop: 4,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    backgroundColor: '#fff',
    overflow: 'hidden',
  },
  optionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  optionItemSelected: {
    backgroundColor: '#e3f2fd',
  },
  optionText: {
    fontSize: 16,
    color: '#333',
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
    color: '#666',
    fontWeight: '500',
  },
  userChipTextSelected: {
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
