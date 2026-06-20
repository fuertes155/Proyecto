-- Refresh tokens para rotación y revocación
create table if not exists refresh_tokens (
    jti uuid primary key,
    user_id uuid not null,
    issued_at timestamp not null,
    expires_at timestamp not null,
    revoked boolean not null default false,
    version bigint
);

create index if not exists idx_refresh_tokens_user_id on refresh_tokens (user_id);
create index if not exists idx_refresh_tokens_revoked on refresh_tokens (revoked);
