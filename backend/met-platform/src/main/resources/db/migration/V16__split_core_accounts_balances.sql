ALTER TABLE core_accounts RENAME COLUMN balance TO principal_balance;
ALTER TABLE core_accounts ADD COLUMN interest_balance DECIMAL(15, 2) NOT NULL DEFAULT 50.00;
