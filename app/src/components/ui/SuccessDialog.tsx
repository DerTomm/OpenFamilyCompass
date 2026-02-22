import React from 'react';
import { Portal, Dialog, Button, Text, useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { View, StyleSheet } from 'react-native';

interface SuccessDialogProps {
  visible: boolean;
  title?: string;
  message: string;
  onDismiss: () => void;
  buttonText?: string;
}

export const SuccessDialog: React.FC<SuccessDialogProps> = ({
  visible,
  title = 'Erfolg',
  message,
  onDismiss,
  buttonText = 'OK',
}) => {
  const theme = useTheme();

  return (
    <Portal>
      <Dialog visible={visible} onDismiss={onDismiss}>
        <Dialog.Content style={styles.content}>
          <View style={styles.iconContainer}>
            <MaterialCommunityIcons
              name="check-circle"
              size={64}
              color={theme.colors.primary}
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
          <Button onPress={onDismiss} mode="contained">
            {buttonText}
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
