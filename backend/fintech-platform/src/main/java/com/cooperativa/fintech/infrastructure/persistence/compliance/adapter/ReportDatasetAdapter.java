package com.cooperativa.fintech.infrastructure.persistence.compliance.adapter;

import com.cooperativa.fintech.domain.compliance.model.ReportDataRow;
import com.cooperativa.fintech.domain.compliance.model.ReportDataset;
import com.cooperativa.fintech.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.fintech.domain.compliance.port.ReportDatasetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportDatasetAdapter implements ReportDatasetPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ReportDataset buildDataset(SupersolidariaReportType type, int year, int month, String entityCode) {
        return switch (type) {
            case ASOCIADOS -> buildAsociados(year, month, entityCode);
            case AHORROS_PROGRAMADOS -> buildAhorrosProgramados(year, month, entityCode);
            case CREDITO_PERSONAL -> buildCreditoPersonal(year, month, entityCode);
            case AHORRO_SOLIDARIO -> buildAhorroSolidario(year, month, entityCode);
            case SARLAFT -> buildSarlaft(year, month, entityCode);
        };
    }

    private ReportDataset buildAsociados(int year, int month, String entityCode) {
        String sql = """
                SELECT document_type, document_number, first_name, last_name, email, status, kyc_status,
                       TO_CHAR(created_at, 'YYYYMMDD')
                FROM users
                WHERE EXTRACT(YEAR FROM created_at) <= ? AND EXTRACT(MONTH FROM created_at) <= ?
                ORDER BY created_at
                """;
        List<ReportDataRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> ReportDataRow.builder()
                .fields(List.of(
                        String.valueOf(rowNum + 1),
                        rs.getString("document_type"),
                        rs.getString("document_number"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getString("kyc_status"),
                        rs.getString(8)
                ))
                .build(), year, month);

        return baseDataset(SupersolidariaReportType.ASOCIADOS, year, month, entityCode,
                List.of("SECUENCIA", "TIPO_DOC", "NUM_DOC", "NOMBRES", "APELLIDOS", "EMAIL", "ESTADO", "KYC", "FECHA_VINCULACION"),
                rows);
    }

    private ReportDataset buildAhorrosProgramados(int year, int month, String entityCode) {
        String sql = """
                SELECT u.document_number, s.name, s.contribution_amount, s.current_balance, s.status, s.frequency
                FROM scheduled_savings_accounts s
                JOIN users u ON u.id = s.user_id
                WHERE EXTRACT(YEAR FROM s.created_at) = ? AND EXTRACT(MONTH FROM s.created_at) = ?
                """;
        List<ReportDataRow> rows = queryRows(sql, year, month, 6);
        return baseDataset(SupersolidariaReportType.AHORROS_PROGRAMADOS, year, month, entityCode,
                List.of("DOC_ASOCIADO", "NOMBRE_META", "APORTE", "SALDO", "ESTADO", "FRECUENCIA"), rows);
    }

    private ReportDataset buildCreditoPersonal(int year, int month, String entityCode) {
        String sql = """
                SELECT u.document_number, p.amount, p.term_months, p.monthly_payment, p.status, p.purpose
                FROM personal_loan_applications p
                JOIN users u ON u.id = p.user_id
                WHERE EXTRACT(YEAR FROM p.submitted_at) = ? AND EXTRACT(MONTH FROM p.submitted_at) = ?
                """;
        List<ReportDataRow> rows = queryRows(sql, year, month, 6);
        return baseDataset(SupersolidariaReportType.CREDITO_PERSONAL, year, month, entityCode,
                List.of("DOC_ASOCIADO", "MONTO", "PLAZO", "CUOTA", "ESTADO", "PROPOSITO"), rows);
    }

    private ReportDataset buildAhorroSolidario(int year, int month, String entityCode) {
        String sql = """
                SELECT g.name, g.pool_balance, g.status, COUNT(m.id) as members
                FROM solidarity_groups g
                LEFT JOIN solidarity_members m ON m.group_id = g.id
                WHERE EXTRACT(YEAR FROM g.created_at) = ? AND EXTRACT(MONTH FROM g.created_at) = ?
                GROUP BY g.id, g.name, g.pool_balance, g.status
                """;
        List<ReportDataRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> ReportDataRow.builder()
                .fields(List.of(
                        String.valueOf(rowNum + 1),
                        rs.getString("name"),
                        rs.getBigDecimal("pool_balance").toPlainString(),
                        rs.getString("status"),
                        rs.getString("members")
                ))
                .build(), year, month);

        return baseDataset(SupersolidariaReportType.AHORRO_SOLIDARIO, year, month, entityCode,
                List.of("SECUENCIA", "GRUPO", "FONDO", "ESTADO", "MIEMBROS"), rows);
    }

    private ReportDataset buildSarlaft(int year, int month, String entityCode) {
        String sql = """
                SELECT u.document_number, c.list_type, c.result, TO_CHAR(c.checked_at, 'YYYYMMDD')
                FROM compliance_checks c
                JOIN users u ON u.id = c.user_id
                WHERE EXTRACT(YEAR FROM c.checked_at) = ? AND EXTRACT(MONTH FROM c.checked_at) = ?
                """;
        List<ReportDataRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> ReportDataRow.builder()
                .fields(List.of(
                        String.valueOf(rowNum + 1),
                        rs.getString("document_number"),
                        rs.getString("list_type"),
                        rs.getString("result"),
                        rs.getString(4)
                ))
                .build(), year, month);

        return baseDataset(SupersolidariaReportType.SARLAFT, year, month, entityCode,
                List.of("SECUENCIA", "DOC_ASOCIADO", "LISTA", "RESULTADO", "FECHA"), rows);
    }

    private List<ReportDataRow> queryRows(String sql, int year, int month, int columnCount) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            List<String> fields = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                fields.add(rs.getString(i));
            }
            return ReportDataRow.builder().fields(fields).build();
        }, year, month);
    }

    private ReportDataset baseDataset(SupersolidariaReportType type, int year, int month,
                                       String entityCode, List<String> headers, List<ReportDataRow> rows) {
        return ReportDataset.builder()
                .reportType(type)
                .periodYear(year)
                .periodMonth(month)
                .entityCode(entityCode)
                .headerFields(headers)
                .detailRows(rows)
                .build();
    }
}
