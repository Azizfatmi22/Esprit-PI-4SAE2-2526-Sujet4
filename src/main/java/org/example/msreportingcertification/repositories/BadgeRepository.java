package org.example.msreportingcertification.repositories;

import org.example.msreportingcertification.entities.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    List<Badge> findByCategory(String category);
}
