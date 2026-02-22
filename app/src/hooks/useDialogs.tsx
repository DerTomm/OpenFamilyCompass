import React, { useState } from 'react';
import { SuccessDialog, ErrorDialog, ConfirmDialog } from '../components/ui';

interface DialogConfig {
  title?: string;
  message: string;
}

interface ConfirmConfig extends DialogConfig {
  onConfirm: () => void;
  confirmText?: string;
  cancelText?: string;
  destructive?: boolean;
}

export const useDialogs = () => {
  const [successConfig, setSuccessConfig] = useState<DialogConfig | null>(null);
  const [errorConfig, setErrorConfig] = useState<DialogConfig | null>(null);
  const [confirmConfig, setConfirmConfig] = useState<ConfirmConfig | null>(null);

  const showSuccess = (message: string, title?: string) => {
    setSuccessConfig({ message, title });
  };

  const showError = (message: string, title?: string) => {
    setErrorConfig({ message, title });
  };

  const showConfirm = (config: ConfirmConfig) => {
    setConfirmConfig(config);
  };

  const hideSuccess = () => setSuccessConfig(null);
  const hideError = () => setErrorConfig(null);
  const hideConfirm = () => setConfirmConfig(null);

  const handleConfirm = () => {
    if (confirmConfig) {
      confirmConfig.onConfirm();
      hideConfirm();
    }
  };

  const Dialogs = () => (
    <>
      {successConfig && (
        <SuccessDialog
          visible={true}
          title={successConfig.title}
          message={successConfig.message}
          onDismiss={hideSuccess}
        />
      )}
      {errorConfig && (
        <ErrorDialog
          visible={true}
          title={errorConfig.title}
          message={errorConfig.message}
          onDismiss={hideError}
        />
      )}
      {confirmConfig && (
        <ConfirmDialog
          visible={true}
          title={confirmConfig.title || ''}
          message={confirmConfig.message}
          onConfirm={handleConfirm}
          onCancel={hideConfirm}
          confirmText={confirmConfig.confirmText}
          cancelText={confirmConfig.cancelText}
          destructive={confirmConfig.destructive}
        />
      )}
    </>
  );

  return {
    showSuccess,
    showError,
    showConfirm,
    Dialogs,
  };
};
