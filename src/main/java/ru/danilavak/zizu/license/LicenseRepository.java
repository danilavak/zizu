package ru.danilavak.zizu.license;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByCode(String code);

    @Query("""
            select l from License l
            join DeviceLicense dl on dl.license = l
            where dl.device = :device
              and l.user.id = :userId
              and l.product.id = :productId
              and l.blocked = false
              and l.product.blocked = false
              and l.endingDate is not null
              and l.endingDate >= :now
            """)
    Optional<License> findActiveByDeviceUserAndProduct(
            @Param("device") Device device,
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("now") Instant now
    );
}
