# ZAKARY GRD — Cross-platform architecture

## Goal
Deliver one application for Android and Windows desktop, preserving the 17 November workflow and visual identity as the reference. Both clients use the same Supabase project so authenticated users see the same factory records.

## Protected baseline
- Existing Kotlin Android implementation remains unchanged on its original branch.
- This work happens on `feature/flutter-cross-platform`, created from the verified commit `12ebd29644d43b141384d068a1ada721086b3317`.
- No merge to `main` without review and successful tests.
- Do not remove the existing SEVIRA section or the unified 11-category waste register.

## Target stack
- Flutter / Dart: shared UI and business logic.
- Android: APK build.
- Windows: native desktop build, then package the Windows output as an installer.
- Supabase: email/password authentication, PostgreSQL records, Row Level Security, and optional Realtime updates.

## Application sections
1. Sign in / account access.
2. Dashboard.
3. New waste operation.
4. Unified waste register (11 categories).
5. Separate costs and income by waste category.
6. SEVIRA register (separate data and fields).
7. Reports, CSV export, and backup/restore.
8. User and access management (administrator only).

## Waste data requirements
- A waste operation stores its own measurement unit; do not assume every category is measured in kg.
- Support kg, tonne, litre, and unit as configurable units. Confirm the default unit for each category before enforcing it.
- Costs and income are entered per operation and aggregated per waste category. Never distribute one category's costs across other categories.
- Contract membership and payment eligibility are separate flags. Do not automatically apply contract prices or calculate revenue for any category until the user confirms which categories are currently paid.
- Keep the five contract categories and six additional categories in one register.

## Access model
- Email/password only; anonymous access disabled.
- New accounts are inactive by default until an administrator activates them.
- Roles: administrator and employee.
- Employees can view shared factory records and create/update their own records; administrators can manage users, categories, and all records.
- Enforce permissions in Supabase Row Level Security, not only in the app UI.
- Never place a Supabase service-role key in a mobile or desktop client.

## Data model draft
- `profiles`: auth user id, display name, role, active flag, timestamps.
- `waste_types`: category name, contract flag, payment-enabled flag, optional contract rate, default unit, allowed units, active flag.
- `waste_records`: date, waste type, quantity, recovered quantity, unit, department, destination, treatment method, cost, income, notes, creator and timestamps.
- `sevira_records`: independent SEVIRA waste type, quantity, unit, handling method, destination, notes, creator and timestamps.

## Delivery and acceptance tests
- Same account can sign in on Android and Windows.
- A record created on either device appears on the other after sync.
- SEVIRA remains independent from the main waste register.
- All 11 waste categories are available.
- Unit validation and totals use the selected unit correctly.
- Financial summaries remain separated by waste category.
- Employee/admin permissions are enforced by database policies.
- APK installs and runs on Android.
- Windows package installs and launches on a supported Windows PC.
- Export and backup work without losing Arabic/French text.

## Known limitations to resolve before database deployment
- The Supabase database schema and current RLS policies have not yet been inspected.
- The original SEVIRA source document is not currently available in this project review; retain the current separate section until its exact fields can be checked.
- The approved launcher artwork must be obtained as an actual image asset before it can be embedded; the repository currently points to a vector recycle icon.
