import React, { useState } from 'react';
import { Modal, ScrollView, StyleSheet, TouchableOpacity, View } from 'react-native';
import { Button, Dialog, Portal, Text, useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { PREDEFINED_AVATARS, Avatar } from '../../constants/avatars';
import { spacing } from '../../theme/theme';

interface AvatarPickerProps {
  visible: boolean;
  onClose: () => void;
  onSelectEmoji: (emoji: Avatar) => void;
  onSelectImage: (imageUri: string) => void;
  currentAvatarType?: string;
  currentAvatarIconName?: string;
}

export const AvatarPicker: React.FC<AvatarPickerProps> = ({
  visible,
  onClose,
  onSelectEmoji,
  onSelectImage,
  currentAvatarType,
  currentAvatarIconName,
}) => {
  const theme = useTheme();
  const [selectedTab, setSelectedTab] = useState<'emoji' | 'upload'>('emoji');

  const handleImagePick = async () => {
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (permissionResult.granted === false) {
      alert('Bitte erlaube den Zugriff auf deine Fotos in den Einstellungen.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]) {
      onSelectImage(result.assets[0].uri);
      onClose();
    }
  };

  const handleEmojiSelect = (avatar: Avatar) => {
    onSelectEmoji(avatar);
    onClose();
  };

  return (
    <Portal>
      <Dialog visible={visible} onDismiss={onClose} style={{ maxHeight: '80%' }}>
        <Dialog.Title>Avatar auswählen</Dialog.Title>
        <Dialog.Content>
          {/* Tabs */}
          <View style={styles.tabs}>
            <TouchableOpacity
              style={[
                styles.tab,
                selectedTab === 'emoji' && styles.tabActive,
                { borderColor: theme.colors.primary },
              ]}
              onPress={() => setSelectedTab('emoji')}
            >
              <Text
                style={[
                  styles.tabText,
                  selectedTab === 'emoji' && { color: theme.colors.primary },
                ]}
              >
                🐾 Tiere
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.tab,
                selectedTab === 'upload' && styles.tabActive,
                { borderColor: theme.colors.primary },
              ]}
              onPress={() => setSelectedTab('upload')}
            >
              <Text
                style={[
                  styles.tabText,
                  selectedTab === 'upload' && { color: theme.colors.primary },
                ]}
              >
                📷 Eigenes Bild
              </Text>
            </TouchableOpacity>
          </View>

          {/* Content */}
          {selectedTab === 'emoji' ? (
            <ScrollView style={styles.emojiGrid} showsVerticalScrollIndicator={false}>
              <View style={styles.grid}>
                {PREDEFINED_AVATARS.map((avatar) => (
                  <TouchableOpacity
                    key={avatar.id}
                    style={[
                      styles.emojiItem,
                      currentAvatarType === 'ICON' &&
                        currentAvatarIconName === avatar.id &&
                        styles.emojiItemSelected,
                      { backgroundColor: theme.colors.surfaceVariant },
                    ]}
                    onPress={() => handleEmojiSelect(avatar)}
                  >
                    <Text style={styles.emoji}>{avatar.emoji}</Text>
                    <Text style={styles.emojiName}>{avatar.name}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </ScrollView>
          ) : (
            <View style={styles.uploadContainer}>
              <TouchableOpacity
                style={[styles.uploadButton, { backgroundColor: theme.colors.primaryContainer }]}
                onPress={handleImagePick}
              >
                <MaterialCommunityIcons
                  name="camera-plus"
                  size={48}
                  color={theme.colors.primary}
                />
                <Text style={[styles.uploadText, { color: theme.colors.primary }]}>
                  Bild auswählen
                </Text>
              </TouchableOpacity>
            </View>
          )}
        </Dialog.Content>
        <Dialog.Actions>
          <Button onPress={onClose}>Abbrechen</Button>
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
};

const styles = StyleSheet.create({
  tabs: {
    flexDirection: 'row',
    marginBottom: spacing.md,
    gap: spacing.sm,
  },
  tab: {
    flex: 1,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent',
    alignItems: 'center',
  },
  tabActive: {
    borderWidth: 2,
  },
  tabText: {
    fontSize: 14,
    fontWeight: '600',
  },
  emojiGrid: {
    maxHeight: 400,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    paddingBottom: spacing.md,
  },
  emojiItem: {
    width: '22%',
    aspectRatio: 1,
    borderRadius: 12,
    padding: spacing.xs,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emojiItemSelected: {
    borderWidth: 3,
    borderColor: '#4CAF50',
  },
  emoji: {
    fontSize: 32,
    marginBottom: spacing.xs,
  },
  emojiName: {
    fontSize: 10,
    textAlign: 'center',
  },
  uploadContainer: {
    padding: spacing.lg,
    alignItems: 'center',
  },
  uploadButton: {
    padding: spacing.xl,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    minHeight: 200,
  },
  uploadText: {
    marginTop: spacing.md,
    fontSize: 16,
    fontWeight: '600',
  },
});
