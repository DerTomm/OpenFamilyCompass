import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
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
import { usersApi } from '../../api/services';
import { useI18n } from '../../i18n/I18nContext';
import { UserResponse, UserRole } from '../../types/api';
import { useDialogs } from '../../hooks/useDialogs';
import { UserAvatar } from '../../components/ui';

const ROLE_COLORS: Record<UserRole, string> = {
  ADMIN: '#F44336',
  PARENT: '#2196F3',
  CHILD: '#4CAF50',
};

interface UserRowProps {
  user: UserResponse;
  onEdit: () => void;
  onToggleActive: () => void;
  onDelete: () => void;
  isDesktop: boolean;
  isLastActiveAdmin: boolean;
  t: (key: string) => string;
  styles: any;
}

const UserRow: React.FC<UserRowProps> = ({ user, onEdit, onToggleActive, onDelete, isDesktop, isLastActiveAdmin, t, styles }) => {
  if (isDesktop) {
    // Desktop table row
    return (
      <View style={[styles.tableRow, !user.active && styles.tableRowInactive]}>
        <View style={[styles.tableCell, { width: 200 }]}>
          <Text style={styles.tableCellText}>{user.username}</Text>
        </View>
        <View style={[styles.tableCell, { width: 200 }]}>
          <Text style={styles.tableCellText}>{user.firstName}</Text>
        </View>
        <View style={[styles.tableCell, { width: 120 }]}>
          <View style={[styles.roleBadge, { backgroundColor: ROLE_COLORS[user.role] }]}>
            <Text style={styles.roleText}>{t(`role.${user.role.toLowerCase()}`)}</Text>
          </View>
        </View>
        <View style={styles.tableCellActions}>
          <TouchableOpacity style={styles.iconButton} onPress={onEdit}>
            <MaterialIcons name="edit" size={20} color="#2196F3" />
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.iconButton, isLastActiveAdmin && styles.iconButtonDisabled]} 
            onPress={onToggleActive}
            disabled={isLastActiveAdmin}
          >
            <MaterialIcons 
              name={user.active ? "toggle-on" : "toggle-off"} 
              size={24} 
              color={isLastActiveAdmin ? "#ccc" : (user.active ? "#4CAF50" : "#999")} 
            />
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.iconButton, isLastActiveAdmin && styles.iconButtonDisabled]} 
            onPress={onDelete}
            disabled={isLastActiveAdmin}
          >
            <MaterialIcons name="delete" size={20} color={isLastActiveAdmin ? "#ccc" : "#F44336"} />
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // Mobile card layout
  return (
    <View style={[styles.card, !user.active && styles.cardInactive]}>
      <View style={styles.cardHeader}>
        <UserAvatar
          avatarType={user.avatarType}
          avatarIconName={user.avatarIconName}
          avatarPath={user.avatarPath}
          firstName={user.firstName}
          size={48}
        />
        <View style={styles.userInfo}>
          <Text style={styles.userName}>{user.firstName}</Text>
          <Text style={styles.userUsername}>@{user.username}</Text>
        </View>
        <View style={[styles.roleBadge, { backgroundColor: ROLE_COLORS[user.role] }]}>
          <Text style={styles.roleText}>{t(`role.${user.role.toLowerCase()}`)}</Text>
        </View>
      </View>

      <View style={styles.cardActions}>
        <TouchableOpacity style={styles.mobileActionButton} onPress={onEdit}>
          <MaterialIcons name="edit" size={20} color="#2196F3" />
          <Text style={[styles.actionButtonText, { color: '#2196F3' }]}>
            {t('button.edit')}
          </Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.mobileActionButton, isLastActiveAdmin && styles.mobileActionButtonDisabled]} 
          onPress={onToggleActive}
          disabled={isLastActiveAdmin}
        >
          <MaterialIcons 
            name={user.active ? "toggle-on" : "toggle-off"} 
            size={20} 
            color={isLastActiveAdmin ? "#ccc" : (user.active ? "#4CAF50" : "#999")} 
          />
          <Text style={[styles.actionButtonText, { color: isLastActiveAdmin ? '#ccc' : (user.active ? '#4CAF50' : '#999') }]}>
            {user.active ? t('admin.users.deactivate') : t('admin.users.activate')}
          </Text>
        </TouchableOpacity>
        <TouchableOpacity 
          style={[styles.mobileActionButton, isLastActiveAdmin && styles.mobileActionButtonDisabled]} 
          onPress={onDelete}
          disabled={isLastActiveAdmin}
        >
          <MaterialIcons name="delete" size={20} color={isLastActiveAdmin ? "#ccc" : "#F44336"} />
          <Text style={[styles.actionButtonText, { color: isLastActiveAdmin ? '#ccc' : '#F44336' }]}>
            {t('admin.users.delete')}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

interface EditUserModalProps {
  visible: boolean;
  user: UserResponse | null;
  onClose: () => void;
  onSubmit: (data: { firstName: string; role: string; password?: string }) => void;
  isLoading: boolean;
  t: (key: string) => string;
  showError: (message: string, title?: string) => void;
  styles: any;
}

const EditUserModal: React.FC<EditUserModalProps> = ({ visible, user, onClose, onSubmit, isLoading, t, showError, styles }) => {
  const [firstName, setFirstName] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('CHILD');

  React.useEffect(() => {
    if (user) {
      setFirstName(user.firstName);
      setRole(user.role);
      setPassword('');
    }
  }, [user]);

  const handleSubmit = () => {
    if (!firstName) {
      showError(t('admin.users.create.error.fields'), t('common.error'));
      return;
    }
    
    const data: { firstName: string; role: string; password?: string } = {
      firstName,
      role,
    };
    
    // Only include password if it was changed
    if (password) {
      data.password = password;
    }
    
    onSubmit(data);
  };

  return (
    <Modal visible={visible} transparent animationType="slide">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>{t('admin.users.edit.title')}</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.username')}</Text>
            <TextInput
              style={[styles.input, styles.inputDisabled]}
              value={user?.username || ''}
              editable={false}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.first_name')}</Text>
            <TextInput
              style={styles.input}
              value={firstName}
              onChangeText={setFirstName}
              placeholder={t('admin.users.first_name')}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.password')} ({t('admin.users.edit.password_optional')})</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder={t('admin.users.edit.password_placeholder')}
              secureTextEntry
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.role')}</Text>
            <View style={styles.roleSelector}>
              {(['CHILD', 'PARENT', 'ADMIN'] as UserRole[]).map((r) => (
                <TouchableOpacity
                  key={r}
                  style={[styles.roleOption, role === r && { backgroundColor: ROLE_COLORS[r] }]}
                  onPress={() => setRole(r)}
                >
                  <Text style={[styles.roleOptionText, role === r && styles.roleOptionTextActive]}>
                    {t(`role.${r.toLowerCase()}`)}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          <TouchableOpacity
            style={styles.submitButton}
            onPress={handleSubmit}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.submitButtonText}>{t('admin.users.edit.button')}</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
            <Text style={styles.cancelButtonText}>{t('button.cancel')}</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

interface CreateUserModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (data: { username: string; password: string; firstName: string; role: string }) => void;
  isLoading: boolean;
  t: (key: string) => string;
  showError: (message: string, title?: string) => void;
  styles: any;
}

const CreateUserModal: React.FC<CreateUserModalProps> = ({ visible, onClose, onSubmit, isLoading, t, showError, styles }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [role, setRole] = useState<UserRole>('CHILD');

  const handleSubmit = () => {
    if (!username || !password || !firstName) {
      showError(t('admin.users.create.error.fields'), t('common.error'));
      return;
    }
    onSubmit({ username, password, firstName, role });
  };

  const resetForm = () => {
    setUsername('');
    setPassword('');
    setFirstName('');
    setRole('CHILD');
  };

  React.useEffect(() => {
    if (!visible) resetForm();
  }, [visible]);

  return (
    <Modal visible={visible} transparent animationType="slide">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>{t('admin.users.create.title')}</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.username')}</Text>
            <TextInput
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              placeholder={t('admin.users.username')}
              autoCapitalize="none"
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.password')}</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder={t('admin.users.password')}
              secureTextEntry
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.first_name')}</Text>
            <TextInput
              style={styles.input}
              value={firstName}
              onChangeText={setFirstName}
              placeholder={t('admin.users.first_name')}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>{t('admin.users.role')}</Text>
            <View style={styles.roleSelector}>
              {(['CHILD', 'PARENT', 'ADMIN'] as UserRole[]).map((r) => (
                <TouchableOpacity
                  key={r}
                  style={[styles.roleOption, role === r && { backgroundColor: ROLE_COLORS[r] }]}
                  onPress={() => setRole(r)}
                >
                  <Text style={[styles.roleOptionText, role === r && styles.roleOptionTextActive]}>
                    {r}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          <TouchableOpacity
            style={styles.submitButton}
            onPress={handleSubmit}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.submitButtonText}>{t('admin.users.create.button')}</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
            <Text style={styles.cancelButtonText}>{t('button.cancel')}</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

export const UserListScreen: React.FC = () => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);
  const [roleFilter, setRoleFilter] = useState<UserRole | 'ALL'>('ALL');
  const queryClient = useQueryClient();
  const { t } = useI18n();
  const { width } = useWindowDimensions();
  const isDesktop = width >= 768;
  const { showSuccess, showError, showConfirm, Dialogs } = useDialogs();
  const theme = useTheme();
  const styles = createStyles(theme);

  // Check if user is the last active admin
  const isLastActiveAdmin = (user: UserResponse, allUsers: UserResponse[] | undefined): boolean => {
    if (user.role !== 'ADMIN') return false;
    if (!allUsers) return false;
    
    const activeAdmins = allUsers.filter(u => u.role === 'ADMIN' && u.active);
    return activeAdmins.length === 1 && activeAdmins[0].id === user.id;
  };

  const { data: users, isLoading, refetch, isRefetching, error } = useQuery({
    queryKey: ['users', roleFilter],
    queryFn: async () => {
      console.log('Fetching users with role filter:', roleFilter);
      const result = await usersApi.list(roleFilter === 'ALL' ? undefined : roleFilter);
      console.log('Users fetched:', result);
      return result;
    },
  });

  const createUser = useMutation({
    mutationFn: usersApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setShowCreateModal(false);
      showSuccess(t('admin.users.create.success'), t('common.success'));
    },
    onError: () => {
      showError(t('admin.users.create.error'), t('common.error'));
    },
  });

  const updateUser = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<{ firstName: string; password: string; role: string }> }) =>
      usersApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setShowEditModal(false);
      setEditingUser(null);
      showSuccess(t('admin.users.edit.success'), t('common.success'));
    },
    onError: () => {
      showError(t('admin.users.edit.error'), t('common.error'));
    },
  });

  const deactivateUser = useMutation({
    mutationFn: usersApi.deactivate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const deleteUserPermanent = useMutation({
    mutationFn: usersApi.deletePermanent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const handleEdit = (user: UserResponse) => {
    setEditingUser(user);
    setShowEditModal(true);
  };

  const handleToggleActive = (user: UserResponse) => {
    // Prevent deactivating the last active admin
    if (user.active && isLastActiveAdmin(user, users)) {
      showError(t('admin.users.error.last_admin'), t('common.error'));
      return;
    }

    const message = t('admin.users.toggle.message', { 0: user.active ? t('admin.users.deactivate').toLowerCase() : t('admin.users.activate').toLowerCase(), 1: user.firstName });

    showConfirm({
      title: user.active ? t('admin.users.deactivate.title') : t('admin.users.activate.title'),
      message,
      onConfirm: () => deactivateUser.mutate(user.id),
      confirmText: user.active ? t('admin.users.deactivate') : t('admin.users.activate'),
      cancelText: t('button.cancel'),
      destructive: user.active,
    });
  };

  const handleDelete = (user: UserResponse) => {
    // Prevent deleting the last active admin
    if (isLastActiveAdmin(user, users)) {
      showError(t('admin.users.error.last_admin_delete'), t('common.error'));
      return;
    }

    const message = t('admin.users.delete.message', { 0: user.firstName });

    showConfirm({
      title: t('admin.users.delete.title'),
      message,
      onConfirm: () => deleteUserPermanent.mutate(user.id),
      confirmText: t('admin.users.delete'),
      cancelText: t('button.cancel'),
      destructive: true,
    });
  };

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      <View style={styles.filterContainer}>
        {(['ALL', 'CHILD', 'PARENT', 'ADMIN'] as const).map((r) => (
          <TouchableOpacity
            key={r}
            style={[styles.filterButton, roleFilter === r && styles.filterButtonActive]}
            onPress={() => setRoleFilter(r)}
          >
            <Text style={[styles.filterText, roleFilter === r && styles.filterTextActive]}>
              {t(`role.${r.toLowerCase()}`)}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {error && (
        <View style={styles.errorContainer}>
          <Text style={styles.errorText}>
            Fehler beim Laden: {(error as Error).message}
          </Text>
        </View>
      )}

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : isDesktop ? (
        // Desktop table view
        <ScrollView
          style={styles.tableContainer}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
        >
          <View style={styles.table}>
            {/* Table Header */}
            <View style={styles.tableHeader}>
              <View style={[styles.tableCell, { width: 200 }]}>
                <Text style={styles.tableHeaderText}>{t('admin.users.username')}</Text>
              </View>
              <View style={[styles.tableCell, { width: 200 }]}>
                <Text style={styles.tableHeaderText}>{t('admin.users.first_name')}</Text>
              </View>
              <View style={[styles.tableCell, { width: 120 }]}>
                <Text style={styles.tableHeaderText}>{t('admin.users.role')}</Text>
              </View>
              <View style={styles.tableCellActions}>
                <Text style={styles.tableHeaderText}>{t('common.actions')}</Text>
              </View>
            </View>

            {/* Table Body */}
            {users && users.length > 0 ? (
              users.map((user) => (
                <UserRow
                  key={user.id}
                  user={user}
                  onEdit={() => handleEdit(user)}
                  onToggleActive={() => handleToggleActive(user)}
                  onDelete={() => handleDelete(user)}
                  isDesktop={isDesktop}
                  isLastActiveAdmin={isLastActiveAdmin(user, users)}
                  t={t}
                  styles={styles}
                />
              ))
            ) : (
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyText}>{t('admin.users.no_users')}</Text>
              </View>
            )}
          </View>
        </ScrollView>
      ) : (
        // Mobile list view
        <FlatList
          data={users}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <UserRow
              user={item}
              onEdit={() => handleEdit(item)}
              onToggleActive={() => handleToggleActive(item)}
              onDelete={() => handleDelete(item)}
              isDesktop={isDesktop}
              isLastActiveAdmin={isLastActiveAdmin(item, users)}
              t={t}
              styles={styles}
            />
          )}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>{t('admin.users.no_users')}</Text>
            </View>
          }
        />
      )}

      <TouchableOpacity
        style={styles.fab}
        onPress={() => setShowCreateModal(true)}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>

      <EditUserModal
        visible={showEditModal}
        user={editingUser}
        onClose={() => {
          setShowEditModal(false);
          setEditingUser(null);
        }}
        onSubmit={(data) => editingUser && updateUser.mutate({ id: editingUser.id, data })}
        isLoading={updateUser.isPending}
        t={t}
        showError={showError}
        styles={styles}
      />

      <CreateUserModal
        visible={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={(data) => createUser.mutate(data)}
        isLoading={createUser.isPending}
        t={t}
        showError={showError}
        styles={styles}
      />

      <Dialogs />
    </SafeAreaView>
  );
};

const createStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
  },
  filterContainer: {
    flexDirection: 'row',
    padding: 12,
    gap: 8,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    flexWrap: 'wrap',
  },
  filterButton: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 16,
    backgroundColor: '#f0f0f0',
  },
  filterButtonActive: {
    backgroundColor: '#2196F3',
  },
  filterText: {
    color: theme.colors.onSurfaceVariant,
    fontSize: 13,
    fontWeight: '500',
  },
  filterTextActive: {
    color: '#fff',
  },
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorContainer: {
    padding: 16,
    backgroundColor: '#ffebee',
    margin: 16,
    borderRadius: 8,
  },
  errorText: {
    color: '#c62828',
    fontSize: 14,
  },
  // Desktop table styles
  tableContainer: {
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
    borderBottomWidth: 2,
    borderBottomColor: '#e0e0e0',
    paddingVertical: 16,
    paddingHorizontal: 16,
  },
  tableHeaderText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#424242',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  tableRow: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
    paddingVertical: 16,
    paddingHorizontal: 16,
    alignItems: 'center',
    ...Platform.select({
      web: {
        transition: 'background-color 0.2s',
      },
    }),
  },
  tableRowInactive: {
    opacity: 0.5,
    backgroundColor: '#fafafa',
  },
  tableCell: {
    justifyContent: 'center',
    paddingRight: 16,
  },
  tableCellText: {
    fontSize: 15,
    color: theme.colors.onSurface,
  },
  tableCellActions: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'flex-start',
    alignItems: 'center',
    gap: 8,
    paddingLeft: 32,
    paddingRight: 0,
  },
  iconButton: {
    padding: 8,
    borderRadius: 6,
    backgroundColor: '#f5f5f5',
    ...Platform.select({
      web: {
        cursor: 'pointer',
        transition: 'background-color 0.2s',
      },
    }),
  },
  iconButtonDisabled: {
    opacity: 0.4,
    ...Platform.select({
      web: {
      },
    }),
  },
  // Mobile list styles
  listContent: {
    padding: 16,
    paddingBottom: 80,
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
  cardInactive: {
    opacity: 0.6,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
    gap: 12,
  },
  userInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 18,
    fontWeight: '600',
    color: theme.colors.onSurface,
  },
  userUsername: {
    fontSize: 14,
    color: '#999',
  },
  roleBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  roleText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  cardActions: {
    flexDirection: 'row',
    gap: 8,
  },
  mobileActionButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    padding: 10,
    borderRadius: 8,
    backgroundColor: '#f5f5f5',
  },
  mobileActionButtonDisabled: {
    opacity: 0.4,
  },
  actionButtonText: {
    fontWeight: '500',
    fontSize: 13,
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
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
  },
  fabText: {
    fontSize: 28,
    color: '#fff',
    fontWeight: '300',
  },
  emptyContainer: {
    padding: 32,
    alignItems: 'center',
  },
  emptyText: {
    color: '#999',
    fontSize: 16,
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
    padding: 24,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: theme.colors.onSurface,
    marginBottom: 20,
  },
  inputGroup: {
    marginBottom: 16,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: theme.colors.onSurface,
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: theme.colors.outline,
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
  inputDisabled: {
    backgroundColor: '#f5f5f5',
    color: '#999',
  },
  roleSelector: {
    flexDirection: 'row',
    gap: 8,
  },
  roleOption: {
    flex: 1,
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
  },
  roleOptionText: {
    fontWeight: '500',
    color: theme.colors.onSurfaceVariant,
  },
  roleOptionTextActive: {
    color: '#fff',
  },
  submitButton: {
    backgroundColor: '#2196F3',
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 8,
  },
  submitButtonText: {
    color: '#fff',
    fontWeight: '600',
    fontSize: 16,
  },
  cancelButton: {
    padding: 12,
    alignItems: 'center',
    marginTop: 8,
  },
  cancelButtonText: {
    color: theme.colors.onSurfaceVariant,
    fontSize: 16,
  },
});
