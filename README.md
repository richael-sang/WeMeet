# WeMeet - Enterprise Meeting Room Booking System

A comprehensive web-based meeting room booking and management system built with Spring Boot, featuring dual portals for end-users and administrators. This system provides a complete solution for managing meeting room reservations in enterprise environments.

## Project Overview

WeMeet is an enterprise-grade meeting room booking system that streamlines the process of reserving and managing meeting spaces. The system handles the complete lifecycle from requirements analysis to deployment, demonstrating modern software engineering practices and full-stack development capabilities.

## Key Features

### User Portal
- **Room Browsing**: Advanced filtering by capacity, equipment, and availability
- **Real-time Booking**: Instant reservation with conflict detection
- **Personal Dashboard**: Manage all your bookings in one place
- **Profile Management**: Customize your account settings
- **Notification System**: Email and in-app alerts for booking updates
- **Help Center**: Comprehensive user guides and FAQs

### Administrator Portal
- **User Management**: Account creation, role assignment, and access control
- **Room Inventory**: CRUD operations with image uploads and equipment tracking
- **Booking Oversight**: Approve/reject reservations, manage conflicts
- **Audit Logging**: Complete activity tracking for compliance
- **Statistical Dashboard**: Booking trends, room utilization, user activity
- **Export Functions**: Generate reports in various formats

## Technology Stack

### Backend
- **Spring Boot 3.4.4**: Modern Java framework
- **Spring Data JPA**: Database ORM and repository pattern
- **Spring Security**: Authentication and authorization
- **MySQL**: Relational database with JPA
- **Redis**: Caching and session management
- **Maven**: Dependency management and build tool

### Frontend
- **Thymeleaf**: Server-side rendering
- **HTML5/CSS3**: Modern web standards
- **JavaScript (ES6+)**: Client-side interactivity
- **Responsive Design**: Mobile-friendly interface
- **Theme Support**: Light/dark mode

### DevOps & Deployment
- **Docker**: Containerization for consistent deployment
- **Alibaba Cloud**: Production environment hosting
- **Git**: Version control
- **Maven**: Build automation

## Project Structure

```
WeMeet/
├── src/
│   ├── main/
│   │   ├── java/com/onlinemeetingbookingsystem/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── MailConfig.java
│   │   │   │   └── RedisConfig.java
│   │   │   ├── controller/          # REST and MVC controllers
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── UserPortalController.java
│   │   │   │   └── ...
│   │   │   ├── entity/              # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── MeetingRoom.java
│   │   │   │   └── Booking.java
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── service/             # Business logic
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── security/            # Security components
│   │   │   └── util/                # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── database-initialization.sql
│   │       ├── static/              # CSS, JavaScript, images
│   │       └── templates/           # Thymeleaf templates
│   └── test/                        # Unit and integration tests
├── pom.xml
└── README.md
```

## Core Functionality

### Authentication & Authorization
- User registration with email verification
- Secure login with password hashing
- JWT token-based session management
- Role-based access control (USER/ADMIN)
- Password recovery system

### Booking Workflow
1. **Search**: Filter rooms by criteria
2. **Reserve**: Select time slot and submit request
3. **Review**: Admin approves/rejects
4. **Confirm**: User receives notification
5. **Manage**: Cancel or modify booking

### Data Management
- **User Profiles**: Avatar upload, personal information
- **Meeting Rooms**: Images, capacity, equipment list
- **Bookings**: Start time, end time, purpose, status
- **Notifications**: Real-time alerts for all stakeholders
- **Audit Logs**: Comprehensive activity tracking

## Database Schema

Key entities and relationships:
- **Users** (1) ← (N) **Bookings** (N) → (1) **MeetingRooms**
- **Users** (1) ← (N) **Notifications**
- **Users** (1) ← (N) **AuditLogs**
- **Bookings** (1) ← (N) **BookingRejectLogs**

## API Endpoints

### User APIs
- `POST /api/auth/login` - User authentication
- `POST /api/auth/register` - Account creation
- `GET /api/rooms` - List available rooms
- `POST /api/bookings` - Create reservation
- `GET /api/bookings/my` - User's bookings

### Admin APIs
- `GET /api/admin/users` - User management
- `POST /api/admin/rooms` - Room CRUD operations
- `PUT /api/admin/bookings/{id}/approve` - Approve booking
- `GET /api/admin/statistics` - System analytics

## Security Features

- **Password Security**: BCrypt hashing with salt
- **Session Management**: Redis-backed tokens
- **CSRF Protection**: Built-in Spring Security
- **Input Validation**: Server-side and client-side
- **SQL Injection Prevention**: JPA parameterized queries
- **XSS Protection**: Template escaping

## Performance Optimization

- **Caching**: Redis for frequently accessed data
- **Connection Pooling**: Efficient database connections
- **Lazy Loading**: JPA fetch strategies
- **Pagination**: Large dataset handling
- **Indexing**: Optimized database queries

## How to Run

### Prerequisites
```bash
# Java 17 or higher
java -version

# MySQL 8.0 or higher
mysql --version

# Maven 3.6+
mvn --version
```

### Database Setup
```sql
CREATE DATABASE wemeet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Run database-initialization.sql
```

### Configuration
Update `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/wemeet
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Build & Run
```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Or run JAR
java -jar target/onlinemeetingbookingsystem-0.0.1-SNAPSHOT.jar
```

Access the application:
- User Portal: `http://localhost:8080`
- Admin Portal: `http://localhost:8080/admin`

### Default Credentials
```
Admin:
Username: admin@example.com
Password: admin123

User:
Username: user@example.com
Password: user123
```

## Docker Deployment

```bash
# Build image
docker build -t wemeet:latest .

# Run container
docker run -p 8080:8080 wemeet:latest
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Design Patterns

- **MVC**: Clear separation of concerns
- **Repository Pattern**: Data access abstraction
- **Service Layer**: Business logic encapsulation
- **DTO Pattern**: Data transfer optimization
- **Factory Pattern**: Object creation
- **Singleton**: Configuration management

## Team Members

- **Rui Sang** - Full-stack development
- **Yuxin He** - Backend development
- **Ziyang Liu** - Frontend development
- **Jiacheng Ni** - Database design
- **Jiaxin Zhang** - Testing & QA

## Course Information

- **Course**: CPT202 - Software Engineering
- **Institution**: Xi'an Jiaotong-Liverpool University
- **Year**: 2025

## Future Enhancements

- [ ] Mobile application (iOS/Android)
- [ ] Calendar integration (Google Calendar, Outlook)
- [ ] Video conferencing integration
- [ ] Room IoT integration (sensors, smart locks)
- [ ] Machine learning for booking predictions
- [ ] Multi-language support
- [ ] Advanced analytics dashboard

## License

This project is developed for educational purposes as part of coursework.

## Acknowledgments

- Spring Boot community for excellent documentation
- Bootstrap team for responsive design framework
- All team members for their contributions

