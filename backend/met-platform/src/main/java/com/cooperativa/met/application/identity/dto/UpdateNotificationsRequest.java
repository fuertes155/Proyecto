package com.cooperativa.met.application.identity.dto;

import lombok.Data;

@Data
public class UpdateNotificationsRequest {
    private boolean emailNotificationsEnabled;
    private boolean pushNotificationsEnabled;
}
