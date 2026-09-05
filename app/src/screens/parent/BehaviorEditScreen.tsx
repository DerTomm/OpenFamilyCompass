import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View
} from 'react-native';
import { useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { behaviorsApi, usersApi } from '../../api/services';
import { useDialogs } from '../../hooks/useDialogs';
import { useI18n } from '../../i18n/I18nContext';
import { ManageStackParamList } from '../../navigation/types';

type BehaviorEditRouteProp = RouteProp<ManageStackParamList, 'BehaviorEdit'>;

export const BehaviorEditScreen: React.FC = () => {
    const navigation = useNavigation();
    const route = useRoute<BehaviorEditRouteProp>();
    const { t } = useI18n();
    const { showError, showConfirm, Dialogs } = useDialogs();
    const theme = useTheme();
    const styles = createStyles(theme);
    const queryClient = useQueryClient();

    const behaviorId = route.params?.behaviorId;
    const isEditing = !!behaviorId;

    const { data: behavior, isLoading: isLoadingBehavior } = useQuery({
        queryKey: ['behavior', behaviorId],
        queryFn: () => behaviorsApi.getById(behaviorId!),
        enabled: !!behaviorId,
    });

    const { data: children } = useQuery({
        queryKey: ['children'],
        queryFn: () => usersApi.listChildren(),
    });

    const [title, setTitle] = useState('');
    const [guideline, setGuideline] = useState('');
    const [plusPoints, setPlusPoints] = useState('');
    const [minusPoints, setMinusPoints] = useState('');
    const [userId, setUserId] = useState<number | undefined>(undefined);

    useEffect(() => {
        if (behavior) {
            setTitle(behavior.title);
            setGuideline(behavior.guideline);
            setPlusPoints(behavior.plusPoints.toString());
            setMinusPoints(behavior.minusPoints.toString());
            setUserId(behavior.user?.id);
        }
    }, [behavior]);

    const createMutation = useMutation({
        mutationFn: behaviorsApi.create,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['behaviors'] });
            navigation.goBack();
        },
        onError: () => {
            showError(t('behavior.create.error'), t('common.error'));
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, data }: { id: number; data: Parameters<typeof behaviorsApi.update>[1] }) =>
            behaviorsApi.update(id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['behaviors'] });
            queryClient.invalidateQueries({ queryKey: ['behavior', behaviorId] });
            navigation.goBack();
        },
        onError: () => {
            showError(t('behavior.edit.error'), t('common.error'));
        },
    });

    const deactivateMutation = useMutation({
        mutationFn: behaviorsApi.deactivate,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['behaviors'] });
            navigation.goBack();
        },
    });

    const handleSave = () => {
        if (!title.trim() || !guideline.trim()) {
            showError(t('behavior.error.fields'), t('common.error'));
            return;
        }

        const parsedPlus = plusPoints.trim().length ? parseInt(plusPoints, 10) : 0;
        const parsedMinus = minusPoints.trim().length ? parseInt(minusPoints, 10) : 0;

        if (Number.isNaN(parsedPlus) || Number.isNaN(parsedMinus) || parsedPlus < 0 || parsedMinus < 0) {
            showError(t('behavior.error.fields'), t('common.error'));
            return;
        }

        if (parsedPlus === 0 && parsedMinus === 0) {
            showError(t('behavior.error.fields'), t('common.error'));
            return;
        }

        if (isEditing) {
            updateMutation.mutate({
                id: behaviorId!,
                data: {
                    title,
                    guideline,
                    plusPoints: parsedPlus,
                    minusPoints: parsedMinus,
                    // The update API uses 0 to explicitly remove a child restriction.
                    userId: userId ?? 0,
                },
            });
        } else {
            createMutation.mutate({
                title,
                guideline,
                plusPoints: parsedPlus,
                minusPoints: parsedMinus,
                userId,
            });
        }
    };

    const handleDelete = () => {
        showConfirm({
            title: t('behavior.delete.title'),
            message: t('behavior.delete.confirm'),
            onConfirm: () => deactivateMutation.mutate(behaviorId!),
            confirmText: t('behavior.delete'),
            cancelText: t('button.cancel'),
            destructive: true,
        });
    };

    const isLoading =
        isLoadingBehavior ||
        createMutation.isPending ||
        updateMutation.isPending ||
        deactivateMutation.isPending;

    if (isEditing && isLoadingBehavior) {
        return (
            <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color="#ffc107" />
            </View>
        );
    }

    return (
        <SafeAreaView style={styles.container} edges={[]}>
            <ScrollView contentContainerStyle={styles.content}>
                <View style={styles.section}>
                    <Text style={styles.label}>{t('behavior.title.label')} *</Text>
                    <TextInput
                        style={styles.input}
                        value={title}
                        onChangeText={setTitle}
                        placeholder={t('behavior.title.hint')}
                        placeholderTextColor={theme.colors.onSurfaceVariant}
                    />
                </View>

                <View style={styles.section}>
                    <Text style={styles.label}>{t('behavior.guidelines.label')} *</Text>
                    <TextInput
                        style={[styles.input, styles.textArea]}
                        value={guideline}
                        onChangeText={setGuideline}
                        placeholder={t('behavior.guidelines.hint')}
                        placeholderTextColor={theme.colors.onSurfaceVariant}
                        multiline
                        numberOfLines={4}
                        textAlignVertical="top"
                    />
                </View>

                <View style={styles.row}>
                    <View style={[styles.section, styles.halfSection]}>
                        <Text style={styles.label}>Max. Minuspunkte *</Text>
                        <TextInput
                            style={styles.input}
                            value={minusPoints}
                            onChangeText={setMinusPoints}
                            placeholder="0"
                            placeholderTextColor={theme.colors.onSurfaceVariant}
                            keyboardType="number-pad"
                        />
                    </View>
                    <View style={[styles.section, styles.halfSection]}>
                        <Text style={styles.label}>Max. Pluspunkte *</Text>
                        <TextInput
                            style={styles.input}
                            value={plusPoints}
                            onChangeText={setPlusPoints}
                            placeholder="0"
                            placeholderTextColor={theme.colors.onSurfaceVariant}
                            keyboardType="number-pad"
                        />
                    </View>
                </View>

                <View style={styles.section}>
                    <Text style={styles.label}>{t('behavior.child.label')}</Text>
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

                <View style={styles.buttonContainer}>
                    <TouchableOpacity style={styles.saveButton} onPress={handleSave} disabled={isLoading}>
                        {createMutation.isPending || updateMutation.isPending ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <Text style={styles.saveButtonText}>{t('button.save')}</Text>
                        )}
                    </TouchableOpacity>

                    {isEditing && (
                        <TouchableOpacity style={styles.deleteButton} onPress={handleDelete} disabled={isLoading}>
                            {deactivateMutation.isPending ? (
                                <ActivityIndicator color="#fff" />
                            ) : (
                                <Text style={styles.deleteButtonText}>{t('behavior.delete')}</Text>
                            )}
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
    row: {
        flexDirection: 'row',
        gap: 12,
    },
    halfSection: {
        flex: 1,
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
        color: theme.colors.onSurface,
        backgroundColor: theme.colors.surface,
    },
    textArea: {
        minHeight: 100,
        textAlignVertical: 'top',
    },
    childSelector: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: 8,
    },
    childOption: {
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderRadius: 20,
        backgroundColor: theme.colors.surfaceVariant,
    },
    childOptionActive: {
        backgroundColor: '#2196F3',
    },
    childOptionText: {
        fontSize: 14,
        fontWeight: '500',
        color: theme.colors.onSurfaceVariant,
    },
    childOptionTextActive: {
        color: '#fff',
    },
    buttonContainer: {
        gap: 12,
        marginTop: 8,
    },
    saveButton: {
        backgroundColor: '#2196F3',
        padding: 14,
        borderRadius: 8,
        alignItems: 'center',
    },
    saveButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
    deleteButton: {
        backgroundColor: '#F44336',
        padding: 14,
        borderRadius: 8,
        alignItems: 'center',
    },
    deleteButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
});
