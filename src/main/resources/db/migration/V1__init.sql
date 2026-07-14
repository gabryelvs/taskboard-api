create table users (
    id            uuid primary key default gen_random_uuid(),
    email         text not null unique,
    password_hash text not null,
    name          text not null,
    created_at    timestamptz not null default now()
);

create table refresh_tokens (
    id         uuid primary key default gen_random_uuid(),
    token_hash text not null unique,
    user_id    uuid not null references users(id) on delete cascade,
    expires_at timestamptz not null,
    revoked    boolean not null default false
);

create table projects (
    id          uuid primary key default gen_random_uuid(),
    name        text not null,
    description text,
    owner_id    uuid not null references users(id),
    created_at  timestamptz not null default now()
);

create table project_members (
    id         uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    user_id    uuid not null references users(id) on delete cascade,
    role       text not null check (role in ('OWNER', 'MEMBER')),
    joined_at  timestamptz not null default now(),
    unique (project_id, user_id)
);

create table board_columns (
    id         uuid primary key default gen_random_uuid(),
    project_id uuid not null references projects(id) on delete cascade,
    name       text not null,
    position   int  not null
);

create table cards (
    id            uuid primary key default gen_random_uuid(),
    column_id     uuid not null references board_columns(id) on delete cascade,
    title         text not null,
    description   text,
    priority      text not null default 'MEDIUM'
                  check (priority in ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    deadline      timestamptz,
    position      int  not null,
    assignee_id   uuid references users(id) on delete set null,
    created_by_id uuid not null references users(id),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create table comments (
    id         uuid primary key default gen_random_uuid(),
    card_id    uuid not null references cards(id) on delete cascade,
    author_id  uuid not null references users(id),
    body       text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_members_user on project_members(user_id);
create index idx_columns_project on board_columns(project_id);
create index idx_cards_column on cards(column_id);
create index idx_cards_deadline on cards(deadline);
create index idx_comments_card on comments(card_id);
