package ru.danilavak.zizu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import ru.danilavak.zizu.license.LicenseTypeRepository;
import ru.danilavak.zizu.model.UserRole;
import ru.danilavak.zizu.service.UserAccountService;
import ru.danilavak.zizu.license.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ClientContractIntegrationTests {
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

    @Test
    void returnsLicenseStatusThroughClientEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var admin = userAccountService.register("contract_admin_" + suffix, "contract_admin_" + suffix + "@example.com", "Admin!234", UserRole.ADMIN);
        var user = userAccountService.register("contract_user_" + suffix, "contract_user_" + suffix + "@example.com", "User!234", UserRole.USER);

        String adminToken = login(admin.getUsername(), "Admin!234");
        String userToken = login(user.getUsername(), "User!234");
        Long productId = productRepository.findByName("Antivirus").orElseThrow().getId();
        Long trialTypeId = licenseTypeRepository.findByName("TRIAL").orElseThrow().getId();

        String createResponse = mockMvc.perform(post("/licenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content("""
                                {
                                  "productId": %d,
                                  "typeId": %d,
                                  "ownerId": %d,
                                  "deviceCount": 1,
                                  "description": "Client contract license"
                                }
                                """.formatted(productId, trialTypeId, user.getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String activationKey = objectMapper.readTree(createResponse).get("code").asText();
        String deviceMac = "AA:11:22:" + suffix.substring(0, 2).toUpperCase() + ":" + suffix.substring(2, 4).toUpperCase() + ":01";

        mockMvc.perform(post("/licenses/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("""
                                {
                                  "activationKey": "%s",
                                  "deviceName": "Client machine",
                                  "deviceMac": "%s"
                                }
                                """.formatted(activationKey, deviceMac)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/license/status")
                        .header("Authorization", "Bearer " + userToken)
                        .param("productId", productId.toString())
                        .param("deviceMac", deviceMac))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.userId").value(user.getId()))
                .andExpect(jsonPath("$.ticket.blocked").value(false))
                .andExpect(jsonPath("$.signature").isNotEmpty());
    }

    @Test
    void returnsUpdateManifestThroughClientEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userAccountService.register("manifest_admin_" + suffix, "manifest_admin_" + suffix + "@example.com", "Admin!234", UserRole.ADMIN);
        String adminToken = login("manifest_admin_" + suffix, "Admin!234");

        mockMvc.perform(post("/malware-signatures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "threatName": "Client.Manifest.%s",
                                  "firstBytesHex": "AA55CC33",
                                  "remainderHashHex": "1111222233334444",
                                  "remainderLength": 128,
                                  "fileType": "bin",
                                  "offsetStart": 0,
                                  "offsetEnd": 32
                                }
                                """.formatted(suffix)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/updates/manifest")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuedAt").isNotEmpty())
                .andExpect(jsonPath("$.databaseVersion").isNotEmpty())
                .andExpect(jsonPath("$.entries[?(@.threatName=='Client.Manifest.%s')]".formatted(suffix)).isArray())
                .andExpect(jsonPath("$.manifestSignatureBase64").isNotEmpty());
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

        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }
}
