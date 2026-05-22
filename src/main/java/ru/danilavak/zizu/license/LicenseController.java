package ru.danilavak.zizu.license;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.danilavak.zizu.common.security.AuthenticatedUser;

@RestController
@RequestMapping("/licenses")
public class LicenseController {
    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LicenseService.LicenseResponse createLicense(
            @Valid @RequestBody CreateLicenseRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();
        return licenseService.createLicense(request.toServiceRequest(), currentUser.userId());
    }

    @PostMapping("/activate")
    public TicketResponse activateLicense(
            @Valid @RequestBody ActivateLicenseRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();
        return licenseService.activateLicense(request.toServiceRequest(), currentUser.userId());
    }

    @PostMapping("/renew")
    public TicketResponse renewLicense(
            @Valid @RequestBody RenewLicenseRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();
        return licenseService.renewLicense(request.toServiceRequest(), currentUser.userId());
    }

    @PostMapping("/check")
    public TicketResponse checkLicense(
            @Valid @RequestBody CheckLicenseRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();
        return licenseService.checkLicense(request.toServiceRequest(), currentUser.userId());
    }

    public record CreateLicenseRequest(
            @NotNull Long productId,
            @NotNull Long typeId,
            @NotNull Long ownerId,
            @Min(1) int deviceCount,
            @Size(max = 255) String description
    ) {
        LicenseService.CreateLicenseRequest toServiceRequest() {
            return new LicenseService.CreateLicenseRequest(productId, typeId, ownerId, deviceCount, description);
        }
    }

    public record ActivateLicenseRequest(
            @NotBlank String activationKey,
            @NotBlank @Size(max = 120) String deviceName,
            @NotBlank @Size(max = 64) String deviceMac
    ) {
        LicenseService.ActivateLicenseRequest toServiceRequest() {
            return new LicenseService.ActivateLicenseRequest(activationKey, deviceName, deviceMac);
        }
    }

    public record RenewLicenseRequest(@NotBlank String activationKey) {
        LicenseService.RenewLicenseRequest toServiceRequest() {
            return new LicenseService.RenewLicenseRequest(activationKey);
        }
    }

    public record CheckLicenseRequest(
            @NotNull Long productId,
            @NotBlank @Size(max = 64) String deviceMac
    ) {
        LicenseService.CheckLicenseRequest toServiceRequest() {
            return new LicenseService.CheckLicenseRequest(productId, deviceMac);
        }
    }
}
