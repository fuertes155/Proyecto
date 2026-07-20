package com.cooperativa.met.application.identity.dto;

import lombok.Data;

@Data
public class UpdateNotificationsRequest {
    private Boolean emailNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
}
