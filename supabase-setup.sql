create table if not exists public.app_states (
    id text primary key,
    payload jsonb not null,
    updated_at timestamptz not null default timezone('utc', now())
);

alter table public.app_states enable row level security;

drop policy if exists "anon can read app_states" on public.app_states;
create policy "anon can read app_states"
on public.app_states
for select
to anon
using (true);

drop policy if exists "anon can insert app_states" on public.app_states;
create policy "anon can insert app_states"
on public.app_states
for insert
to anon
with check (true);

drop policy if exists "anon can update app_states" on public.app_states;
create policy "anon can update app_states"
on public.app_states
for update
to anon
using (true)
with check (true);

comment on table public.app_states is 'Single-record JSON storage for the Qurbani society app frontend.';
