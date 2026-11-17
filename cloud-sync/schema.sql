-- HENKEL ZAKARY GRD | Execute once in Supabase SQL Editor.
create table if not exists public.contract_items (
 id uuid primary key default gen_random_uuid(), user_id uuid not null references auth.users(id) on delete cascade,
 name text not null, price numeric(14,3) not null default 0 check(price>=0), unit text not null default 'كغ',
 method text not null default 'بيع', updated_at timestamptz not null default now(), unique(user_id,name)
);
create table if not exists public.waste_records (
 id uuid primary key default gen_random_uuid(), user_id uuid not null references auth.users(id) on delete cascade,
 event_date date not null, waste_type text not null, department text not null default '',
 quantity numeric(14,3) not null check(quantity>=0), recovered numeric(14,3) not null default 0 check(recovered>=0 and recovered<=quantity),
 destination text not null default '', cost_dzd numeric(14,2) not null default 0 check(cost_dzd>=0),
 income_dzd numeric(14,2) not null default 0 check(income_dzd>=0), note text not null default '',
 updated_at timestamptz not null default now()
);
create table if not exists public.audit_logs (
 id bigint generated always as identity primary key, user_id uuid not null references auth.users(id) on delete cascade,
 table_name text not null, record_id text not null, action text not null, changed_at timestamptz not null default now(),
 details jsonb not null default '{}'::jsonb
);
alter table public.contract_items enable row level security;
alter table public.waste_records enable row level security;
alter table public.audit_logs enable row level security;
drop policy if exists contract_owner_all on public.contract_items;
create policy contract_owner_all on public.contract_items for all to authenticated using(auth.uid()=user_id) with check(auth.uid()=user_id);
drop policy if exists waste_owner_all on public.waste_records;
create policy waste_owner_all on public.waste_records for all to authenticated using(auth.uid()=user_id) with check(auth.uid()=user_id);
drop policy if exists audit_owner_read on public.audit_logs;
create policy audit_owner_read on public.audit_logs for select to authenticated using(auth.uid()=user_id);
create or replace function public.log_henkel_change() returns trigger language plpgsql security definer set search_path=public as $$
declare uid uuid; rid text; act text; payload jsonb;
begin
 if TG_OP='DELETE' then uid:=old.user_id; rid:=old.id::text; act:='DELETE'; payload:=to_jsonb(old);
 elsif TG_OP='UPDATE' then uid:=new.user_id; rid:=new.id::text; act:='UPDATE'; payload:=jsonb_build_object('before',to_jsonb(old),'after',to_jsonb(new));
 else uid:=new.user_id; rid:=new.id::text; act:='INSERT'; payload:=to_jsonb(new); end if;
 insert into public.audit_logs(user_id,table_name,record_id,action,details) values(uid,TG_TABLE_NAME,rid,act,payload);
 if TG_OP='DELETE' then return old; else return new; end if;
end $$;
drop trigger if exists audit_waste_records on public.waste_records;
create trigger audit_waste_records after insert or update or delete on public.waste_records for each row execute function public.log_henkel_change();
drop trigger if exists audit_contract_items on public.contract_items;
create trigger audit_contract_items after insert or update or delete on public.contract_items for each row execute function public.log_henkel_change();
create index if not exists waste_user_date_idx on public.waste_records(user_id,event_date desc);
create index if not exists audit_user_time_idx on public.audit_logs(user_id,changed_at desc);

create table if not exists public.production_logs (
 id uuid primary key default gen_random_uuid(), user_id uuid not null references auth.users(id) on delete cascade,
 production_month date not null, quantity_kg numeric(16,3) not null check(quantity_kg>=0),
 updated_at timestamptz not null default now(), unique(user_id,production_month)
);
alter table public.production_logs enable row level security;
drop policy if exists production_owner_all on public.production_logs;
create policy production_owner_all on public.production_logs for all to authenticated using(auth.uid()=user_id) with check(auth.uid()=user_id);
drop trigger if exists audit_production_logs on public.production_logs;
create trigger audit_production_logs after insert or update or delete on public.production_logs for each row execute function public.log_henkel_change();


-- Upgrade existing installations: link each contract line to its buyer and classify every waste movement.
alter table public.contract_items add column if not exists company_name text not null default 'SNC Groupe EL BADISSIA';
alter table public.waste_records add column if not exists buyer_company text not null default '';
alter table public.waste_records add column if not exists operation_type text not null default 'بيع مباشر';


-- Contract conditions transcribed from the supplied 2026 Henkel contract.
alter table public.contract_items add column if not exists contract_reference text not null default 'NEW CONTRAT 2026';
alter table public.contract_items add column if not exists signed_date date default '2026-09-29';
alter table public.contract_items add column if not exists effective_date date default '2026-08-08';
alter table public.contract_items add column if not exists duration_months integer not null default 12 check(duration_months>0);
alter table public.contract_items add column if not exists invoice_frequency text not null default 'نهاية كل شهر';
alter table public.contract_items add column if not exists payment_due_days integer not null default 10 check(payment_due_days>=0);
alter table public.contract_items add column if not exists collection_days text not null default 'الأحد إلى الخميس';
alter table public.contract_items add column if not exists collection_start_time time not null default '08:00';
alter table public.contract_items add column if not exists collection_end_time time not null default '16:00';
alter table public.contract_items add column if not exists quarterly_reporting boolean not null default true;
alter table public.contract_items add column if not exists onsite_resale_prohibited boolean not null default true;
alter table public.contract_items add column if not exists buyer_subcontracting_requires_approval boolean not null default true;
alter table public.contract_items add column if not exists prices_include_tax boolean not null default true;
alter table public.contract_items add column if not exists fixed_price_during_term boolean not null default true;
alter table public.waste_records add column if not exists invoice_number text not null default '';
alter table public.waste_records add column if not exists invoice_date date;
alter table public.waste_records add column if not exists payment_status text not null default 'غير محدد';
alter table public.waste_records add column if not exists payment_date date;
alter table public.waste_records add column if not exists dispatch_reference text not null default '';


-- Contract versioning and immutable price snapshots for accurate historical sales.
alter table public.contract_items add column if not exists contract_reference text not null default 'NEW CONTRAT 2026';
alter table public.waste_records add column if not exists contract_reference text not null default 'NEW CONTRAT 2026';
alter table public.waste_records add column if not exists unit_price numeric(14,3) not null default 0 check(unit_price>=0);
alter table public.waste_records add column if not exists sale_unit text not null default 'كغ';

-- Allow the same waste type to appear in different contracts without overwriting prior terms.
alter table public.contract_items drop constraint if exists contract_items_user_id_name_key;
create unique index if not exists contract_items_user_contract_name_uq
  on public.contract_items(user_id, contract_reference, name);
