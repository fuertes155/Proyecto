package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Las listas restrictivas cambian con el tiempo (una persona puede ser
 * sancionada DESPUÉS de haberse vinculado). El screening de KYC solo corre
 * una vez, al registrarse — este caso de uso vuelve a screenear a todos los
 * usuarios activos cada vez que se refresca una lista, para detectar
 * coincidencias nuevas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RescreenActiveUsersUseCase {

    private final UserRepositoryPort userRepository;
    private final ComplianceCheckPort complianceCheckPort;

    public int execute() {
        int newMatches = 0;
        for (User user : userRepository.findAll()) {
            if (user.getStatus() != UserStatus.ACTIVE) continue;

            for (ComplianceListType listType : new ComplianceListType[]{ComplianceListType.OFAC, ComplianceListType.ONU}) {
                ComplianceResult result = complianceCheckPort.checkUser(user.getId(), listType);
                complianceCheckPort.persistCheck(user.getId(), listType, result, "{\"context\":\"PERIODIC_RESCREEN\"}");
                if (result == ComplianceResult.MATCH) {
                    newMatches++;
                    log.warn("Re-screening: posible coincidencia en lista {} para userId={}", listType, user.getId());
                }
            }
        }
        log.info("Re-screening periódico completado. {} coincidencias detectadas.", newMatches);
        return newMatches;
    }
}
