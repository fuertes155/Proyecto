package com.cooperativa.met.infrastructure.adapter.wompi;

import com.cooperativa.met.application.bank.usecase.SyncBankCatalogUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WompiStartupCheckTest {

    private final SyncBankCatalogUseCase syncBankCatalogUseCase = mock(SyncBankCatalogUseCase.class);

    private WompiStartupCheck check(String publicKey, String privateKey, String integrity, String events, boolean sync) {
        WompiStartupCheck check = new WompiStartupCheck(syncBankCatalogUseCase);
        ReflectionTestUtils.setField(check, "apiUrl", "https://sandbox.wompi.co/v1");
        ReflectionTestUtils.setField(check, "publicKey", publicKey);
        ReflectionTestUtils.setField(check, "privateKey", privateKey);
        ReflectionTestUtils.setField(check, "integritySecret", integrity);
        ReflectionTestUtils.setField(check, "eventsSecret", events);
        ReflectionTestUtils.setField(check, "redirectUrl", "https://met.test/api/v1/payments/wompi/return");
        ReflectionTestUtils.setField(check, "syncBanksOnStartup", sync);
        return check;
    }

    @Test
    void sandboxKeysPass() {
        assertEquals(List.of(),
                check("pub_test_a", "prv_test_b", "test_integrity_c", "test_events_d", false).credentialProblems(true));
    }

    @Test
    void productionKeyAgainstSandboxAndPlaceholdersAreReported() {
        List<String> problems = check("pub_prod_a", "prv_test_REPLACE_ME", "test_integrity_c", "wh_test_placeholder", false)
                .credentialProblems(true);

        assertEquals(3, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("WOMPI_PUBLIC_KEY"));
        assertTrue(problems.get(1).contains("WOMPI_PRIVATE_KEY"));
        assertTrue(problems.get(2).contains("WOMPI_WEBHOOK_SECRET"));
    }

    @Test
    void syncsPseBanksOnlyWhenEnabledAndNeverBlocksStartup() {
        check("pub_test_a", "prv_test_b", "test_integrity_c", "test_events_d", false)
                .run(new DefaultApplicationArguments());
        verifyNoInteractions(syncBankCatalogUseCase);

        doThrow(new IllegalStateException("Wompi caído")).when(syncBankCatalogUseCase).executePseOnly();
        check("pub_test_a", "prv_test_b", "test_integrity_c", "test_events_d", true)
                .run(new DefaultApplicationArguments());
        verify(syncBankCatalogUseCase).executePseOnly();
    }
}
