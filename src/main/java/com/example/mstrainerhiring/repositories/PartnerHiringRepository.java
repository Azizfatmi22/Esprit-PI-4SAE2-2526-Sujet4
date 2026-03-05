package com.example.mstrainerhiring.repositories;

import com.example.mstrainerhiring.entities.PartnerHiring;
import com.example.mstrainerhiring.enums.PartnerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PartnerHiringRepository extends JpaRepository<PartnerHiring, UUID> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Page<PartnerHiring> findByStatus(PartnerStatus status, Pageable pageable);
}
