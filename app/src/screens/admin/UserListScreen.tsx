import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../../api/services';
import { UserResponse, UserRole } from '../../types/api';

const ROLE_COLORS: Record<UserRole, string> = {
  ADMIN: '#F44336',
  PARENT: '#2196F3',
  CHILD: '#4CAF50',
};

interface UserCardProps {
  user: UserResponse;
  onEdit: () => void;
  onToggleActive: () => void;
}

const UserCard: React.FC<UserCardProps> = ({ user, onEdit, onToggleActive }) => (
  <View style={[styles.card, !user.active && styles.cardInactive]}>
    <View style={styles.cardHeader}>
      <View style={styles.avatar}>
        <Text style={styles.avatarText}>
          {user.firstName.charAt(0).toUpperCase()}
        </Text>
      </View>
      <View style={styles.userInfo}>
        <Text style={styles.userName}>{user.firstName}</Text>
        <Text style={styles.userUsername}>@{user.username}</Text>
      </View>
      <View style={[styles.roleBadge, { backgroundColor: ROLE_COLORS[user.role] }]}>
        <Text style={styles.roleText}>{user.role}</Text>
      </View>
    </View>
    
    {user.role === 'CHILD' && (
      <View style={styles.statsRow}>
        <Text style={styles.statsLabel}>Points:</Text>
        <Text style={styles.statsValue}>{user.totalPoints}</Text>
      </View>
    )}
    
    <View style={styles.cardActions}>
      <TouchableOpacity style={styles.actionButton} onPress={onEdit}>
        <Text style={styles.actionButtonText}>Edit</Text>
      </TouchableOpacity>
      <TouchableOpacity 
        style={[styles.actionButton, user.active ? styles.deactivateButton : styles.activateButton]} 
        onPress={onToggleActive}
      >
        <Text style={[styles.actionButtonText, user.active ? styles.deactivateText : styles.activateText]}>
          {user.active ? 'Deactivate' : 'Activate'}
        </Text>
      </TouchableOpacity>
    </View>
  </View>
);

interface CreateUserModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (data: { username: string; password: string; firstName: string; role: string }) => void;
  isLoading: boolean;
}

const CreateUserModal: React.FC<CreateUserModalProps> = ({ visible, onClose, onSubmit, isLoading }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [role, setRole] = useState<UserRole>('CHILD');

  const handleSubmit = () => {
    if (!username || !password || !firstName) {
      Alert.alert('Error', 'Please fill in all fields');
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
          <Text style={styles.modalTitle}>Create New User</Text>
          
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Username</Text>
            <TextInput
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              placeholder="username"
              autoCapitalize="none"
            />
          </View>
          
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Password</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder="password"
              secureTextEntry
            />
          </View>
          
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>First Name</Text>
            <TextInput
              style={styles.input}
              value={firstName}
              onChangeText={setFirstName}
              placeholder="First Name"
            />
          </View>
          
          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Role</Text>
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
              <Text style={styles.submitButtonText}>Create User</Text>
            )}
          </TouchableOpacity>
          
          <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
            <Text style={styles.cancelButtonText}>Cancel</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

export const UserListScreen: React.FC = () => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [roleFilter, setRoleFilter] = useState<UserRole | 'ALL'>('ALL');
  const queryClient = useQueryClient();

  const { data: users, isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['users', roleFilter],
    queryFn: () => usersApi.list(roleFilter === 'ALL' ? undefined : roleFilter),
  });

  const createUser = useMutation({
    mutationFn: usersApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setShowCreateModal(false);
      Alert.alert('Success', 'User created successfully');
    },
    onError: () => {
      Alert.alert('Error', 'Failed to create user');
    },
  });

  const deactivateUser = useMutation({
    mutationFn: usersApi.deactivate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const handleToggleActive = (user: UserResponse) => {
    Alert.alert(
      user.active ? 'Deactivate User' : 'Activate User',
      `Are you sure you want to ${user.active ? 'deactivate' : 'activate'} ${user.firstName}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: user.active ? 'Deactivate' : 'Activate',
          style: user.active ? 'destructive' : 'default',
          onPress: () => deactivateUser.mutate(user.id),
        },
      ]
    );
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
              {r}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {isLoading ? (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#2196F3" />
        </View>
      ) : (
        <FlatList
          data={users}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <UserCard
              user={item}
              onEdit={() => {}}
              onToggleActive={() => handleToggleActive(item)}
            />
          )}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={isRefetching} onRefresh={refetch} />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>No users found</Text>
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

      <CreateUserModal
        visible={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={(data) => createUser.mutate(data)}
        isLoading={createUser.isPending}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  filterContainer: {
    flexDirection: 'row',
    padding: 12,
    gap: 8,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
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
    color: '#666',
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
  listContent: {
    padding: 16,
    paddingBottom: 80,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  },
  cardInactive: {
    opacity: 0.6,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#e0e0e0',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  avatarText: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#666',
  },
  userInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
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
  statsRow: {
    flexDirection: 'row',
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
  },
  statsLabel: {
    color: '#666',
    marginRight: 4,
  },
  statsValue: {
    fontWeight: '600',
    color: '#4CAF50',
  },
  cardActions: {
    flexDirection: 'row',
    marginTop: 12,
    gap: 8,
  },
  actionButton: {
    flex: 1,
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
  },
  actionButtonText: {
    fontWeight: '500',
    color: '#333',
  },
  deactivateButton: {
    backgroundColor: '#ffebee',
  },
  deactivateText: {
    color: '#F44336',
  },
  activateButton: {
    backgroundColor: '#e8f5e9',
  },
  activateText: {
    color: '#4CAF50',
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
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 20,
  },
  inputGroup: {
    marginBottom: 16,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
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
    color: '#666',
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
    color: '#666',
    fontSize: 16,
  },
});
