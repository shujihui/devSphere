# Dev-Sphere Development Framework

## Project Introduction

Dev-Sphere is a microservices development framework based on Spring Cloud Alibaba, featuring comprehensive modules such as permission management, code generation, system utilities, and instant messaging. It is suitable for enterprise-level backend management system development, providing complete permission control, data dictionary, log management, and other functions.

## Technical Architecture

Designed using mainstream microservices architecture:
- Spring Boot 2.7.x
- Spring Cloud Alibaba 2021.0.4
- Nacos for service registration and configuration management
- Sentinel for traffic protection
- Gateway as the API gateway service
- MyBatis Plus for data access layer
- Redis for caching service
- WebSocket for instant messaging

## Functional Modules

### Permission Management (shutu-auth)
- User Management: User creation, permission assignment, password management
- Role Management: Role definition, permission binding
- Menu Management: System menu configuration, permission identifiers
- Department Management: Organization structure maintenance
- Log Management: Operation logs, login logs recording

### System Tools (shutu-commons)
- Dynamic Data Sources: Support for multi-data source switching
- Code Generation: Automatically generate CRUD code based on table structures
- File Export: Support for Excel import and export
- Parameter Management: System parameter configuration
- Message Notification: System message push

### Instant Messaging (zhiLiao)
- Private Chat: Point-to-point communication between users
- Group Chat: Support for multi-user chat rooms
- Friend Management: Friend adding and group management
- Message Push: Real-time message notification based on WebSocket
- Online Status: Real-time display of user online status

## Project Structure

```
├── shutu-admin        # Administration backend module
├── shutu-auth         # Permission management module
├── shutu-commons      # Common components module
├── shutu-gateway      # API gateway module
├── shutu-module       # Business module
└── zhiLiao           # Instant messaging module
```

## Development Standards

1. Coding Style: Follow Alibaba Java Development Guidelines
2. Log Management: Use SLF4J to record system logs
3. Exception Handling: Unified exception handling mechanism
4. Data Access: Use MyBatis Plus for database operations
5. Interface Documentation: Generate API documentation through Swagger

## Deployment Instructions

1. Database: MySQL 5.7+, create the corresponding database and execute initialization scripts
2. Middleware: Install basic components such as Redis and RabbitMQ
3. Configuration Center: Configure module configuration files in Nacos
4. Service Startup: Start the registry, configuration center, basic services, and business services in order

## Version Updates

Check [CHANGELOG.md](CHANGELOG.md) for complete version update records

## Contribution Guide

Code contributions are welcome. Please follow the steps below:
1. Fork the project repository
2. Create a feature branch
3. Commit code changes
4. Create a Pull Request

## License

This project is licensed under the Apache-2.0 License. For details, please refer to the [LICENSE](LICENSE) file