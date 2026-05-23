package ru.danilavak.zizu.license;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.danilavak.zizu.common.security.AuthenticatedUser;

@RestController
@RequestMapping("/license")
public class LicenseStatusController {
    private final LicenseService licenseService;

    public LicenseStatusController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping("/status")
    public TicketResponse licenseStatus(
            @RequestParam("productId") @NotNull Long productId,
            @RequestParam("deviceMac") @NotBlank @Size(max = 64) String deviceMac,
            Authentication authentication
    ) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();
        return licenseService.checkLicense(
                new LicenseService.CheckLicenseRequest(productId, deviceMac),
                currentUser.userId()
        );
    }
}
