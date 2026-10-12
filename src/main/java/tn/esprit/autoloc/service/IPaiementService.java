package tn.esprit.autoloc.service;

import java.util.List;
import tn.esprit.autoloc.domain.Paiement;

/**
 * Lecture seule : un paiement est créé, modifié et supprimé via son Contrat (composition).
 */
public interface IPaiementService {

    Paiement findById(Long id);

    List<Paiement> findAll();
}
