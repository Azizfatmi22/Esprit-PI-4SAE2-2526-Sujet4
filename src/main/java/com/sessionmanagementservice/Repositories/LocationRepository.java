package com.sessionmanagementservice.Repositories;



import com.sessionmanagementservice.entities.Location;
import com.sessionmanagementservice.entities.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByType(LocationType type);

    boolean existsByName(String name);

    Optional<Location> findByPlatformUrl(String platformUrl);
}
