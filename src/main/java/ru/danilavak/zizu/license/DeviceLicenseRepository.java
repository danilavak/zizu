package ru.danilavak.zizu.license;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {
    long countByLicenseId(Long licenseId);

    boolean existsByLicenseAndDevice(License license, Device device);

    Optional<DeviceLicense> findByLicenseAndDevice(License license, Device device);

    Optional<DeviceLicense> findTopByLicenseIdOrderByActivatedAtDesc(Long licenseId);
}
