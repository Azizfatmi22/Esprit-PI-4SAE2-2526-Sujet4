package tn.esprit.mucroservice.msenrollment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.mucroservice.msenrollment.entities.Invoice;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Trouver toutes les factures d'un étudiant spécifique
    @Query("SELECT DISTINCT i FROM Invoice i WHERE i.learnerId = :learnerId")
    List<Invoice> findByLearnerId(String learnerId);

    // Trouver une facture par son numéro unique (ex: INV-2026-001)
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Trouver la facture associée à un paiement spécifique
    Optional<Invoice> findByPaymentId(Long paymentId);

    // Vérifier si un numéro de facture existe déjà
    boolean existsByInvoiceNumber(String invoiceNumber);

}