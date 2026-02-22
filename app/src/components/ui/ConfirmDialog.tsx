import React from 'react';
import { Portal, Dialog, Button, Text, useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { View, StyleSheet } from 'react-native';

interface ConfirmDialogProps {
  visible: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  confirmText?: string;
  cancelText?: string;
  destructive?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  visible,
  title,
  message,
  onConfirm,
  onCancel,
  confirmText = 'OK',
  cancelText = 'Abbrechen',
  destructive = false,
}) => {
  const theme = useTheme();

  return (
    <Portal>
      <Dialog visible={visible} onDismiss={onCancel}>
        <Dialog.Content style={styles.content}>
          <View style={styles.iconContainer}>
            <MaterialCommunityIcons
              name={destructive ? 'alert' : 'help-circle'}
              size={64}
              color={destructive ? theme.colors.error : theme.colors.primary}
            />
          </View>
          <Text variant="headlineSmall" style={[styles.title, { color: theme.colors.onSurface }]}>
            {title}
          </Text>
          <Text variant="bodyMedium" style={[styles.message, { color: theme.colors.onSurfaceVariant }]}>
            {message}
          </Text>
        </Dialog.Content>
        <Dialog.Actions>
          <Button onPress={onCancel} mode="outlined">
            {cancelText}
          </Button>
          <Button
            onPress={onConfirm}
            mode="contained"
            buttonColor={destructive ? theme.colors.error : theme.colors.primary}
          >
            {confirmText}
          </Button>
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
};

const styles = StyleSheet.create({
  content: {
    alignItems: 'center',
    paddingTop: 24,
  },
  iconContainer: {
    marginBottom: 16,
  },
  title: {
    fontWeight: '600',
    marginBottom: 8,
    textAlign: 'center',
  },
  message: {
    textAlign: 'center',
    marginBottom: 8,
  },
});
