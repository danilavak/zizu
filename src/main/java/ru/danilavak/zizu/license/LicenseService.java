package ru.danilavak.zizu.license;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ru.danilavak.zizu.model.UserAccount;
import ru.danilavak.zizu.repository.UserAccountRepository;

@Service
public class LicenseService {
    private final ProductRepository productRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final LicenseRepository licenseRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final long ticketLifetimeSeconds;

    public LicenseService(
            ProductRepository productRepository,
            LicenseTypeRepository licenseTypeRepository,
            LicenseRepository licenseRepository,
            DeviceRepository deviceRepository,
            DeviceLicenseRepository deviceLicenseRepository,
            LicenseHistoryRepository licenseHistoryRepository,
            UserAccountRepository userAccountRepository,
            @Value("${app.license.ticket-lifetime-seconds:300}") long ticketLifetimeSeconds
    ) {
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.licenseRepository = licenseRepository;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.userAccountRepository = userAccountRepository;
        this.ticketLifetimeSeconds = ticketLifetimeSeconds;
    }

    @Transactional
    public LicenseResponse createLicense(CreateLicenseRequest request, Long adminId) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> notFound("Product not found"));
        LicenseType type = licenseTypeRepository.findById(request.typeId())
                .orElseThrow(() -> notFound("License type not found"));
        UserAccount owner = getActiveUserOrFail(request.ownerId());

        License license = new License();
        license.setCode(generateCode());
        license.setProduct(product);
        license.setType(type);
        license.setOwner(owner);
        license.setUser(null);
        license.setBlocked(false);
        license.setDeviceCount(request.deviceCount());
        license.setDescription(normalize(request.description()));

        License saved = licenseRepository.save(license);
        writeHistory(saved, LicenseHistoryStatus.CREATED, adminId, "License created");
        return LicenseResponse.from(saved);
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request, Long userId) {
        Instant now = Instant.now();
        License license = findByCodeOrFail(request.activationKey());
        UserAccount user = getActiveUserOrFail(userId);

        if (license.getUser() != null && !license.getUser().getId().equals(userId)) {
            throw forbidden("License owned by another user");
        }
        if (license.isBlocked() || license.getProduct().isBlocked()) {
            throw conflict("License is blocked");
        }

        Device device = deviceRepository.findByMacAddress(normalizeMac(request.deviceMac()))
                .map(existing -> ensureDeviceOwnedByUser(existing, userId))
                .orElseGet(() -> createDevice(user, request.deviceName(), request.deviceMac()));

        if (license.getUser() == null) {
            license.setUser(user);
            license.setFirstActivationDate(now);
            license.setEndingDate(extendFromBase(license.getEndingDate(), now, license.getType().getDefaultDurationInDays()));
            licenseRepository.save(license);
            createDeviceBinding(license, device);
            writeHistory(license, LicenseHistoryStatus.ACTIVATED, userId, "First activation");
            return buildTicketResponse(license, device);
        }

        if (deviceLicenseRepository.existsByLicenseAndDevice(license, device)) {
            return buildTicketResponse(license, device);
        }

        long activeDevices = deviceLicenseRepository.countByLicenseId(license.getId());
        if (activeDevices >= license.getDeviceCount()) {
            throw conflict("Device limit reached");
        }

        createDeviceBinding(license, device);
        writeHistory(license, LicenseHistoryStatus.ACTIVATED, userId, "Additional device activation");
        return buildTicketResponse(license, device);
    }

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request, Long userId) {
        Instant now = Instant.now();
        License license = findByCodeOrFail(request.activationKey());

        if (license.getUser() != null && !license.getUser().getId().equals(userId)) {
            throw forbidden("License owned by another user");
        }
        if (license.isBlocked() || license.getProduct().isBlocked()) {
            throw conflict("License is blocked");
        }
        if (!isRenewable(license, now)) {
            throw conflict("License is not renewable yet");
        }

        license.setEndingDate(extendFromBase(license.getEndingDate(), now, license.getType().getDefaultDurationInDays()));
        licenseRepository.save(license);
        writeHistory(license, LicenseHistoryStatus.RENEWED, userId, "License renewed");

        Device device = resolveLatestDeviceOrNull(license);
        return buildTicketResponse(license, device);
    }

    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest request, Long userId) {
        Device device = deviceRepository.findByMacAddress(normalizeMac(request.deviceMac()))
                .orElseThrow(() -> notFound("Device not found"));

        if (!device.getUserAccount().getId().equals(userId)) {
            throw forbidden("Device owned by another user");
        }

        License license = licenseRepository.findActiveByDeviceUserAndProduct(device, userId, request.productId(), Instant.now())
                .orElseThrow(() -> notFound("License not found"));
        return buildTicketResponse(license, device);
    }

    private UserAccount getActiveUserOrFail(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> notFound("User not found"));
        if (!user.isEnabled() || user.isAccountExpired() || user.isAccountLocked() || user.isCredentialsExpired()) {
            throw notFound("User not found");
        }
        return user;
    }

    private License findByCodeOrFail(String activationKey) {
        return licenseRepository.findByCode(normalizeRequired(activationKey, "activationKey"))
                .orElseThrow(() -> notFound("License not found"));
    }

    private Device ensureDeviceOwnedByUser(Device device, Long userId) {
        if (!device.getUserAccount().getId().equals(userId)) {
            throw forbidden("Device owned by another user");
        }
        return device;
    }

    private Device createDevice(UserAccount user, String deviceName, String deviceMac) {
        Device device = new Device();
        device.setUserAccount(user);
        device.setName(normalizeRequired(deviceName, "deviceName"));
        device.setMacAddress(normalizeMac(deviceMac));
        return deviceRepository.save(device);
    }

    private void createDeviceBinding(License license, Device device) {
        DeviceLicense binding = new DeviceLicense();
        binding.setLicense(license);
        binding.setDevice(device);
        deviceLicenseRepository.save(binding);
    }

    private void writeHistory(License license, LicenseHistoryStatus status, Long userId, String details) {
        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setStatus(status);
        history.setUserAccount(getActiveUserOrFail(userId));
        history.setDetails(details);
        licenseHistoryRepository.save(history);
    }

    private TicketResponse buildTicketResponse(License license, Device device) {
        Ticket ticket = new Ticket(
                Instant.now(),
                ticketLifetimeSeconds,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                license.getUser() == null ? null : license.getUser().getId(),
                device == null ? null : device.getId(),
                license.isBlocked()
        );
        return new TicketResponse(ticket, null);
    }

    private Device resolveLatestDeviceOrNull(License license) {
        return deviceLicenseRepository.findTopByLicenseIdOrderByActivatedAtDesc(license.getId())
                .map(DeviceLicense::getDevice)
                .orElse(null);
    }

    private boolean isRenewable(License license, Instant now) {
        if (license.getUser() == null || license.getFirstActivationDate() == null) {
            return true;
        }
        Instant endingDate = license.getEndingDate();
        return endingDate == null || !endingDate.isAfter(now.plus(7, ChronoUnit.DAYS));
    }

    private Instant extendFromBase(Instant currentEndingDate, Instant now, int durationInDays) {
        Instant base = currentEndingDate != null && currentEndingDate.isAfter(now) ? currentEndingDate : now;
        return base.plus(durationInDays, ChronoUnit.DAYS);
    }

    private String generateCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw badRequest("Field " + field + " is required");
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeMac(String macAddress) {
        return normalizeRequired(macAddress, "deviceMac").replace('-', ':').toUpperCase(Locale.ROOT);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    public record CreateLicenseRequest(Long productId, Long typeId, Long ownerId, int deviceCount, String description) {
    }

    public record ActivateLicenseRequest(String activationKey, String deviceName, String deviceMac) {
    }

    public record RenewLicenseRequest(String activationKey) {
    }

    public record CheckLicenseRequest(Long productId, String deviceMac) {
    }

    public record LicenseResponse(
            Long id,
            String code,
            Long productId,
            String productName,
            Long typeId,
            String typeName,
            Long ownerId,
            Long userId,
            Instant firstActivationDate,
            Instant endingDate,
            boolean blocked,
            int deviceCount,
            String description
    ) {
        static LicenseResponse from(License license) {
            return new LicenseResponse(
                    license.getId(),
                    license.getCode(),
                    license.getProduct().getId(),
                    license.getProduct().getName(),
                    license.getType().getId(),
                    license.getType().getName(),
                    license.getOwner().getId(),
                    license.getUser() == null ? null : license.getUser().getId(),
                    license.getFirstActivationDate(),
                    license.getEndingDate(),
                    license.isBlocked(),
                    license.getDeviceCount(),
                    license.getDescription()
            );
        }
    }
}
