package tn.esprit.autoloc.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicule")
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVehicule;

    @Column(nullable = false, unique = true, length = 20)
    private String immatriculation;

    @Column(nullable = false, length = 50)
    private String marque;

    @Column(nullable = false, length = 50)
    private String modele;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategorieVehicule categorie;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifJournalier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutVehicule statut;

    public Vehicule() {
    }

    public Vehicule(Long idVehicule, String immatriculation, String marque, String modele, CategorieVehicule categorie, BigDecimal tarifJournalier, StatutVehicule statut) {
        this.idVehicule = idVehicule;
        this.immatriculation = immatriculation;
        this.marque = marque;
        this.modele = modele;
        this.categorie = categorie;
        this.tarifJournalier = tarifJournalier;
        this.statut = statut;
    }

    public Long getIdVehicule() {
        return idVehicule;
    }

    public void setIdVehicule(Long idVehicule) {
        this.idVehicule = idVehicule;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public CategorieVehicule getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieVehicule categorie) {
        this.categorie = categorie;
    }

    public BigDecimal getTarifJournalier() {
        return tarifJournalier;
    }

    public void setTarifJournalier(BigDecimal tarifJournalier) {
        this.tarifJournalier = tarifJournalier;
    }

    public StatutVehicule getStatut() {
        return statut;
    }

    public void setStatut(StatutVehicule statut) {
        this.statut = statut;
    }
}