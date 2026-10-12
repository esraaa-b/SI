# Note Technique : Stratégies de Chargement (Fetch) et de Cascade (AutoLoc)

Cette note technique documente et justifie les choix d'architecture de persistance JPA retenus pour les associations du modèle de données de l'application **AutoLoc** (Atelier 2).

---

## 1. Principes généraux retenus

- **Stratégie de Fetch (`FetchType.LAZY`)** : 
  Par défaut, toutes les associations (`@OneToMany`, `@ManyToMany`, `@ManyToOne`, `@OneToOne`) sont configurées en **`LAZY`**.
  - Évite le problème de requêtes excessives et le surcoût de mémoire lié au chargement systématique de l'arbre complet d'entités (effet boule de neige, requêtes N+1).
  - Préserve les performances de l'application en différant le chargement aux cas où les données associées sont explicitement requises.

- **Propagation de cycle de vie (`CascadeType` & `orphanRemoval`)** :
  - La cascade n'est appliquée que lorsqu'il existe un lien fort de dépendance ou de composition métier entre deux entités.
  - La suppression en cascade (`REMOVE` / `ALL`) et `orphanRemoval = true` sont réservées aux entités qui n'ont aucun sens d'exister en dehors de leur parent (ex. `Paiement` lié à `Contrat`, `Contrat` lié à `Reservation`).
  - Pour les entités indépendantes ou partagées (ex. un véhicule vis-à-vis de son agence, un équipement partagé entre véhicules), **aucune cascade de suppression** n'est appliquée afin de garantir l'intégrité référentielle et éviter toute perte de données accidentelle.

---

## 2. Tableau récapitulatif des associations

| Association | Fetch | Cascade | Justification |
| :--- | :---: | :---: | :--- |
| **Contrat → Paiement** | `LAZY` | `ALL` + `orphanRemoval` | Un paiement n'existe que rattaché à son contrat (composition stricte) ; la suppression d'un contrat doit automatiquement entraîner la suppression de tous ses paiements associés. Le retrait d'un paiement de la liste supprime l'enregistrement orphelin en base. |
| **Agence → Vehicule** | `LAZY` | `Aucune` | Un véhicule survit à son agence : la suppression éventuelle d'une agence ne doit jamais entraîner la suppression de sa flotte (les véhicules peuvent être transférés vers une autre agence). `LAZY` évite de charger l'ensemble du parc automobile lors de la simple consultation d'une agence. |
| **Agence → Employe** | `LAZY` | `Aucune` | Les employés possèdent leur propre cycle de vie administratif indépendant de l'agence. La suppression d'une agence ne doit pas supprimer les fiches employés. Le chargement différé optimise les requêtes sur l'agence. |
| **Vehicule ↔ Equipement** | `LAZY` | `Aucune` | Les équipements (GPS, siège bébé, etc.) sont des ressources partagées et réutilisables entre plusieurs véhicules. Supprimer un véhicule ne doit en aucun cas supprimer les équipements du catalogue. Utilisation d'un `Set` pour garantir l'unicité des associations sans doublons. |
| **Client → Reservation** | `LAZY` | `PERSIST` | La création d'un client peut automatiquement persister ses réservations initiales lors d'un parcours de réservation en ligne. Cependant, aucune suppression en cascade n'est définie : supprimer un client ne doit pas effacer silencieusement l'historique comptable et contractuel des réservations. |
| **Reservation → Vehicule** | `LAZY` | `Aucune` | La réservation fait référence à un véhicule loué, mais le véhicule existe indépendamment de la réservation. Association unidirectionnelle pour ne pas alourdir l'entité `Vehicule`. `LAZY` évite de charger l'objet véhicule complet quand seules les dates de réservation sont nécessaires. |
| **Reservation ↔ Contrat** | `LAZY` | `ALL` (côté Reservation) | Le contrat est le prolongement juridique direct de la réservation (relation 1-1). La réservation pilote le cycle de vie du contrat (création, mise à jour et annulation/suppression). `LAZY` permet d'accéder à la réservation sans surcharger la mémoire avec les données contractuelles non requises immédiatement. |
| **Vehicule → Maintenance** | `LAZY` | `PERSIST` | Lors de la planification d'un entretien, la persistance du véhicule propage la création de la fiche d'intervention. En revanche, aucune cascade `REMOVE` n'est appliquée pour préserver l'historique réglementaire et technique de maintenance du véhicule. `LAZY` évite de charger toutes les fiches d'entretien lors d'un affichage du véhicule. |

---

## 3. Détails d'implémentation JPA

1. **Association Contrat ↔ Paiement (1-N bidirectionnelle)** :
   - Côté propriétaire : `Paiement` (`@ManyToOne(fetch = FetchType.LAZY) private Contrat contrat;`)
   - Côté inverse : `Contrat` (`@OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)`)

2. **Association Agence → Vehicule (1-N bidirectionnelle)** :
   - Côté propriétaire : `Vehicule` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_agence") private Agence agence;`)
   - Côté inverse : `Agence` (`@OneToMany(mappedBy = "agence", fetch = FetchType.LAZY) private List<Vehicule> vehicules = new ArrayList<>();`)

3. **Association Agence → Employe (1-N bidirectionnelle)** :
   - Côté propriétaire : `Employe` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_agence") private Agence agence;`)
   - Côté inverse : `Agence` (`@OneToMany(mappedBy = "agence", fetch = FetchType.LAZY) private List<Employe> employes = new ArrayList<>();`)

4. **Association Vehicule ↔ Equipement (N-N bidirectionnelle avec table de jointure)** :
   - Côté propriétaire : `Vehicule` (`@ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "vehicule_equipement", joinColumns = @JoinColumn(name = "id_vehicule"), inverseJoinColumns = @JoinColumn(name = "id_equipement")) private Set<Equipement> equipements = new HashSet<>();`)
   - Côté inverse : `Equipement` (`@ManyToMany(mappedBy = "equipements", fetch = FetchType.LAZY) private Set<Vehicule> vehicules = new HashSet<>();`)

5. **Association Client → Reservation (1-N bidirectionnelle)** :
   - Côté propriétaire : `Reservation` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_client") private Client client;`)
   - Côté inverse : `Client` (`@OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY) private List<Reservation> reservations = new ArrayList<>();`)

6. **Association Reservation → Vehicule (N-1 unidirectionnelle)** :
   - Côté propriétaire : `Reservation` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_vehicule") private Vehicule vehicule;`)

7. **Association Reservation ↔ Contrat (1-1 bidirectionnelle)** :
   - Côté propriétaire : `Contrat` (`@OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_reservation") private Reservation reservation;`)
   - Côté inverse : `Reservation` (`@OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY) private Contrat contrat;`)

8. **Association Vehicule → Maintenance (1-N bidirectionnelle)** :
   - Côté propriétaire : `Maintenance` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_vehicule") private Vehicule vehicule;`)
   - Côté inverse : `Vehicule` (`@OneToMany(mappedBy = "vehicule", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY) private List<Maintenance> maintenances = new ArrayList<>();`)

---

## 4. Bonnes pratiques Clean Code & Lombok appliquées

- Utilisation stricte des annotations ciblées `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`.
- **Exclusion de `@Data`, `@ToString` et `@EqualsAndHashCode` génériques** sur les entités liées afin de prévenir définitivement tout risque de récursion infinie (`StackOverflowError`) lors de la navigation bidirectionnelle des associations.
- Initialisation explicite de toutes les collections (`ArrayList` et `HashSet`) pour éliminer tout risque de `NullPointerException`.
