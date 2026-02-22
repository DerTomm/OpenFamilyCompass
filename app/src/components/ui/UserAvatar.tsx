import React from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { Avatar as PaperAvatar } from 'react-native-paper';
import { PREDEFINED_AVATARS } from '../../constants/avatars';

interface UserAvatarProps {
  avatarType?: string;
  avatarIconName?: string;
  avatarPath?: string;
  firstName?: string;
  size?: number;
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  avatarType,
  avatarIconName,
  avatarPath,
  firstName,
  size = 80,
}) => {
  // Photo avatar
  if (avatarType === 'PHOTO' && avatarPath) {
    return (
      <Image
        source={{ uri: avatarPath }}
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
