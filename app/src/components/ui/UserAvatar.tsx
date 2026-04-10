import React, { useEffect, useState } from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { Avatar as PaperAvatar } from 'react-native-paper';
import { api } from '../../api/client';
import { PREDEFINED_AVATARS } from '../../constants/avatars';
import { useAuthStore } from '../../store/authStore';

interface UserAvatarProps {
  avatarType?: string;
  avatarIconName?: string;
  avatarPath?: string;
  firstName?: string;
  size?: number;
  refreshKey?: number;
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  avatarType,
  avatarIconName,
  avatarPath,
  firstName,
  size = 80,
  refreshKey,
}) => {
  const [imageDataUri, setImageDataUri] = useState<string | null>(null);
  const avatarVersion = useAuthStore((state) => state.avatarVersion);

  useEffect(() => {
    if (avatarType === 'PHOTO' && avatarPath) {
      setImageDataUri(null);
      api.fetchImageAsBase64(avatarPath).then(setImageDataUri);
    } else {
      setImageDataUri(null);
    }
  }, [avatarType, avatarPath, refreshKey, avatarVersion]);

  // Photo avatar
  if (avatarType === 'PHOTO') {
    if (!imageDataUri) {
      // Still loading or failed - show initials as fallback
      const initials = firstName?.charAt(0).toUpperCase() || '?';
      return <PaperAvatar.Text size={size} label={initials} />;
    }
    return (
      <Image
        source={{ uri: imageDataUri }}
        style={[styles.image, { width: size, height: size, borderRadius: size / 2 }]}
      />
    );
  }

  // Icon/Emoji avatar
  if (avatarType === 'ICON' && avatarIconName) {
    const avatar = PREDEFINED_AVATARS.find((a) => a.id === avatarIconName);
    if (avatar) {
      return (
        <View
          style={[
            styles.emojiContainer,
            { width: size, height: size, borderRadius: size / 2 },
          ]}
        >
          <PaperAvatar.Text
            size={size}
            label={avatar.emoji}
            style={styles.emoji}
            labelStyle={{ fontSize: size * 0.5 }}
          />
        </View>
      );
    }
  }

  // Default avatar (initials)
  const initials = firstName?.charAt(0).toUpperCase() || '?';
  return <PaperAvatar.Text size={size} label={initials} />;
};

const styles = StyleSheet.create({
  image: {
    resizeMode: 'cover',
  },
  emojiContainer: {
    overflow: 'hidden',
  },
  emoji: {
    backgroundColor: 'transparent',
  },
});
