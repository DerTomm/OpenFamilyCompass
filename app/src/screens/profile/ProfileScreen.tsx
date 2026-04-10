import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import React, { useState } from 'react';
import {
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  Button,
  Dialog,
  Divider,
  HelperText,
  List,
  Portal,
  RadioButton,
  Snackbar,
  Surface,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { profileApi, usersApi } from '../../api/services';
import { AvatarPicker, UserAvatar } from '../../components/ui';
import { Avatar as AvatarType } from '../../constants/avatars';
import { queryKeys, usePointTransactions } from '../../hooks/useApi';
import { useDialogs } from '../../hooks/useDialogs';
import { useI18n } from '../../i18n/I18nContext';
import { selectIsChild, useAuthStore } from '../../store/authStore';
import { useTheme as useAppTheme } from '../../theme/ThemeContext';
import { spacing } from '../../theme/theme';

export const ProfileScreen: React.FC = () => {
  const queryClient = useQueryClient();
  const { user, logout, fetchUser } = useAuthStore();
  const bumpAvatarVersion = useAuthStore((state) => state.bumpAvatarVersion);
  const isChild = useAuthStore(selectIsChild);
  const theme = useTheme();
  const { isDark, toggleTheme } = useAppTheme();
  const { t, language, setLanguage, availableLanguages } = useI18n();
  const { showConfirm, Dialogs } = useDialogs();

  const [languageDialogVisible, setLanguageDialogVisible] = useState(false);
  const [usernameDialogVisible, setUsernameDialogVisible] = useState(false);
  const [firstNameDialogVisible, setFirstNameDialogVisible] = useState(false);
  const [passwordDialogVisible, setPasswordDialogVisible] = useState(false);
  const [avatarPickerVisible, setAvatarPickerVisible] = useState(false);
  const [avatarRefreshKey, setAvatarRefreshKey] = useState(0);
  const [snackbarVisible, setSnackbarVisible] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarType, setSnackbarType] = useState<'success' | 'error'>('success');

  const [newUsername, setNewUsername] = useState('');
  const [newFirstName, setNewFirstName] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const { data: transactions, isLoading } = usePointTransactions({
    userId: user?.id,
    limit: 5
  });

  const updateProfileMutation = useMutation({
    mutationFn: profileApi.update,
    onSuccess: async () => {
      await fetchUser();
      await queryClient.invalidateQueries({ queryKey: queryKeys.children });
      setFirstNameDialogVisible(false);
      setSnackbarMessage(t('profile.update.success'));
      setSnackbarType('success');
      setSnackbarVisible(true);
    },
    onError: (error: any) => {
      console.error('Profile update error - Status:', error?.response?.status);
      console.error('Profile update error - Message:', error?.response?.data?.message);

      const status = error?.response?.status;
      const backendMessage = error?.response?.data?.message;
      const errorMessage = error?.message;

      let message = t('common.error');

      if (backendMessage) {
        message = backendMessage;
      } else if (errorMessage) {
        message = errorMessage;
      }

      console.log('Showing snackbar with message:', message);
      setSnackbarMessage(message);
      setSnackbarType('error');
      setSnackbarVisible(true);
    },
  });

  const updateUsernameMutation = useMutation({
    mutationFn: profileApi.update,
    onSuccess: async () => {
      setUsernameDialogVisible(false);
      setSnackbarMessage(t('profile.username.success'));
      setSnackbarType('success');
      setSnackbarVisible(true);
      // Logout immediately after username change to avoid token issues
      // The snackbar will still be visible during logout/navigation
      await logout();
    },
    onError: (error: any) => {
      console.error('Username update error - Status:', error?.response?.status);
      console.error('Username update error - Message:', error?.response?.data?.message);

      const status = error?.response?.status;
      const backendMessage = error?.response?.data?.message;
      const errorMessage = error?.message;

      let message = t('common.error');

      // Check for username conflict
      if (status === 409 || (backendMessage && backendMessage.includes('Username'))) {
        console.log('Username conflict detected');
        message = t('profile.username.taken');
      } else if (status === 400 && backendMessage && backendMessage.includes('Username')) {
        message = t('profile.username.taken');
      } else if (backendMessage) {
        message = backendMessage;
      } else if (errorMessage) {
        message = errorMessage;
      }

      console.log('Showing snackbar with message:', message);
      setSnackbarMessage(message);
      setSnackbarType('error');
      setSnackbarVisible(true);
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: profileApi.changePassword,
    onSuccess: () => {
      setPasswordDialogVisible(false);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSnackbarMessage(t('profile.password.success'));
      setSnackbarType('success');
      setSnackbarVisible(true);
    },
    onError: () => {
      setSnackbarMessage(t('profile.password.error'));
      setSnackbarType('error');
      setSnackbarVisible(true);
    },
  });

  const uploadAvatarMutation = useMutation({
    mutationFn: (uri: string) => usersApi.uploadAvatar(user!.id, uri),
    onSuccess: async () => {
      await fetchUser();
      bumpAvatarVersion();
      setAvatarRefreshKey(Date.now());
      setSnackbarMessage(t('profile.avatar.upload.success'));
      setSnackbarType('success');
      setSnackbarVisible(true);
    },
    onError: (error: any) => {
      console.error('Avatar upload error:', error);
      setSnackbarMessage(t('profile.avatar.upload.error'));
      setSnackbarType('error');
      setSnackbarVisible(true);
    },
  });

  const handleUsernameUpdate = () => {
    if (!newUsername || newUsername === user?.username) {
      setUsernameDialogVisible(false);
      return;
    }
    updateUsernameMutation.mutate({ username: newUsername });
  };

  const handleFirstNameUpdate = () => {
    if (!newFirstName || newFirstName === user?.firstName) {
      setFirstNameDialogVisible(false);
      return;
    }
    updateProfileMutation.mutate({ firstName: newFirstName });
  };

  const handlePasswordUpdate = () => {
    if (newPassword !== confirmPassword) {
      setSnackbarMessage(t('profile.password.mismatch'));
      setSnackbarType('error');
      setSnackbarVisible(true);
      return;
    }
    if (newPassword.length < 6) {
      setSnackbarMessage(t('profile.password.min'));
      setSnackbarType('error');
      setSnackbarVisible(true);
      return;
    }
    changePasswordMutation.mutate({
      currentPassword,
      newPassword,
    });
  };

  const handleLogout = () => {
    console.log('[PROFILE] Logout button clicked');
    showConfirm({
      title: t('nav.logout'),
      message: t('profile.logout.confirm'),
      onConfirm: () => {
        console.log('[PROFILE] Logout confirmed by user');
        logout();
      },
      confirmText: t('nav.logout'),
      cancelText: t('button.cancel'),
      destructive: true,
    });
  };

  const handleAvatarEmojiSelect = (avatar: AvatarType) => {
    updateProfileMutation.mutate({
      avatarType: 'ICON',
      avatarIconName: avatar.id,
    });
  };

  const handleAvatarImageSelect = async (imageUri: string) => {
    uploadAvatarMutation.mutate(imageUri);
  };

  const getRoleLabel = (role?: string) => {
    switch (role) {
      case 'ADMIN': return t('role.admin');
      case 'PARENT': return t('role.parent');
      case 'CHILD': return t('role.child');
      default: return role;
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]} edges={['bottom']}>
      <ScrollView>
        {/* Profile Header */}
        <Surface style={[styles.header, { backgroundColor: theme.colors.primary }]} elevation={2}>
          <View style={styles.avatarContainer}>
            <UserAvatar
              avatarType={user?.avatarType}
              avatarIconName={user?.avatarIconName}
              avatarPath={user?.avatarPath}
              firstName={user?.firstName}
              size={80}
              refreshKey={avatarRefreshKey}
            />
            <TouchableOpacity
              style={[styles.avatarEditButton, { backgroundColor: theme.colors.primary }]}
              onPress={() => setAvatarPickerVisible(true)}
            >
              <MaterialCommunityIcons name="pencil" size={16} color="#FFF" />
            </TouchableOpacity>
          </View>
          <Text variant="headlineSmall" style={styles.name}>
            {user?.firstName}
          </Text>
          <Text variant="bodyMedium" style={styles.role}>
            {getRoleLabel(user?.role)}
          </Text>

          {isChild && (
            <Surface style={styles.pointsCard} elevation={1}>
              <Text variant="bodyMedium" style={styles.pointsLabel}>
                {t('child.dashboard.points.badge')}
              </Text>
              <Text variant="displaySmall" style={styles.pointsValue}>
                {user?.totalPoints || 0}
              </Text>
            </Surface>
          )}
        </Surface>

        {/* Settings Menu */}
        <View style={styles.section}>
          <Text variant="titleMedium" style={[styles.sectionTitle, { color: theme.colors.onSurfaceVariant }]}>
            {t('settings.title')}
          </Text>
          <Surface style={styles.menuGroup} elevation={1}>
            {/* Edit Username */}
            <List.Item
              title={t('profile.username')}
              description={user?.username}
              left={props => <List.Icon {...props} icon="account" />}
              right={props => <List.Icon {...props} icon="pencil" />}
              onPress={() => {
                setNewUsername(user?.username || '');
                setUsernameDialogVisible(true);
              }}
            />
            <Divider />

            {/* Edit First Name */}
            <List.Item
              title={t('profile.firstName')}
              description={user?.firstName}
              left={props => <List.Icon {...props} icon="card-account-details" />}
              right={props => <List.Icon {...props} icon="pencil" />}
              onPress={() => {
                setNewFirstName(user?.firstName || '');
                setFirstNameDialogVisible(true);
              }}
            />
            <Divider />

            {/* Change Password */}
            <List.Item
              title={t('profile.edit.password')}
              left={props => <List.Icon {...props} icon="lock" />}
              right={props => <List.Icon {...props} icon="chevron-right" />}
              onPress={() => setPasswordDialogVisible(true)}
            />
            <Divider />

            {/* Language Selection */}
            <List.Item
              title={t('profile.language')}
              description={availableLanguages[language].nativeName}
              left={props => <List.Icon {...props} icon="translate" />}
              right={props => <List.Icon {...props} icon="chevron-right" />}
              onPress={() => setLanguageDialogVisible(true)}
            />
            <Divider />

            {/* Theme Toggle */}
            <List.Item
              title={t('settings.theme.title')}
              description={isDark ? t('settings.theme.dark') : t('settings.theme.light')}
              left={props => <List.Icon {...props} icon={isDark ? 'weather-night' : 'weather-sunny'} />}
              right={props => <List.Icon {...props} icon="chevron-right" />}
              onPress={toggleTheme}
            />
            <Divider />

            {/* Notifications */}
            <List.Item
              title={t('nav.settings')}
              left={props => <List.Icon {...props} icon="cog" />}
              right={props => <List.Icon {...props} icon="chevron-right" />}
              onPress={() => { }}
            />
          </Surface>
        </View>

        {/* Logout Button */}
        <View style={styles.section}>
          <List.Item
            title={t('nav.logout')}
            titleStyle={{ color: theme.colors.error }}
            left={props => <List.Icon {...props} icon="logout" color={theme.colors.error} />}
            onPress={handleLogout}
            style={[styles.logoutButton, { backgroundColor: theme.colors.surface }]}
          />
        </View>

        {/* Footer */}
        <View style={styles.footer}>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {t('app.name')} v1.0.0
          </Text>
        </View>
      </ScrollView>

      {/* Language Selection Dialog */}
      <Portal>
        <Dialog visible={languageDialogVisible} onDismiss={() => setLanguageDialogVisible(false)}>
          <Dialog.Title>{t('settings.language.select')}</Dialog.Title>
          <Dialog.Content>
            <RadioButton.Group
              onValueChange={(value) => {
                setLanguage(value as any);
                updateProfileMutation.mutate({ language: value as any });
                setLanguageDialogVisible(false);
              }}
              value={language}
            >
              {Object.entries(availableLanguages).map(([code, lang]) => (
                <RadioButton.Item
                  key={code}
                  label={lang.nativeName}
                  value={code}
                />
              ))}
            </RadioButton.Group>
          </Dialog.Content>
        </Dialog>

        {/* Edit Username Dialog */}
        <Dialog visible={usernameDialogVisible} onDismiss={() => setUsernameDialogVisible(false)}>
          <Dialog.Title>{t('profile.edit.username')}</Dialog.Title>
          <Dialog.Content>
            <TextInput
              label={t('profile.username')}
              value={newUsername}
              onChangeText={setNewUsername}
              autoCapitalize="none"
              mode="outlined"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setUsernameDialogVisible(false)}>{t('button.cancel')}</Button>
            <Button onPress={handleUsernameUpdate}>
              {t('button.save')}
            </Button>
          </Dialog.Actions>
        </Dialog>

        {/* Edit First Name Dialog */}
        <Dialog visible={firstNameDialogVisible} onDismiss={() => setFirstNameDialogVisible(false)}>
          <Dialog.Title>{t('profile.edit.firstName')}</Dialog.Title>
          <Dialog.Content>
            <TextInput
              label={t('profile.firstName')}
              value={newFirstName}
              onChangeText={setNewFirstName}
              mode="outlined"
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setFirstNameDialogVisible(false)}>{t('button.cancel')}</Button>
            <Button onPress={handleFirstNameUpdate}>{t('button.save')}</Button>
          </Dialog.Actions>
        </Dialog>

        {/* Change Password Dialog */}
        <Dialog visible={passwordDialogVisible} onDismiss={() => setPasswordDialogVisible(false)}>
          <Dialog.Title>{t('profile.edit.password')}</Dialog.Title>
          <Dialog.Content>
            <TextInput
              label={t('profile.password.current')}
              value={currentPassword}
              onChangeText={setCurrentPassword}
              secureTextEntry
              mode="outlined"
              style={styles.dialogInput}
            />
            <TextInput
              label={t('profile.password.new')}
              value={newPassword}
              onChangeText={setNewPassword}
              secureTextEntry
              mode="outlined"
              style={styles.dialogInput}
            />
            <TextInput
              label={t('profile.password.confirm')}
              value={confirmPassword}
              onChangeText={setConfirmPassword}
              secureTextEntry
              mode="outlined"
              style={styles.dialogInput}
            />
            {newPassword && newPassword.length < 6 && (
              <HelperText type="error">{t('profile.password.min')}</HelperText>
            )}
            {confirmPassword && newPassword !== confirmPassword && (
              <HelperText type="error">{t('profile.password.mismatch')}</HelperText>
            )}
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setPasswordDialogVisible(false)}>{t('button.cancel')}</Button>
            <Button
              onPress={handlePasswordUpdate}
              disabled={!currentPassword || !newPassword || !confirmPassword || newPassword !== confirmPassword || newPassword.length < 6}
            >
              {t('button.save')}
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>

      {/* Snackbar for feedback */}
      <Snackbar
        visible={snackbarVisible}
        onDismiss={() => setSnackbarVisible(false)}
        duration={4000}
        style={{
          backgroundColor: snackbarType === 'error' ? theme.colors.error : theme.colors.primary,
        }}
        action={{
          label: t('button.close'),
          onPress: () => setSnackbarVisible(false),
        }}
      >
        {snackbarMessage}
      </Snackbar>

      {/* Dialogs */}
      <Dialogs />

      {/* Avatar Picker */}
      <AvatarPicker
        visible={avatarPickerVisible}
        onClose={() => setAvatarPickerVisible(false)}
        onSelectEmoji={handleAvatarEmojiSelect}
        onSelectImage={handleAvatarImageSelect}
        currentAvatarType={user?.avatarType}
        currentAvatarIconName={user?.avatarIconName}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    padding: spacing.lg,
    alignItems: 'center',
  },
  avatarContainer: {
    position: 'relative',
    marginBottom: spacing.md,
  },
  avatarEditButton: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 32,
    height: 32,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#FFF',
  },
  avatar: {
    marginBottom: spacing.md,
    backgroundColor: 'rgba(255,255,255,0.3)',
  },
  name: {
    color: '#fff',
    fontWeight: '700',
    marginBottom: spacing.xs,
  },
  role: {
    color: 'rgba(255,255,255,0.9)',
  },
  pointsCard: {
    borderRadius: 16,
    padding: spacing.md,
    marginTop: spacing.md,
    alignItems: 'center',
    minWidth: 150,
    backgroundColor: 'rgba(255,255,255,0.2)',
  },
  pointsLabel: {
    color: 'rgba(255,255,255,0.9)',
  },
  pointsValue: {
    color: '#fff',
    fontWeight: '900',
  },
  section: {
    padding: spacing.md,
  },
  sectionTitle: {
    fontWeight: '600',
    textTransform: 'uppercase',
    marginBottom: spacing.sm,
    marginLeft: spacing.xs,
  },
  menuGroup: {
    borderRadius: 12,
    overflow: 'hidden',
  },
  logoutButton: {
    borderRadius: 12,
    overflow: 'hidden',
  },
  dialogInput: {
    marginBottom: spacing.sm,
  },
  footer: {
    padding: spacing.lg,
    alignItems: 'center',
  },
});
