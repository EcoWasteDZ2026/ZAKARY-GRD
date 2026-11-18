# HENKEL ALGÉRIE — ZAKARY GRD
نسخة ويب قابلة للتثبيت على الهاتف والحاسوب من مجلد cloud-sync.

## إعداد قاعدة البيانات والأمان
1. أنشئ مشروع Supabase.
2. افتح SQL Editor ونفّذ كامل الملف `cloud-sync/schema.sql`.
3. فعّل Email/Password Authentication.
4. انسخ Project URL والمفتاح العام Publishable أو anon فقط. يمنع وضع service_role في الواجهة.
5. افتح التطبيق وأدخل الرابط والمفتاح، ثم أنشئ حساباً بكلمة مرور قوية.
6. سجّل الدخول بالحساب نفسه من الهاتف والحاسوب لمشاهدة البيانات المشتركة.\n7. في تطبيق Android اضغط «ربط الحساب والمزامنة» في لوحة القيادة، ثم أدخل رابط المشروع والمفتاح العام والبريد وكلمة المرور نفسها. بعد الربط، تُزامَن سجلات النفايات والعقود تلقائياً عند فتح التطبيق أو حفظ عملية، ويمكن الضغط على «مزامنة البيانات مع الحاسوب» يدوياً.\n8. إذا كانت لديك سجلات قديمة محفوظة محلياً على الهاتف، تُرفع في أول مزامنة إلى الحساب الذي سجلت الدخول به.

## الحماية
- سياسات RLS تقصر الوصول على سجلات صاحب الحساب.
- سجل تدقيق تلقائي لإضافة وتعديل وحذف العقود والسجلات.
- فحوصات تحقق للكمية والمسترجع والأسعار.
- لا تشارك مفاتيح الإدارة أو كلمات المرور.

## نشر الواجهة
فعّل GitHub Pages ليستخدم GitHub Actions كمصدر للنشر (Settings > Pages > Build and deployment > Source: GitHub Actions). بعد نجاح سير عمل Publish Henkel GRD web app ستظهر وصلة الموقع في صفحة Actions أو Settings > Pages.

## بيانات العقد المبدئية
الأسعار المدرجة مأخوذة من المعلومات المتاحة في المحادثة، ويجب مطابقتها مع النسخة الأصلية الموقعة قبل اعتماد أي فاتورة. تاريخا تحرير العقد وسريانه لم يُثبتا. معدل 0.96 كغ/100 كغ إنتاج مؤشر مرجعي قابل للمقارنة وليس حداً قانونياً مثبتاً.

## حدود النسخة
تطبيق الويب يستخدم قاعدة Supabase مشتركة بعد إعدادها. تطبيق Android الأصلي الحالي لا يزال يستخدم تخزيناً محلياً؛ لا تعتبره متزامناً مع الويب إلى أن يتم دمج مصادقة وقاعدة البيانات فيه واختبارها. التخزين دون اتصال حالياً يحفظ ملفات الواجهة فقط، أما إدخال البيانات فيتطلب اتصالاً عند الحفظ.


## Owner-only ownership and release control

The in-app **المالك والحماية** panel has no monthly schedule. Only the account whose UUID is stored as `owner_user_id` in `public.app_owner_control` can edit the ownership notice/settings or publish a new release record. Supabase Row Level Security enforces this on the database; hiding the panel in the browser is not the security boundary.

### One-time owner binding
1. Create/sign in to the intended owner's account in Supabase Auth.
2. Run the `cloud-sync/schema.sql` script in the Supabase SQL Editor.
3. In the commented `INITIAL OWNER REGISTRATION` statement at the end of that script, replace `OWNER_EMAIL` with the exact owner's login email, uncomment the statement, and run it once.
4. Do not grant the owner role to another account. Never place the Supabase service-role key in the app.

After that, the owner can open **المالك والحماية** inside ZAKARY GRD and publish a release whenever they choose. Publishing release information does not itself compile or silently install an APK: build the signed/release APK through the trusted build process, then enter its HTTPS download URL and release notes in the owner panel.

The database owner binding protects management rights on the connected service. It does not, by itself, prevent copying the APK file or prove copyright ownership in court; a separate offline signed license and protected private signing key are needed for stronger clone/ownership protection.
