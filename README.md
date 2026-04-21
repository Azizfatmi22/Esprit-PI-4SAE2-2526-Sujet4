# Reclamation Service

Microservice de gestion des réclamations pour la plateforme Formini.

## Description

Ce microservice permet de gérer les réclamations des apprenants concernant :
- Problèmes techniques
- Problèmes de paiement
- Problèmes de contenu
- Problèmes d'accès
- Problèmes de certificat
- Autres problèmes

## Technologies

- Java 17
- Spring Boot 4.0.2
- Spring Data JPA
- MySQL
- Lombok

## Configuration

- **Port**: 8090
- **Base de données**: `formini_reclamation_db`
- **URL de base**: `http://localhost:8090/msreclamation`

## Endpoints API

### Réclamations

- `POST /msreclamation/reclamations` - Créer une réclamation
- `GET /msreclamation/reclamations` - Récupérer toutes les réclamations
- `GET /msreclamation/reclamations/{id}` - Récupérer une réclamation par ID
- `GET /msreclamation/reclamations/learner/{learnerId}` - Récupérer les réclamations d'un apprenant
- `GET /msreclamation/reclamations/status/{status}` - Récupérer les réclamations par statut
- `GET /msreclamation/reclamations/type/{type}` - Récupérer les réclamations par type
- `GET /msreclamation/reclamations/unresolved` - Récupérer les réclamations non résolues
- `PUT /msreclamation/reclamations/{id}` - Mettre à jour une réclamation
- `PUT /msreclamation/reclamations/{id}/status` - Mettre à jour le statut d'une réclamation
- `POST /msreclamation/reclamations/{id}/response` - Ajouter une réponse admin
- `DELETE /msreclamation/reclamations/{id}` - Supprimer une réclamation
- `GET /msreclamation/reclamations/stats` - Obtenir les statistiques

## Statuts des réclamations

- `PENDING` - En attente
- `IN_PROGRESS` - En cours de traitement
- `RESOLVED` - Résolue
- `CLOSED` - Fermée
- `REJECTED` - Rejetée

## Types de réclamations

- `TECHNICAL` - Problème technique
- `PAYMENT` - Problème de paiement
- `CONTENT` - Problème de contenu
- `ACCESS` - Problème d'accès
- `CERTIFICATE` - Problème de certificat
- `OTHER` - Autre

## Lancement

```bash
mvn spring-boot:run
```

Le service sera accessible sur `http://localhost:8090`
