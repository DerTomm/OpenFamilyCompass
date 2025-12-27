# Internationalization (i18n) Documentation

## Overview

OpenFamilyCompass now supports multiple languages! The application is available in **English** (default) and **German**, with the ability to easily add more languages in the future.

## Webapp (Spring Boot)

### Configuration

The webapp uses Spring Boot's standard i18n support with the following configuration:

- **LocaleResolver**: Uses `CookieLocaleResolver` to store the user's language preference in a cookie
- **Default Language**: English (`en`)
- **Message Files Location**: `src/main/resources/`
  - `messages.properties` - English (default)
  - `messages_de.properties` - German

### Changing Language

Users can change the language by:
1. Using the language switcher (🌐 globe icon) in the navigation bar
2. Adding `?lang=de` or `?lang=en` to any URL

The selected language is stored in a cookie and persists across sessions.

### Message Keys

All text in the webapp is now internationalized using message keys. In Thymeleaf templates, use:

```html
<span th:text="#{message.key}">Default Text</span>
```

For messages with parameters:
```html
<span th:text="#{message.key(${variable})}">Default Text</span>
```

### Adding a New Language

To add a new language (e.g., French):

1. Create a new message file: `src/main/resources/messages_fr.properties`
2. Copy the content from `messages.properties`
3. Translate all values to French
4. Add the language option to the language switcher in templates:
   ```html
   <li><a class="dropdown-item" href="?lang=fr">🇫🇷 Français</a></li>
   ```

### Available Message Categories

- **Login**: Login page text
- **Navigation**: Menu items and navigation
- **Dashboard**: Dashboard content
- **Tasks**: Task management
- **Rewards**: Reward system
- **Behavior**: Behavior evaluation
- **Points**: Point system
- **Children**: Child management
- **Profile**: User settings
- **Common**: Buttons, status messages, validation

## Android App

### Configuration

The Android app uses Android's standard resource system for internationalization:

- **Default Language**: English
- **Resource Files**:
  - `app/src/main/res/values/strings.xml` - English (default)
  - `app/src/main/res/values-de/strings.xml` - German

### Language Selection

The Android app automatically uses the device's system language. If the system language is German, the German strings are used; otherwise, English is used as the default.

Users can change the app language by changing their device's system language settings.

### Using String Resources in Code

In Kotlin code:
```kotlin
getString(R.string.message_key)
```

With parameters:
```kotlin
getString(R.string.message_key, parameter1, parameter2)
```

In XML layouts:
```xml
<TextView
    android:text="@string/message_key" />
```

### Adding a New Language

To add a new language (e.g., French):

1. Create a new directory: `app/src/main/res/values-fr/`
2. Copy `strings.xml` from `values/` to `values-fr/`
3. Translate all string values to French
4. The app will automatically use French strings when the device language is set to French

### Available String Categories

- **Menu**: App menu items
- **Settings**: Settings screen
- **Buttons**: Common button labels
- **Dialogs**: Dialog messages
- **Error Messages**: Error text
- **Toast Messages**: Notification messages

## Contributing Translations

We welcome contributions for additional language support! If you'd like to add a new language:

1. Fork the repository
2. Add the new language files (both webapp and Android)
3. Ensure all keys are translated
4. Test the translations
5. Submit a pull request

### Translation Guidelines

- Keep translations natural and contextually appropriate
- Maintain consistent terminology across the application
- Test translations in the actual UI to ensure they fit properly
- Include cultural considerations (date formats, icons, etc.)

## Technical Details

### Webapp Stack
- **Framework**: Spring Boot 3.x
- **Template Engine**: Thymeleaf
- **i18n Support**: Spring MessageSource
- **Locale Storage**: Cookie-based (1 year expiry)

### Android Stack
- **Platform**: Android
- **Language**: Kotlin
- **i18n Support**: Android Resources System
- **Locale Source**: Device system language

## Future Enhancements

Potential improvements for the i18n system:

- Database-driven translations for dynamic content
- User preference for language (independent of system/browser language)
- Translation management interface for administrators
- Automatic translation suggestions using AI
- RTL (Right-to-Left) language support
- Date and number format localization
- Plural forms handling
- Gender-specific translations where applicable

## Supported Languages

| Language | Code | Webapp | Android | Status |
|----------|------|--------|---------|--------|
| English  | en   | ✅     | ✅      | Complete |
| German   | de   | ✅     | ✅      | Complete |
| French   | fr   | ❌     | ❌      | Planned |
| Spanish  | es   | ❌     | ❌      | Planned |

---

**Note**: This implementation follows industry-standard i18n practices and can be easily extended to support additional languages as the community grows.
