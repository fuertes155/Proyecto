drop table if exists refresh_tokens cascade;

create table refresh_tokens (
    jti uuid primary key,
    user_id uuid not null,
    issued_at timestamp not null,
    expires_at timestamp not null,
    revoked boolean not null default false,
    version bigint
);

create index idx_refresh_tokens_user_id on refresh_tokens (user_id);
create index idx_refresh_tokens_revoked on refresh_tokens (revoked);

alter table refresh_tokens add constraint refresh_tokens_user_id_fkey foreign key (user_id) references users (id) on delete cascade;
