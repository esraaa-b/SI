# Note technique — Atelier 4 : IoC, injection de dépendances et couche Service

Packages ajoutés sous `tn.esprit.autoloc` : `repository` (les neuf `I…Repository`, prérequis de l'Atelier 3),
`service` (neuf interfaces `I…Service` et neuf `…ServiceImpl`) et `exception` (`ResourceNotFoundException`).

## A. Modes d'injection comparés

| Critère | Constructeur (`@RequiredArgsConstructor` + champ `final`) | Attribut (`@Autowired` sur le champ) |
|---|---|---|
| Champ `final` possible ? | Oui | Non : le champ est affecté après la construction |
| Dépendance visible dans la signature de la classe ? | Oui : le constructeur la déclare | Non : elle est cachée dans un champ privé |
| Utilisable avec `new` hors conteneur sans dépendance `null` ? | Oui : on est obligé de fournir le repository | Non : le champ reste `null` (`NullPointerException` au premier appel) |
| Signalé par SonarQube for IDE ? | Non | Oui (règle S6813, « field injection is not recommended » — à confirmer dans le panneau de votre IDE) |

Convention AutoLoc retenue : injection par constructeur, champs `final`, dépendance typée par une interface.

## B. Services

Tous les services sont des `@Service` singletons sans état, avec `@RequiredArgsConstructor` et un champ `final`
typé par l'interface du repository.

| Service | Dépendances injectées | Mode | Justification |
|---|---|---|---|
| AgenceServiceImpl | IAgenceRepository | Constructeur | CRUD ; refuse une agence sans nom. |
| EmployeServiceImpl | IEmployeRepository | Constructeur | CRUD ; refuse un employé sans nom ou sans prénom. |
| VehiculeServiceImpl | IVehiculeRepository | Constructeur | CRUD ; refuse un tarif journalier absent ou négatif. |
| EquipementServiceImpl | IEquipementRepository | Constructeur | CRUD ; refuse un équipement sans libellé (le modèle n'a pas d'attribut « quantité »). |
| ClientServiceImpl | IClientRepository | Constructeur | CRUD ; refuse un client sans nom ou sans e-mail. |
| ReservationServiceImpl | IReservationRepository | Constructeur | CRUD ; refuse une date de fin antérieure à la date de début. |
| ContratServiceImpl | IContratRepository | Constructeur | CRUD ; refuse un montant total négatif ; la suppression propage aux paiements par cascade. |
| PaiementServiceImpl | IPaiementRepository | Constructeur | Lecture seule (`findById`, `findAll`) : un paiement ne vit que rattaché à son contrat. |
| MaintenanceServiceImpl | IMaintenanceRepository | Constructeur | CRUD ; refuse une date de fin antérieure à la date de début (le modèle n'a pas d'attribut « coût »). |

Règles communes : `create` refuse un identifiant non `null` (sinon `save` ferait un `merge` et pourrait écraser une
ligne existante) ; `update` valide, charge l'entité existante puis recopie les attributs modifiables
(jamais l'identifiant, ni les collections ni les associations) ; `findById` et `deleteById` lèvent
`ResourceNotFoundException` si l'identifiant est inconnu.

## C. Messages d'erreur du conteneur

**Message 1 — `IContratService` introuvable (`NoSuchBeanDefinitionException`).**
Bean concerné : celui de type `IContratService`, demandé par `ContratController`. Causes probables côté
`ContratServiceImpl` : (1) l'annotation `@Service` est oubliée, donc aucune définition de bean n'est créée ;
(2) la classe est placée hors de `tn.esprit.autoloc`, donc le scan de `@SpringBootApplication` ne la voit pas.
Correction : ajouter `@Service` ou déplacer la classe dans le package racine ou l'un de ses sous-packages.

**Message 2 — deux beans pour un seul paramètre (`NoUniqueBeanDefinitionException`).**
Bean concerné : le paramètre 0 du constructeur de `NotificationController` ; candidats `emailNotificateur` et
`smsNotificateur`. Deux corrections possibles :
- `@Primary` **sur la classe** du bean à privilégier (par exemple `EmailNotificateur`) : il devient le candidat par défaut ;
- `@Qualifier("smsNotificateur")` **sur le paramètre du constructeur** de `NotificationController` : choix explicite,
  fait à l'endroit de l'injection (nom par défaut du bean = nom de la classe avec la première lettre en minuscule).

**Message 3 — cycle `clientServiceImpl` ↔ `reservationServiceImpl` (dépendance circulaire).**
Avec l'injection par constructeur, chaque bean doit être entièrement construit avant d'être fourni à l'autre :
`clientServiceImpl` a besoin de `reservationServiceImpl` pour exister, et inversement. Aucun des deux ne peut être
créé en premier, et il n'y a pas d'objet « à moitié construit » à injecter, contrairement au setter ou à l'attribut
qui masqueraient le problème. Piste de conception : supprimer la dépendance mutuelle en extrayant la responsabilité
commune dans un troisième service, ou en faisant dépendre l'un des deux services uniquement du repository de
l'autre (`IReservationRepository` dans `ClientServiceImpl`, par exemple).

## D. Anomalies SonarQube for IDE

> Cette partie se remplit en lançant l'analyse dans votre IntelliJ (clic droit sur les packages `service` et
> `exception` → *Analyze with SonarQube for IDE*). Les deux premières lignes décrivent les expériences de
> l'énoncé ; vérifiez le numéro de règle et le libellé exacts dans le panneau, et ajoutez au moins une anomalie
> réellement relevée sur votre code.

| Anomalie | Règle | Explication | Correction |
|---|---|---|---|
| Injection par attribut (`@Autowired` sur le champ, étape 2) | S6813 (à confirmer) | Champ non `final`, dépendance cachée, `null` hors conteneur. | Retour à l'injection par constructeur avec `@RequiredArgsConstructor` et un champ `final`. |
| Exception générique `RuntimeException` dans `findById` (étape 4) | S112 (à confirmer) | Une exception générique ne dit rien sur l'erreur et force les appelants à attraper trop large. | Restauration de `ResourceNotFoundException`, exception métier explicite. |
| À compléter | à compléter | Anomalie relevée sur votre code (import inutile, nom non conforme, commentaire de code…). | à compléter |

## E. Questions de compréhension

1. **Où est le `new` de `ContratServiceImpl` et qui l'exécute ?** Il n'est écrit nulle part dans le code applicatif :
   c'est le conteneur Spring qui appelle le constructeur au démarrage, après avoir détecté `@Service` par le scan
   des composants, et qui lui fournit le proxy de `IContratRepository`.
2. **Pourquoi un contrôleur dépendra-t-il de `IContratService` et non de `ContratServiceImpl` ?** Pour dépendre d'une
   abstraction : on peut changer ou remplacer l'implémentation (autre version, double de test) sans modifier le
   contrôleur, ce qui garde les couches faiblement couplées.
3. **Pourquoi un service singleton doit-il rester sans état ?** Une seule instance est partagée par tous les
   threads. Exemple de bug : un attribut `Contrat contratCourant` modifié par `findById` ; deux requêtes simultanées
   s'écrasent mutuellement et l'une travaille sur le contrat de l'autre.
4. **Pourquoi `update` charge-t-il l'entité existante avant `save` ?** Passer directement l'objet reçu à `save`
   écraserait toutes les colonnes (celles non renseignées deviendraient `null`) et, avec `orphanRemoval`, une
   collection de paiements vide pourrait supprimer les paiements existants. Charger puis recopier ne modifie que
   les attributs voulus, et permet de lever `ResourceNotFoundException` si l'identifiant n'existe pas.
5. **Différences.**
   - `@Component` se pose sur une classe que l'on écrit et que le scan détecte ; `@Bean` se pose sur une méthode
     d'une classe `@Configuration`, pour une classe qu'on ne peut pas annoter (bibliothèque, JDK) ou une
     construction particulière.
   - `@Primary` se pose sur la classe et désigne le candidat par défaut ; `@Qualifier("nom")` se pose à
     l'injection et fait un choix explicite.
   - `IPaiementService` n'expose que la lecture : un paiement n'existe que rattaché à son contrat (composition),
     sa création, sa modification et sa suppression passent donc par le `Contrat` (cascade et `orphanRemoval`).
