ALTER TABLE scheduled_savings_accounts
    ALTER COLUMN debit_day_of_week TYPE INTEGER USING debit_day_of_week::INTEGER,
    ALTER COLUMN debit_day_of_month TYPE INTEGER USING debit_day_of_month::INTEGER;
