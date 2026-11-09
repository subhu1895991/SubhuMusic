create table if not exists public.subhumusic_libraries (
  user_id uuid primary key references auth.users (id) on delete cascade,
  favorites jsonb not null default '[]'::jsonb,
  playlists jsonb not null default '[]'::jsonb,
  recent jsonb not null default '[]'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.subhumusic_libraries enable row level security;

drop policy if exists "Users can read their SubhuMusic library" on public.subhumusic_libraries;
create policy "Users can read their SubhuMusic library"
  on public.subhumusic_libraries for select
  to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists "Users can create their SubhuMusic library" on public.subhumusic_libraries;
create policy "Users can create their SubhuMusic library"
  on public.subhumusic_libraries for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update their SubhuMusic library" on public.subhumusic_libraries;
create policy "Users can update their SubhuMusic library"
  on public.subhumusic_libraries for update
  to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

grant select, insert, update on public.subhumusic_libraries to authenticated;
revoke all on public.subhumusic_libraries from anon;
