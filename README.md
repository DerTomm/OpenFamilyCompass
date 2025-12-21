# Kids Chores & Rewards 🌟

Eine webbasierte Anwendung zur Motivation von Kindern durch Aufgaben und Belohnungen.

## 📋 Übersicht

Diese Anwendung ermöglicht es Eltern, Aufgaben für ihre Kinder zu definieren und sie mit einem Punktesystem zu belohnen. Kinder können ihre Punkte gegen vordefinierte Belohnungen eintauschen.

### Hauptfunktionen

- 👨‍👩‍👧‍👦 **Multi-User System** mit drei Rollen: Admin, Eltern, Kinder
- 📝 **Aufgabenverwaltung** mit einmaligen und wiederkehrenden Aufgaben (täglich, wöchentlich, monatlich)
- ⭐ **Punktesystem** mit flexibler Punktevergabe durch Eltern
- 🛍️ **Belohnungs-Shop** für Kinder zum Einlösen ihrer Punkte
- 💚 **Gewohnheiten-Tracking** für positive Verhaltensweisen
- ⚠️ **Strafpunkte** für negatives Verhalten
- 📊 **Punkte-Historie** zur Nachvollziehbarkeit
- 👤 **Avatar-System** (vordefiniert + Upload-Möglichkeit)

## 🛠️ Technologie-Stack

- **Backend:** Java 21 (LTS), Spring Boot 3.5.9
- **Frontend:** Thymeleaf, Bootstrap 5, Font Awesome
- **Security:** Spring Security mit PIN-basierter Authentifizierung
- **Datenbank:** PostgreSQL 16
- **Build-Tool:** Maven
- **Containerisierung:** Docker (PostgreSQL)

## 📦 Voraussetzungen

- Java 21 oder höher (LTS)
- Maven 3.6+
- Docker & Docker Compose
- Optional: IDE (IntelliJ IDEA, Eclipse, VS Code)

## 🚀 Installation & Start

### 1. Repository klonen (falls vorhanden)

```bash
cd C:\Development\KidsChoresAndRewards
```

### 2. Datenbank starten

```bash
docker-compose up -d
```

Dies startet eine PostgreSQL-Instanz auf Port 5432.

### 3. Anwendung kompilieren

```bash
mvn clean install
```

### 4. Anwendung starten

```bash
mvn spring-boot:run
```

Die Anwendung ist dann verfügbar unter: **http://localhost:8080**

## 🔐 Standard-Login

Nach dem ersten Start wird automatisch ein Admin-Account erstellt:

- **Benutzername:** `admin`
- **PIN:** `0000`

⚠️ **Wichtig:** Bitte ändern Sie das Admin-Passwort nach der ersten Anmeldung!

## 👥 Benutzer-Rollen

### Admin
- Vollständiger Zugriff auf alle Funktionen
- Benutzer- und Kinderverwaltung
- Aufgaben-, Belohnungs- und Gewohnheitsverwaltung
- Zugriff: `/admin/dashboard`

### Eltern
- Aufgaben genehmigen/ablehnen
- Belohnungsanfragen genehmigen
- Positive Gewohnheiten erfassen
- Strafpunkte vergeben
- Zugriff: `/parent/dashboard`

### Kinder
- Aufgaben ansehen und als erledigt markieren
- Punkte-Stand einsehen
- Belohnungen im Shop eintauschen
- Punkte-Historie anzeigen
- Zugriff: `/child/dashboard`

## 📁 Projektstruktur

```
KidsChoresAndRewards/
├── src/
│   ├── main/
│   │   ├── java/com/family/kidschores/
│   │   │   ├── config/              # Konfiguration (Security, Web)
│   │   │   ├── controller/          # Web-Controller
│   │   │   ├── init/                # Daten-Initialisierung
│   │   │   ├── model/               # Domain-Modelle
│   │   │   ├── repository/          # JPA Repositories
│   │   │   ├── service/             # Business-Logik
│   │   │   └── KidsChoresApplication.java
│   │   └── resources/
│   │       ├── static/              # CSS, JS, Bilder
│   │       ├── templates/           # Thymeleaf Templates
│   │       └── application.yml      # Konfiguration
│   └── test/                        # Tests
├── docker-compose.yml               # PostgreSQL Setup
├── pom.xml                          # Maven Dependencies
└── README.md
```

## 💾 Datenbank-Schema

### Hauptentitäten

- **User:** Benutzer-Accounts (Admin, Parent, Child)
- **Child:** Kinder-Profile mit Avatar und Punktestand
- **Task:** Aufgaben (einmalig oder wiederkehrend)
- **Reward:** Belohnungen im Shop
- **RewardRedemption:** Eingelöste Belohnungen
- **Habit:** Positive Gewohnheiten
- **Penalty:** Strafpunkte
- **PointTransaction:** Historie aller Punktebewegungen

## 🎯 Typischer Workflow

### Für Eltern/Admin:

1. Neue Kinder anlegen (Admin)
2. Aufgaben definieren (einmalig oder wiederkehrend)
3. Belohnungen im Shop hinterlegen
4. Positive Gewohnheiten definieren
5. Aufgaben von Kindern genehmigen/ablehnen
6. Belohnungsanfragen bearbeiten
7. Bei Bedarf Strafpunkte vergeben

### Für Kinder:

1. Anmelden mit Vorname und PIN
2. Aufgaben-Übersicht ansehen
3. Aufgaben als erledigt markieren
4. Auf Genehmigung der Eltern warten
5. Punkte im Shop gegen Belohnungen eintauschen
6. Historie der Punktebewegungen einsehen

## 🔄 Wiederkehrende Aufgaben

Das System erstellt automatisch wiederkehrende Aufgaben:

- **Täglich:** Jeden Tag um Mitternacht
- **Wöchentlich:** Wöchentlich am gleichen Wochentag
- **Monatlich:** Monatlich am gleichen Tag

Ein Scheduler-Job läuft täglich um 0:00 Uhr und erstellt die fälligen Aufgaben.

## 🎨 Avatar-System

Kinder können einen Avatar wählen aus:

- **Vordefinierte Avatare:** cat, dog, bear, lion, elephant, giraffe, panda, unicorn
- **Eigene Uploads:** Bilder können hochgeladen werden (max. 5MB)

Avatare werden gespeichert unter: `uploads/avatars/`

## 🔧 Konfiguration

Die wichtigsten Einstellungen in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/kidschores
    username: kidschores_user
    password: kidschores_password

server:
  port: 8080

app:
  admin:
    default-username: admin
    default-pin: "0000"
  avatars:
    upload-dir: uploads/avatars
```

## 🧪 Tests ausführen

```bash
mvn test
```

## 📝 Nächste Schritte / Erweiterungen

Mögliche zukünftige Features:

- [ ] Mobile App (React Native / Flutter)
- [ ] Kalenderansicht für Aufgaben
- [ ] Push-Benachrichtigungen
- [ ] Statistiken und Reports
- [ ] Familien-Rangliste
- [ ] Aufgaben-Templates
- [ ] Export von Punktehistorie (PDF/Excel)
- [ ] Multi-Tenancy (mehrere Familien)
- [ ] Gamification (Badges, Achievements)

## 🐛 Bekannte Einschränkungen

- Bild-Upload für Belohnungen noch nicht implementiert
- Keine E-Mail-Benachrichtigungen
- Keine API-Endpunkte (nur Web-UI)

## 📄 Lizenz

Dieses Projekt ist für den privaten Gebrauch bestimmt.

## 👨‍💻 Entwickler

Erstellt mit GitHub Copilot für eine bessere Familien-Organisation! 🏠

## 🆘 Support

Bei Problemen oder Fragen:

1. Datenbank-Logs prüfen: `docker-compose logs postgres`
2. Anwendungs-Logs prüfen: Console-Output
3. Datenbank zurücksetzen: `docker-compose down -v && docker-compose up -d`

---

**Viel Erfolg bei der Motivation Ihrer Kinder! 🌟**
