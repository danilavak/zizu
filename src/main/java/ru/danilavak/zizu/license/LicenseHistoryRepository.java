package ru.danilavak.zizu.license;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, Long> {
    List<LicenseHistory> findAllByLicenseIdOrderByCreatedAtAsc(Long licenseId);
}
