-- investment_returns.investment_id se reutiliza ahora tanto para MicroInvestment (CDT)
-- como para el motor de distribución de capital P2P (PaymentDistributionUseCase), donde
-- guarda el id del PersonalLoanApplication en vez de un micro_investments.id. La FK original
-- solo tenía sentido cuando la tabla era exclusiva del primer sistema.
ALTER TABLE investment_returns DROP CONSTRAINT investment_returns_investment_id_fkey;
