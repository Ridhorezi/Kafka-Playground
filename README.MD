# 🧠 KAFKA PLAYGROUND

This project is a comprehensive demonstration of event-driven microservices architecture using Spring Boot and Apache Kafka. It implements a complete Producer-Consumer pattern for user management with real-time audit trail tracking.

# Project Overview

The system consists of two independent microservices:

- **User Producer Service (user-producer) - REST API for user management**
- **Audit Consumer Service (user-consumer) - Kafka consumer for audit trail logging** 

## ✅ Covered Topics

- **Event-Driven Architecture with Apache Kafka**
- **Spring Boot 3.5.9 & 4.0.1 with Java 21**
- **H2 In-Memory Database for data persistence**
- **RESTful API Design with proper HTTP status codes**
- **Comprehensive Error Handling with global exception handler**
- **Data Validation using Jakarta Bean Validation**
- **Pagination Support for list endpoints**
- **DTO Pattern for request/response mapping**
- **Manual Kafka Acknowledgment for message reliability**
- **Dead Letter Topic (DLT) implementation for error handling**
- **Kafka Topic Configuration with multiple partitions**
- **Audit Trail Logging for all user operations**

## 🚀 How to Run

### Requirements
- Java 21 or above
- Maven or any Java-compatible IDE (e.g. IntelliJ, Eclipse)
- Apache Kafka 3.4+ (or Docker for Kafka)

### Run Tests
```bash
mvn test
```

## 📘 License

This project is licensed under the MIT License - see the LICENSE file for details.

## ✍️ Author

**Ridho Suhaebi Arrowi**  
Feel free to contribute or fork!
