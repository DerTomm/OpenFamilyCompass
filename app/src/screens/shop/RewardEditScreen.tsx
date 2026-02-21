import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { 
  useReward, 
  useCreateReward, 
  useUpdateReward, 
  useDeactivateReward 
} from '../../hooks/useApi';
import { useI18n } from '../../i18n/I18nContext';

export const RewardEditScreen: React.FC = () => {
  const navigation = useNavigation();
  const route = useRoute<any>();
  const { t } = useI18n();
  // route.params can be undefined if navigated via RewardCreate alias
  const rewardId = route.params?.rewardId; 
  const isEditing = !!rewardId;

  const { data: reward, isLoading: isLoadingReward } = useReward(rewardId!);

  const createMutation = useCreateReward();
  const updateMutation = useUpdateReward();
  const deactivateMutation = useDeactivateReward();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [pointsCost, setPointsCost] = useState('50');
  const [active, setActive] = useState(true);

  useEffect(() => {
    if (reward) {
      setTitle(reward.title);
      setDescription(reward.description || '');
      setPointsCost(reward.pointsCost.toString());
      setActive(reward.active);
    }
  }, [reward]);

  const handleSave = () => {
    if (!title) {
      Alert.alert(t('error.title'), t('rewards.error.title_required'));
      return;
    }
    const cost = parseInt(pointsCost, 10);
    if (isNaN(cost) || cost < 0) {
      Alert.alert(t('error.title'), t('rewards.error.points_invalid'));
      return;
    }

    const rewardData = {
      title,
      description,
      pointsCost: cost,
    };

    if (isEditing) {
      updateMutation.mutate(
        { id: rewardId, data: { ...rewardData, active } },
        { onSuccess: () => navigation.goBack() }
      );
    } else {
      createMutation.mutate(rewardData, { onSuccess: () => navigation.goBack() });
    }
  };

  const handleDeactivate = () => {
    Alert.alert(
      t('rewards.deactivate.title'),
      t('rewards.deactivate.confirm'),
      [
        { text: t('button.cancel'), style: 'cancel' },
        {
          text: t('button.deactivate'),
          style: 'destructive',
          onPress: () => {
            deactivateMutation.mutate(rewardId!, { onSuccess: () => navigation.goBack() });
          },
        },
      ]
    );
  };

  const isLoading = isLoadingReward || createMutation.isPending || updateMutation.isPending || deactivateMutation.isPending;

  if (isEditing && isLoadingReward) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#9C27B0" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.section}>
          <Text style={styles.label}>{t('rewards.title')}</Text>
          <TextInput
            style={styles.input}
            value={title}
            onChangeText={setTitle}
            placeholder={t('rewards.title.placeholder')}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>{t('rewards.description')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={description}
            onChangeText={setDescription}
            placeholder={t('rewards.description.placeholder')}
            multiline
            numberOfLines={3}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>{t('rewards.cost')}</Text>
          <TextInput
            style={styles.input}
            value={pointsCost}
            onChangeText={setPointsCost}
            keyboardType="numeric"
            placeholder="0"
          />
        </View>

        {isEditing && (
          <View style={styles.section}>
            <View style={styles.switchRow}>
              <Text style={styles.label}>{t('status.active')}</Text>
              <Switch
                value={active}
                onValueChange={setActive}
                trackColor={{ false: '#ccc', true: '#E1BEE7' }}
                thumbColor={active ? '#9C27B0' : '#f4f3f4'}
              />
            </View>
          </View>
        )}

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

          {isEditing && active && (
            <TouchableOpacity
              style={styles.deleteButton}
              onPress={handleDeactivate}
              disabled={isLoading}
            >
              <Text style={styles.deleteButtonText}>{t('button.deactivate')}</Text>
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
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  buttonContainer: {
    marginTop: 20,
    gap: 12,
  },
  saveButton: {
    backgroundColor: '#9C27B0',
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
