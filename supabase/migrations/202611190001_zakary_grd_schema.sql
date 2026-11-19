-- ZAKARY GRD database foundation
-- Review this migration against the live Supabase schema before execution.
-- It is intentionally NOT executed against the live project by this commit.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  role text not null default 'employee' check (role in ('admin', 'employee')),
  is_active boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.waste_types (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  is_contract_category boolean not null default false,
  payment_enabled boolean not null default false,
  contract_rate numeric(14,2),
  default_unit text,
  allowed_units text[] not null default array['kg','tonne','litre','unit']::text[],
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  constraint waste_types_rate_nonnegative check (contract_rate is null or contract_rate >= 0),
  constraint waste_types_default_unit_allowed check (default_unit is null or default_unit = any(allowed_units))
);

create table if not exists public.waste_records (
  id uuid primary key default gen_random_uuid(),
  operation_date date not null default current_date,
  waste_type_id uuid not null references public.waste_types(id),
  quantity numeric(14,3) not null check (quantity > 0),
  recovered_quantity numeric(14,3) not null default 0 check (recovered_quantity >= 0 and recovered_quantity <= quantity),
  unit text not null,
  department text not null default '',
  destination text not null default '',
  treatment_method text not null default '',
  cost_dzd numeric(14,2) not null default 0 check (cost_dzd >= 0),
  income_dzd numeric(14,2) not null default 0 check (income_dzd >= 0),
  notes text not null default '',
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.sevira_records (
  id uuid primary key default gen_random_uuid(),
  waste_type text not null,
  quantity numeric(14,3) not null check (quantity >= 0),
  unit text not null,
  handling_method text not null,
  destination text not null default '',
  notes text not null default '',
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create or replace function public.zakary_is_active()
returns boolean language sql stable security definer set search_path = public
as $$
  select exists (select 1 from public.profiles p where p.id = auth.uid() and p.is_active = true);
$$;

create or replace function public.zakary_is_admin()
returns boolean language sql stable security definer set search_path = public
as $$
  select exists (select 1 from public.profiles p where p.id = auth.uid() and p.is_active = true and p.role = 'admin');
$$;

create or replace function public.zakary_create_profile()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  insert into public.profiles (id, display_name, role, is_active)
  values (new.id, coalesce(new.raw_user_meta_data->>'display_name',''), 'employee', false)
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created_zakary on auth.users;
create trigger on_auth_user_created_zakary
after insert on auth.users
for each row execute procedure public.zakary_create_profile();

alter table public.profiles enable row level security;
alter table public.waste_types enable row level security;
alter table public.waste_records enable row level security;
alter table public.sevira_records enable row level security;

drop policy if exists "profiles_select_self_or_admin" on public.profiles;
create policy "profiles_select_self_or_admin" on public.profiles
for select to authenticated using (id = auth.uid() or public.zakary_is_admin());

drop policy if exists "profiles_update_admin_only" on public.profiles;
create policy "profiles_update_admin_only" on public.profiles
for update to authenticated using (public.zakary_is_admin()) with check (public.zakary_is_admin());

drop policy if exists "waste_types_read_active_users" on public.waste_types;
create policy "waste_types_read_active_users" on public.waste_types
for select to authenticated using (public.zakary_is_active());

drop policy if exists "waste_types_admin_manage" on public.waste_types;
create policy "waste_types_admin_manage" on public.waste_types
for all to authenticated using (public.zakary_is_admin()) with check (public.zakary_is_admin());

drop policy if exists "waste_records_read_active_users" on public.waste_records;
create policy "waste_records_read_active_users" on public.waste_records
for select to authenticated using (public.zakary_is_active());

drop policy if exists "waste_records_insert_active_users" on public.waste_records;
create policy "waste_records_insert_active_users" on public.waste_records
for insert to authenticated with check (public.zakary_is_active() and created_by = auth.uid());

drop policy if exists "waste_records_update_owner_or_admin" on public.waste_records;
create policy "waste_records_update_owner_or_admin" on public.waste_records
for update to authenticated using (public.zakary_is_active() and (created_by = auth.uid() or public.zakary_is_admin()))
with check (public.zakary_is_active() and (created_by = auth.uid() or public.zakary_is_admin()));

drop policy if exists "waste_records_delete_admin" on public.waste_records;
create policy "waste_records_delete_admin" on public.waste_records
for delete to authenticated using (public.zakary_is_admin());

drop policy if exists "sevira_read_active_users" on public.sevira_records;
create policy "sevira_read_active_users" on public.sevira_records
for select to authenticated using (public.zakary_is_active());

drop policy if exists "sevira_insert_active_users" on public.sevira_records;
create policy "sevira_insert_active_users" on public.sevira_records
for insert to authenticated with check (public.zakary_is_active() and created_by = auth.uid());

drop policy if exists "sevira_update_owner_or_admin" on public.sevira_records;
create policy "sevira_update_owner_or_admin" on public.sevira_records
for update to authenticated using (public.zakary_is_active() and (created_by = auth.uid() or public.zakary_is_admin()))
with check (public.zakary_is_active() and (created_by = auth.uid() or public.zakary_is_admin()));

drop policy if exists "sevira_delete_admin" on public.sevira_records;
create policy "sevira_delete_admin" on public.sevira_records
for delete to authenticated using (public.zakary_is_admin());

insert into public.waste_types (name, is_contract_category, payment_enabled, contract_rate, default_unit, allowed_units)
values
 ('Caisses Cartons et déchets de papiers', true, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Film Plastique (étirable, houssage)', true, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Big Bags', true, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Fûts Métalliques', true, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Déchets palettes en bois et débris de bois', true, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Emballages contaminés par produit chimique', false, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Déchets cantine', false, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Déchets laboratoire', false, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Déchets EPI contaminés', false, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Déchets huile usagée', false, false, null, null, array['kg','tonne','litre','unit']::text[]),
 ('Construction waste', false, false, null, null, array['kg','tonne','litre','unit']::text[])
on conflict (name) do nothing;

-- Client access is granted only to authenticated users; RLS policies above
-- decide which rows each authenticated user may read or modify.
grant usage on schema public to authenticated;
grant select, update on public.profiles to authenticated;
grant select, insert, update, delete on public.waste_types to authenticated;
grant select, insert, update, delete on public.waste_records to authenticated;
grant select, insert, update, delete on public.sevira_records to authenticated;

-- Do not insert a default administrator or activate accounts automatically.
-- The first administrator must be assigned deliberately after the owner account is verified.
-- Payment is disabled and all contract rates are NULL until the user confirms active paid categories.
