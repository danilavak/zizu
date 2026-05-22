package ru.danilavak.zizu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.danilavak.zizu.license.LicenseHistoryRepository;
import ru.danilavak.zizu.license.LicenseRepository;
import ru.danilavak.zizu.license.LicenseTypeRepository;
import ru.danilavak.zizu.license.ProductRepository;
import ru.danilavak.zizu.model.UserRole;
import ru.danilavak.zizu.service.UserAccountService;

@SpringBootTest
@AutoConfigureMockMvc
class LicenseModuleIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LicenseTypeRepository licenseTypeRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseHistoryRepository licenseHistoryRepository;

    @Test
    void createActivateCheckRenewAndEnforceDeviceLimit() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var admin = userAccountService.register("admin_" + suffix, "admin_" + suffix + "@example.com", "Admin!234", UserRole.ADMIN);
        var user = userAccountService.register("user_" + suffix, "user_" + suffix + "@example.com", "User!234", UserRole.USER);

        String adminToken = login(admin.getUsername(), "Admin!234");
        String userToken = login(user.getUsername(), "User!234");

        Long productId = productRepository.findByName("Antivirus").orElseThrow().getId();
        Long trialTypeId = licenseTypeRepository.findByName("TRIAL").orElseThrow().getId();

        String createBody = """
                {
                  "productId": %d,
                  "typeId": %d,
                  "ownerId": %d,
                  "deviceCount": 2,
                  "description": "Trial license for integration test"
                }
                """.formatted(productId, trialTypeId, user.getId());

        String createResponse = mockMvc.perform(post("/licenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.ownerId").value(user.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createJson = objectMapper.readTree(createResponse);
        Long licenseId = createJson.get("id").asLong();
        String activationKey = createJson.get("code").asText();

        String firstDeviceMac = "AA:BB:CC:" + suffix.substring(0, 2).toUpperCase() + ":" + suffix.substring(2, 4).toUpperCase() + ":01";
        String secondDeviceMac = "AA:BB:CC:" + suffix.substring(0, 2).toUpperCase() + ":" + suffix.substring(2, 4).toUpperCase() + ":02";
        String thirdDeviceMac = "AA:BB:CC:" + suffix.substring(0, 2).toUpperCase() + ":" + suffix.substring(2, 4).toUpperCase() + ":03";

        mockMvc.perform(post("/licenses/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {
                                  "activationKey": "%s",
                                  "deviceName": "Laptop",
                                  "deviceMac": "%s"
                                }
                                """.formatted(activationKey, firstDeviceMac)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.userId").value(user.getId()))
                .andExpect(jsonPath("$.ticket.deviceId").isNumber())
                .andExpect(jsonPath("$.ticket.licenseActivationDate").isNotEmpty())
                .andExpect(jsonPath("$.ticket.licenseExpirationDate").isNotEmpty())
                .andExpect(jsonPath("$.signature").isNotEmpty());

        mockMvc.perform(post("/licenses/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {
                                  "productId": %d,
                                  "deviceMac": "%s"
                                }
                                """.formatted(productId, firstDeviceMac)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.userId").value(user.getId()))
                .andExpect(jsonPath("$.ticket.blocked").value(false))
                .andExpect(jsonPath("$.signature").isNotEmpty());

        mockMvc.perform(post("/licenses/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {
                                  "activationKey": "%s",
                                  "deviceName": "Desktop",
                                  "deviceMac": "%s"
                                }
                                """.formatted(activationKey, secondDeviceMac)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.deviceId").isNumber())
                .andExpect(jsonPath("$.signature").isNotEmpty());

        mockMvc.perform(post("/licenses/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {
                                  "activationKey": "%s",
                                  "deviceName": "Tablet",
                                  "deviceMac": "%s"
                                }
                                """.formatted(activationKey, thirdDeviceMac)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Device limit reached"));

        mockMvc.perform(post("/licenses/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {
                                  "activationKey": "%s"
                                }
                                """.formatted(activationKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.licenseExpirationDate").isNotEmpty())
                .andExpect(jsonPath("$.signature").isNotEmpty());

        mockMvc.perform(post("/licenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(createBody))
                .andExpect(status().isForbidden());

        var license = licenseRepository.findById(licenseId).orElseThrow();
        var history = licenseHistoryRepository.findAllByLicenseIdOrderByCreatedAtAsc(licenseId);

        org.assertj.core.api.Assertions.assertThat(license.getUser()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(history)
                .extracting(item -> item.getStatus().name())
                .containsExactly("CREATED", "ACTIVATED", "ACTIVATED", "RENEWED");
    }

    @Test
    void activationByAnotherUserIsForbidden() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var admin = userAccountService.register("admin2_" + suffix, "admin2_" + suffix + "@example.com", "Admin!234", UserRole.ADMIN);
        var owner = userAccountService.register("owner_" + suffix, "owner_" + suffix + "@example.com", "User!234", UserRole.USER);
        var anotherUser = userAccountService.register("another_" + suffix, "another_" + suffix + "@example.com", "User!234", UserRole.USER);

        String adminToken = login(admin.getUsername(), "Admin!234");
        String ownerToken = login(owner.getUsername(), "User!234");
        String anotherToken = login(anotherUser.getUsername(), "User!234");
        Long productId = productRepository.findByName("Antivirus").orElseThrow().getId();
        Long yearTypeId = licenseTypeRepository.findByName("YEAR").orElseThrow().getId();

        String createResponse = mockMvc.perform(post("/licenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("""
                                {
                                  "productId": %d,
                                  "typeId": %d,
                                  "ownerId": %d,
                                  "deviceCount": 1,
                                  "description": "Year license"
                                }
                                """.formatted(productId, yearTypeId, owner.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String activationKey = objectMapper.readTree(createResponse).get("code").asText();

        mockMvc.perform(post("/licenses/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + ownerToken)
                        .content("""
                                {
                                  "activationKey": "%s",
                                  "deviceName": "Owner laptop",
                                  "deviceMac": "AA:00:00:%s:%s:01"
                                }
                                """.formatted(activationKey, suffix.substring(0, 2).toUpperCase(), suffix.substring(2, 4).toUpperCase())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/licenses/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + anotherToken)
                        .content("""
                                {
                                  "activationKey": "%s",
                                  "deviceName": "Another laptop",
                                  "deviceMac": "AA:00:00:%s:%s:02"
                                }
                                """.formatted(activationKey, suffix.substring(0, 2).toUpperCase(), suffix.substring(2, 4).toUpperCase())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("License owned by another user"));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
