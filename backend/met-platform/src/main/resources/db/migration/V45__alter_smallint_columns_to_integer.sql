-- V3, V4 y V5 crearon estas columnas como SMALLINT, pero las entidades las declaran int (INTEGER).
-- Funcionaba porque los valores son pequeños, pero Hibernate no podía validar el esquema contra las
-- entidades (lo comprueba PostgresMigrationsIntegrationTest). Mismo arreglo que V6; ensanchar no pierde datos.
ALTER TABLE solidarity_groups
    ALTER COLUMN max_members TYPE INTEGER USING max_members::INTEGER;

ALTER TABLE micro_loans
    ALTER COLUMN term_months TYPE INTEGER USING term_months::INTEGER;

ALTER TABLE loan_installments
    ALTER COLUMN installment_number TYPE INTEGER USING installment_number::INTEGER;

ALTER TABLE personal_loan_applications
    ALTER COLUMN term_months TYPE INTEGER USING term_months::INTEGER;

ALTER TABLE personal_loan_amortization
    ALTER COLUMN installment_number TYPE INTEGER USING installment_number::INTEGER;

ALTER TABLE regulatory_reports
    ALTER COLUMN period_year TYPE INTEGER USING period_year::INTEGER,
    ALTER COLUMN period_month TYPE INTEGER USING period_month::INTEGER;
