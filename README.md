# AutoLoc API

API de gestion d'une application de location de véhicules, développée avec **Java**, **Spring Boot**, **Spring Data JPA** et **MySQL** dans le cadre des ateliers d'Architecture des Systèmes d'Information (ASI).

## Présentation

Le projet AutoLoc fournit le socle d'une application de location de véhicules. Il modélise les principales entités métier et leurs relations, puis organise l'accès aux données et la logique métier selon une architecture multicouche.

Les ateliers couverts par le projet portent sur :

- **Atelier 1 — Spring Boot et entités JPA :** création du projet Maven, configuration de MySQL et définition des entités.
- **Atelier 2 — Associations JPA :** relations entre entités, `mappedBy`, stratégies de chargement (`LAZY` / `EAGER`), cascade et `orphanRemoval`.
- **Atelier 3 — Repositories :** accès aux données à travers les interfaces Spring Data JPA héritant de `JpaRepository`.
- **Atelier 4 — IoC, injection de dépendances et Services :** centralisation de la logique métier dans la couche Service et injection des repositories par constructeur.

> Les contrôleurs REST et la validation des endpoints peuvent être ajoutés dans les ateliers suivants selon l'état actuel du dépôt.

## Technologies utilisées

- Java 17 ou version supérieure
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA / Hibernate
- MySQL
- Lombok
- Jakarta Validation
- IntelliJ IDEA
- Postman (pour tester l'API lorsqu'elle est exposée)
- SonarQube for IDE / SonarLint (analyse de la qualité du code)

## Entités métier

Le modèle AutoLoc comprend les neuf entités suivantes :

- `Agence`
- `Vehicule`
- `Employe`
- `Equipement`
- `Client`
- `Reservation`
- `Contrat`
- `Paiement`
- `Maintenance`

Le modèle inclut notamment une relation entre `Contrat` et `Paiement`. Un contrat peut posséder plusieurs paiements ; cette association utilise `cascade = CascadeType.ALL`, `orphanRemoval = true` et un chargement `LAZY`, car un paiement dépend de son contrat.

## Architecture du projet

Les packages principaux sont organisés sous `tn.esprit.autoloc` :

```text
src/
└── main/
    ├── java/tn/esprit/autoloc/
    │   ├── domain/          # Entités JPA et énumérations
    │   ├── repository/      # Interfaces Spring Data JPA
    │   ├── service/         # Interfaces et implémentations de la logique métier
    │   ├── exception/       # Exceptions métier
    │   └── web/
    │       ├── controller/  # Contrôleurs REST
    │       └── dto/         # Objets de transfert de données
    └── resources/
        └── application.properties
```

### Rôle des couches

- **Domain :** représente les entités persistées en base de données.
- **Repository :** fournit les opérations d'accès aux données grâce à Spring Data JPA.
- **Service :** applique les règles métier et utilise les repositories. Les services ne dépendent pas directement de la couche web.
- **Exception :** contient les exceptions métier, par exemple `ResourceNotFoundException`.
- **Controller :** couche d'entrée HTTP destinée à exposer les opérations sous forme d'API REST.

Les services utilisent de préférence l'injection par constructeur, avec des dépendances déclarées `final` et, lorsque cela convient, Lombok `@RequiredArgsConstructor`.

## Prérequis

Avant de lancer le projet, installer ou configurer :

1. JDK 17 ou supérieur.
2. Maven (ou le wrapper Maven fourni par le projet, s'il existe).
3. MySQL Server.
4. IntelliJ IDEA ou un autre IDE compatible Java.
5. Git.

## Installation et lancement

### 1. Cloner le dépôt

```bash
git clone https://github.com/esraaa-b/SI.git
cd SI
```

Ouvrez ensuite le dossier contenant `pom.xml` dans IntelliJ IDEA. Si le projet Spring Boot se trouve dans un sous-dossier, placez-vous dans ce dossier avant d'exécuter les commandes Maven.

### 2. Configurer la base de données

Le projet est prévu pour utiliser une base MySQL nommée `autoloc_db`. Dans `src/main/resources/application.properties`, configurez les paramètres adaptés à votre environnement :

```properties
spring.application.name=autoloc-api

spring.datasource.url=jdbc:mysql://localhost:3306/autoloc_db?createDatabaseIfNotExist=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

logging.level.org.hibernate.SQL=DEBUG
logging.level.tn.esprit.autoloc=INFO
```

Définissez `DB_USERNAME` et `DB_PASSWORD` dans votre environnement si nécessaire. Adaptez ces valeurs à votre installation MySQL. **Ne publiez jamais de vrai mot de passe dans le dépôt Git.**

`spring.jpa.hibernate.ddl-auto=update` est utilisé pour le développement et permet à Hibernate de mettre à jour le schéma à partir des entités. Pour une application en production, privilégiez une stratégie de migration contrôlée.

### 3. Compiler et démarrer

À la racine du module qui contient `pom.xml`, exécutez :

```bash
mvn clean install
mvn spring-boot:run
```

Vous pouvez aussi lancer la classe principale `AutolocApiApplication` depuis votre IDE.

Au démarrage, vérifiez les logs Spring Boot et Hibernate pour confirmer que l'application démarre correctement et que la connexion MySQL fonctionne.

## Bonnes pratiques appliquées

- Déclarer les entités avec `@Entity` et des identifiants JPA générés.
- Préférer les annotations Lombok ciblées (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`) plutôt que `@Data` sur les entités avec associations.
- Utiliser `FetchType.LAZY` pour éviter le chargement inutile des associations, sauf besoin fonctionnel justifié.
- Configurer cascade et `orphanRemoval` selon le cycle de vie réel des objets liés.
- Faire dépendre les services des interfaces Repository, et non de leurs implémentations concrètes.
- Utiliser l'injection par constructeur plutôt que l'injection directe dans les attributs.
- Garder les services sans état mutable partagé.
- Vérifier les erreurs de démarrage en examinant notamment la cause la plus profonde (`Caused by`).
- Utiliser SonarQube for IDE pour repérer et corriger les problèmes de qualité du code.

## Tests et vérifications

- Vérifier que le projet compile : `mvn clean install`.
- Vérifier que l'application démarre sans erreur.
- Vérifier les tables et les clés étrangères générées dans MySQL.
- Lorsque les contrôleurs REST sont disponibles, tester les endpoints avec Postman.

## Dépôt et branche de travail

La branche de travail mentionnée pour les modifications de l'atelier 4 est `atelier-4`.

Pour envoyer de nouveaux commits sur cette branche :

```bash
git add .
git commit -m "Atelier 4 : couche Service, IoC et injection de dépendances"
git push
```

## État du projet

Ce README décrit le périmètre pédagogique AutoLoc et l'architecture prévue par les supports des ateliers 1 à 4. Les fonctionnalités effectivement présentes doivent être confirmées à partir du code du dépôt. Mettez cette section à jour à chaque nouvel atelier, notamment après l'ajout et la validation des contrôleurs REST.

## Auteur et contexte

Projet académique — Architecture des Systèmes d'Information (ASI), année 2026–2027.
