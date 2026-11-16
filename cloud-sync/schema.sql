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
