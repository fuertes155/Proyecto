package com.cooperativa.met.application.solidarity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinSolidarityGroupRequest(
        @NotBlank @Size(min = 8, max = 8) String inviteCode
) {
}
