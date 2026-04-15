# 📘 Formini Platform

> A modern e-learning platform built with a **Microservices Architecture**  
> combining **scalability, security, and AI-driven intelligence**.

---

## 🎯 Project Description

**Smart Learning Platform** is an e-learning application designed to manage:

- Online training programs  
- Learners and trainers  
- Course scheduling and certifications  

The project adopts a **microservices-based architecture** to ensure:
- Scalability
- Modularity
- High maintainability
- Secure access control

---

## 👥 Actors

| Actor | Description |
|-----|------------|
| **Administrator** | Manages users, courses, and system configuration |
| **Learner** | Enrolls in courses, attends sessions, takes exams |
| **Partner** | Proposes trainers and manages trainer requests |

---

## 🧩 Microservices

Each microservice is **independent** and owns its **own database**.

- 🔐 **Identity & Access Service** (Keycloak)
- 👤 **User Profile Service**
- 🤝 **Trainer Hiring Service**
- 📚 **Course Management Service**
- 💳 **Subscription & Billing Service**
- 🗓️ **Learning Schedule Service**
- 📝 **Evaluation & Certification Service**
- 💬 **Forum Service**

---

## 🛠️ Technologies

### 🔹 Backend
- Spring Boot  
- Spring Cloud  

### 🔹 Frontend
- Angular 

### 🔹 Databases
- MySQL  
- H2  
- MongoDB  

### 🔹 Security
- Keycloak  
- OAuth2 / JWT  

---

## 🤖 AI Models Used

| AI Type | Purpose |
|-------|--------|
| **Supervised Learning** | Predict learner success |
| **Unsupervised Learning** | Group learners by behavior |
| **Reinforcement Learning** | Recommend personalized learning paths |

---

## 🏗️ Architecture

The application follows a **Microservices Architecture** based on:

- API Gateway  
- Service Discovery (Eureka)  
- Centralized Configuration  
- Database per microservice  

---

## 📌 Project Goal

This project is developed for **educational purposes**  
to demonstrate **modern microservices design** and **AI integration** in real-world systems.
