# Internationalization (i18n) Documentation

## Overview

OpenFamilyCompass is available in **English** (default) and **German**. The Expo mobile app owns all user-facing text; the Spring Boot backend keeps a small message bundle for API-level responses (e.g. validation errors). Additional languages can be added with minimal effort.

## Backend (API messages)

### Configuration

The backend uses Spring's standard `MessageSource` for any server-produced strings:

- **LocaleResolver**: `UserPreferenceLocaleResolver` (see `backend/src/main/java/org/openfamilycompass/config/UserPreferenceLocaleResolver.java`) — reads the caller's preferred locale from the `Accept-Language` header and the authenticated user's profile.
- **Default language**: English (`en`)
- **Message files**: `backend/src/main/resources/`
  - `messages.properties` — English (default)
  - `messages_de.properties` — German

These bundles drive validation messages, error responses and any other backend-generated text returned through the REST API.

### Adding a new language (backend)

1. Create a new file `backend/src/main/resources/messages_fr.properties`.
2. Copy the content from `messages.properties` and translate all values.
3. Restart the backend — Spring picks up the new bundle automatically; clients that send `Accept-Language: fr` will now receive French responses.

## Mobile app (Expo / React Native)

### Configuration

The mobile app uses **i18next** + **react-i18next** together with **expo-localization**:

- **Default language**: detected from the device locale; falls back to English (`en`).
- **Configuration**: `app/src/i18n/config.ts`
- **Translations**: `app/src/i18n/locales/`
  - `en.ts` — English (default)
  - `de.ts` — German
- **Context provider**: `app/src/i18n/I18nContext.tsx` exposes a `useI18n()` hook that wraps the i18next `t` function and the current language.

Initialization happens once in `App.tsx` via `import './src/i18n/config'`.

### Language selection

On first launch, the app picks the device language via `expo-localization`. Users can override this in the profile screen; the choice is persisted (and a reload re-initializes i18next with the stored value).

### Using translations in components

```tsx
import { useI18n } from '../i18n/I18nContext';

export const MyScreen = () => {
  const { t } = useI18n();
  return <Text>{t('nav.overview')}</Text>;
};
```

With parameters:

```tsx
t('points.balance', { count: 42 })
```

### Adding a new language (mobile app)

1. Create a new file `app/src/i18n/locales/fr.ts` and translate every key from `en.ts`.
2. Register it in `app/src/i18n/config.ts`:

   ```ts
   import { fr } from './locales/fr';

   export const SUPPORTED_LANGUAGES = {
     en: { name: 'English', nativeName: 'English' },
     de: { name: 'German',  nativeName: 'Deutsch' },
     fr: { name: 'French',  nativeName: 'Français' },
   } as const;

   i18n.init({
     resources: {
       en: { translation: en },
       de: { translation: de },
       fr: { translation: fr },
     },
     // …
   });
   ```

3. The language picker in the profile screen picks up new entries automatically.

## Contributing translations

Contributions for additional languages are very welcome:

1. Fork the repository.
2. Add the new language files both in the backend (`messages_xx.properties`) and in the mobile app (`app/src/i18n/locales/xx.ts`).
3. Translate all keys — keep the wording natural and contextually appropriate.
4. Test the translations in the mobile app (UI layouts, truncation, plural forms) and by calling the API with `Accept-Language: xx`.
5. Submit a pull request.

### Translation guidelines

- Keep translations natural and culturally appropriate.
- Use consistent terminology across backend and mobile app.
- Consider date / number formats and RTL rendering if applicable.
- Prefer short phrasing where UI space is tight (tab labels, buttons).

## Technical details

### Backend

- **Framework**: Spring Boot 3.5 (REST API)
- **i18n**: Spring `MessageSource` (`messages*.properties`) — used for API-side messages only
- **Locale resolution**: `Accept-Language` header + user profile preference via `UserPreferenceLocaleResolver`

### Mobile app

- **Platform**: Expo SDK 55 / React Native 0.83
- **i18n**: `i18next` + `react-i18next`
- **Locale source**: `expo-localization` (device language) + user preference via `expo-secure-store`

## Future enhancements

Possible improvements:

- Database-driven translations for dynamic content (task titles, reward names)
- Explicit user preference independent of device language
- Admin UI for translation management
- RTL language support
- Pluralization + gender-specific translations

## Supported languages

| Language | Code | Backend | Mobile App | Status   |
| -------- | ---- | ------- | ---------- | -------- |
| English  | en   | ✔      | ✔         | Complete |
| German   | de   | ✔      | ✔         | Complete |
| French   | fr   | —       | —          | Planned  |
| Spanish  | es   | —       | —          | Planned  |

---

**Note**: This implementation follows standard i18n practices and is easily extended as the community grows.
