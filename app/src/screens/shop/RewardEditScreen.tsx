import { useNavigation, useRoute } from '@react-navigation/native';
import { useQuery } from '@tanstack/react-query';
import * as ImagePicker from 'expo-image-picker';
import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  Platform,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { API_CONFIG, getApiBaseUrl } from '../../api/config';
import { rewardsApi, usersApi } from '../../api/services';
import {
  useCreateReward,
  useDeactivateReward,
  useReward,
  useUpdateReward
} from '../../hooks/useApi';
import { useDialogs } from '../../hooks/useDialogs';
import { useI18n } from '../../i18n/I18nContext';

export const RewardEditScreen: React.FC = () => {
  const navigation = useNavigation();
  const route = useRoute<any>();
  const { t } = useI18n();
  const { showError, showConfirm, Dialogs } = useDialogs();
  const theme = useTheme();
  const styles = createStyles(theme);
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
  const [userId, setUserId] = useState<number | undefined>(undefined);
  const [localImageUri, setLocalImageUri] = useState<string | undefined>(undefined);
  const [isUploadingImage, setIsUploadingImage] = useState(false);
  const [serverUrl, setServerUrl] = useState(API_CONFIG.baseUrl);

  const { data: children } = useQuery({
    queryKey: ['children'],
    queryFn: () => usersApi.listChildren(),
  });

  useEffect(() => {
    getApiBaseUrl().then(setServerUrl);
  }, []);

  useEffect(() => {
    if (reward) {
      setTitle(reward.title);
      setDescription(reward.description || '');
      setPointsCost(reward.pointsCost.toString());
      setActive(reward.active);
      setUserId(reward.userId ?? undefined);
    }
  }, [reward]);

  const handleSave = () => {
    if (!title) {
      showError(t('rewards.error.title_required'), t('error.title'));
      return;
    }
    const cost = parseInt(pointsCost, 10);
    if (isNaN(cost) || cost < 0) {
      showError(t('rewards.error.points_invalid'), t('error.title'));
      return;
    }

    const rewardData = {
      title,
      description,
      pointsCost: cost,
      userId: isEditing ? (userId ?? 0) : (userId ?? undefined),
    };

    const afterSave = async (savedId: number) => {
      if (localImageUri) {
        setIsUploadingImage(true);
        try {
          await rewardsApi.uploadImage(savedId, localImageUri);
        } catch {
          // Image upload failed but reward was saved — continue
        } finally {
          setIsUploadingImage(false);
        }
      }
      navigation.goBack();
    };

    if (isEditing) {
      updateMutation.mutate(
        { id: rewardId, data: { ...rewardData, active } },
        { onSuccess: () => afterSave(rewardId) }
      );
    } else {
      createMutation.mutate(rewardData, { onSuccess: (newReward) => afterSave(newReward.id) });
    }
  };

  const pickImage = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      quality: 0.8,
    });
    if (!result.canceled && result.assets.length > 0) {
      setLocalImageUri(result.assets[0].uri);
    }
  };

  const takePhoto = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert(t('error.title'), t('rewards.image.permission_denied'));
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: true,
      quality: 0.8,
    });
    if (!result.canceled && result.assets.length > 0) {
      setLocalImageUri(result.assets[0].uri);
    }
  };

  const handleSelectImage = () => {
    if (Platform.OS === 'android') {
      Alert.alert(
        t('rewards.image.select_title'),
        undefined,
        [
          { text: t('rewards.image.from_gallery'), onPress: pickImage },
          { text: t('rewards.image.take_photo'), onPress: takePhoto },
          { text: t('button.cancel'), style: 'cancel' },
        ]
      );
    } else {
      pickImage();
    }
  };

  const handleDeactivate = () => {
    showConfirm({
      title: t('rewards.deactivate.title'),
      message: t('rewards.deactivate.confirm'),
      onConfirm: () => deactivateMutation.mutate(rewardId!, { onSuccess: () => navigation.goBack() }),
      confirmText: t('button.deactivate'),
      cancelText: t('button.cancel'),
      destructive: true,
    });
  };

  const isLoading = isLoadingReward || createMutation.isPending || updateMutation.isPending || deactivateMutation.isPending || isUploadingImage;

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
          <Text style={styles.label}>{t('reward.name')}</Text>
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

        <View style={styles.section}>
          <Text style={styles.label}>{t('rewards.child.label')}</Text>
          <View style={styles.childSelector}>
            <TouchableOpacity
              style={[styles.childOption, userId === undefined && styles.childOptionActive]}
              onPress={() => setUserId(undefined)}
            >
              <Text style={[styles.childOptionText, userId === undefined && styles.childOptionTextActive]}>
                {t('children.all')}
              </Text>
            </TouchableOpacity>
            {(children ?? []).map((child) => (
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

        <View style={styles.section}>
          <Text style={styles.label}>{t('rewards.image.label')}</Text>
          {(localImageUri || (isEditing && reward?.hasImage)) ? (
            <View style={styles.imagePreviewContainer}>
              <Image
                source={{
                  uri: localImageUri ?? `${serverUrl}/api/v1/rewards/${rewardId}/image`,
                }}
                style={styles.imagePreview}
                resizeMode="cover"
              />
              <TouchableOpacity style={styles.changeImageButton} onPress={handleSelectImage}>
                <Text style={styles.changeImageButtonText}>{t('rewards.image.change')}</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <TouchableOpacity style={styles.imagePickerButton} onPress={handleSelectImage}>
              <Text style={styles.imagePickerIcon}>📷</Text>
              <Text style={styles.imagePickerText}>{t('rewards.image.add')}</Text>
            </TouchableOpacity>
          )}
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
  childSelector: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  childOption: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#ccc',
    backgroundColor: '#f5f5f5',
  },
  childOptionActive: {
    backgroundColor: '#9C27B0',
    borderColor: '#9C27B0',
  },
  childOptionText: {
    fontSize: 14,
    color: '#333',
  },
  childOptionTextActive: {
    color: '#fff',
    fontWeight: '600',
  },
  imagePickerButton: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderStyle: 'dashed',
    borderRadius: 8,
    height: 120,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fafafa',
    gap: 8,
  },
  imagePickerIcon: {
    fontSize: 32,
  },
  imagePickerText: {
    color: '#666',
    fontSize: 14,
  },
  imagePreviewContainer: {
    gap: 8,
  },
  imagePreview: {
    width: '100%',
    height: 180,
    borderRadius: 8,
  },
  changeImageButton: {
    alignSelf: 'flex-end',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#9C27B0',
  },
  changeImageButtonText: {
    color: '#9C27B0',
    fontSize: 13,
    fontWeight: '600',
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
