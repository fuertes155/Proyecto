package com.cooperativa.met.domain.compliance.model;

/**
 * OPEN: recién generada, nadie la ha visto.
 * UNDER_REVIEW: un oficial de cumplimiento la está analizando.
 * DISMISSED: se revisó y no ameritaba acción (falso positivo).
 * REPORTED: se determinó que corresponde reportar (ej. ROS a la UIAF).
 */
public enum AlertStatus {
    OPEN, UNDER_REVIEW, DISMISSED, REPORTED
}
