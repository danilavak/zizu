package ru.danilavak.zizu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import ru.danilavak.zizu.license.Ticket;
import ru.danilavak.zizu.signature.DigitalSignatureService;

@SpringBootTest
@AutoConfigureMockMvc
class DigitalSignatureServiceIntegrationTests {
    @Autowired
    private DigitalSignatureService digitalSignatureService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signsAndVerifiesTicketPayload() {
        Ticket ticket = new Ticket(
                Instant.parse("2026-01-01T00:00:00Z"),
                300,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-31T00:00:00Z"),
                42L,
                77L,
                false
        );

        String signature = digitalSignatureService.signObject(ticket);

        assertThat(signature).isNotBlank();
        assertThat(digitalSignatureService.verifyObject(ticket, signature)).isTrue();
        assertThat(digitalSignatureService.verifyObject(
                new Ticket(
                        ticket.serverDate(),
                        ticket.lifetimeSeconds(),
                        ticket.licenseActivationDate(),
                        ticket.licenseExpirationDate(),
                        ticket.userId(),
                        88L,
                        ticket.blocked()
                ),
                signature
        )).isFalse();
    }

    @Test
    void exposesPublicCertificate() throws Exception {
        mockMvc.perform(get("/signature/certificate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificatePem").isNotEmpty());
    }
}
