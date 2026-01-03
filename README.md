# OpenFamilyCompass 🧭

**Guiding children's behavior and family routines — together.**

An open-source web application for organizing family tasks, behavior rules, and rewards.

## 📋 Overview

OpenFamilyCompass is an open-source solution designed for parents to manage kids chores and promote good behavior and habbits. It combines task management, behavior points, and a plus/minus system in a child-friendly, intuitive interface. The development is transparent and community-driven.

This project is in a very early stage. User experiences, feature and process ideas and collaboration is highly appreciated.

### Key Features

- 👨‍👩‍👧‍👦 **Multi-User System** with three roles: Admin, Parents, Children
- 📝 **Task Management** with one-time and recurring tasks (daily, weekly, monthly)
- ⭐ **Points System** with flexible point allocation by parents
- 🛍️ **Reward Shop** for children to redeem their points
- 💚 **Habit Tracking** for positive behaviors
- ⚠️ **Penalty Points** for negative behavior
- 📊 **Points History** for accountability
- 👤 **Avatar System** (predefined + upload option)

## 🛠️ Technology Stack

- **Backend:** Java + Spring
- **Frontend:** Thymeleaf, Bootstrap
- **Security:** Spring Security with password-based authentication
- **Database:** PostgreSQL 16
- **Build Tool:** Maven
- **Containerization:** Docker (PostgreSQL)

## 📦 Prerequisites

- Java 21 or higher (LTS)
- Maven 3.6+
- Docker & Docker Compose
- Optional: IDE (IntelliJ IDEA, Eclipse, VS Code)

## 🚀 Installation & Startup

### 1. Clone Repository (if available)

```bash
cd C:\Development\OpenFamilyCompass
```

### 2. Start Database

```bash
docker-compose up -d
```

This starts a PostgreSQL instance on port 5432.

### 3. Compile Application

```bash
mvn clean install
```

### 4. Start Application

```bash
mvn spring-boot:run
```

The application will be available at: **http://localhost:8080**

## 🔐 Default Login

After the first startup, an admin account is automatically created:

- **Username:** `admin`
- **Password:** `admin`

⚠️ **Important:** Please change the admin password after the first login!

## 👥 User Roles

### Admin
- User management

### Parents
- Task, reward, and habit management
- Approve/reject tasks
- Approve reward requests
- Record positive habits
- Assign bonus and penalty points

### Children
- View tasks and mark as completed
- View points balance
- Redeem rewards in shop
- View points history

## 📁 Project Structure

```
OpenFamilyCompass/
├── src/
│   ├── main/
│   │   ├── java/com/family/kidschores/
│   │   │   ├── config/              # Configuration (Security, Web)
│   │   │   ├── controller/          # Web Controllers
│   │   │   ├── init/                # Data Initialization
│   │   │   ├── model/               # Domain Models
│   │   │   ├── repository/          # JPA Repositories
│   │   │   ├── service/             # Business Logic
│   │   │   └── KidsChoresApplication.java
│   │   └── resources/
│   │       ├── static/              # CSS, JS, Images
│   │       ├── templates/           # Thymeleaf Templates
│   │       └── application.yml      # Configuration
│   └── test/                        # Tests
├── docker-compose.yml               # PostgreSQL Setup
├── pom.xml                          # Maven Dependencies
└── README.md
```

## 💾 Database Schema

### Main Entities

- **User:** User accounts (Admin, Parent, Child)
- **Child:** Child profiles with avatar and points balance
- **Task:** Tasks (one-time or recurring)
- **Reward:** Rewards in shop
- **RewardRedemption:** Redeemed rewards
- **Habit:** Positive habits
- **Penalty:** Penalty points
- **PointTransaction:** History of all point movements

## 🎯 Typical Workflow

### For Parents/Admin:

1. Create new children (Admin)
2. Define tasks (one-time or recurring)
3. Set up rewards in shop
4. Define positive habits
5. Approve/reject tasks from children
6. Process reward requests
7. Assign penalty points when needed

### For Children:

1. Log in with first name and password
2. View task overview
3. Mark tasks as completed
4. Wait for parental approval
5. Redeem points for rewards in shop
6. View points history

## 🔄 Recurring Tasks

The system automatically creates recurring tasks:

- **Daily:** Every day at midnight
- **Weekly:** Weekly on the same day of the week
- **Monthly:** Monthly on the same day

A scheduler job runs daily at 0:00 and creates due tasks.

## 🎨 Avatar System

Children can choose an avatar from:

- **Predefined Avatars:** cat, dog, bear, lion, elephant, giraffe, panda, unicorn
- **Custom Uploads:** Images can be uploaded (max. 5MB)

Avatars are stored under: `uploads/avatars/`

## 🔧 Configuration

The most important settings in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openfamilycompass_db
    username: openfamilycompass_user
    password: openfamilycompass_password

server:
  port: 8080

app:
  admin:
    default-username: admin
    default-password: "admin"
  avatars:
    upload-dir: uploads/avatars
```

## 🧪 Running Tests

```bash
mvn test
```

## 📝 Next Steps / Extensions

Possible future features:

- [ ] Mobile App (React Native / Flutter)
- [ ] Calendar view for tasks
- [ ] Push notifications
- [ ] Statistics and reports
- [ ] Family leaderboard
- [ ] Task templates
- [ ] Export points history (PDF/Excel)
- [ ] Multi-tenancy (multiple families)
- [ ] Gamification (Badges, Achievements)

## 🐛 Known Limitations

- Image upload for rewards not yet implemented
- No email notifications

## � Security

For security considerations, deployment guidelines, and best practices, please refer to [SECURITY.md](SECURITY.md).

**Important:** Before deploying to production, ensure you have read and implemented all security recommendations.

## 📄 License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**. See [LICENSE](LICENSE) for details.

### What does AGPL mean?

The AGPL is a **strong copyleft license** that ensures this software remains free and open source:

- ✅ **Free to use** - for personal, educational, and non-profit use
- ✅ **Modification allowed** - you can adapt it to your needs
- ✅ **Distribution allowed** - share it with others
- ⚠️ **Network use = distribution** - if you run a modified version on a server, you must share the source code
- ⚠️ **Share-alike** - derivatives must use the same license
- ⚠️ **No proprietary forks** - you cannot make closed-source versions

### Why AGPL?

We chose AGPL to ensure that:
1. **Open Source stays open** - improvements benefit everyone
2. **No proprietary SaaS** - companies can't create closed commercial products without sharing back
3. **Community collaboration** - all users can see and improve the code
4. **Fair use** - if you use it, you contribute back

### Commercial Use

If you want to use OpenFamilyCompass in a commercial product **without** releasing your source code, please contact us about a commercial license.

## 👥 Contributing

We welcome contributions! Please see [AUTHORS](AUTHORS) for guidelines on how to contribute to this project.

### Development

This project follows these principles:
- **Transparent development** - all changes are tracked in git history
- **Security first** - especially for data handling
- **Community-driven** - your feedback and ideas are welcome

---

**Good luck motivating your children! 🌟**
