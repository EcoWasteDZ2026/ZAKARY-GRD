import 'package:material_ui/material_ui.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

const _supabaseUrl = String.fromEnvironment('SUPABASE_URL');
const _supabaseKey = String.fromEnvironment('SUPABASE_PUBLISHABLE_KEY');
bool get supabaseConfigured => _supabaseUrl.isNotEmpty && _supabaseKey.isNotEmpty;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  if (supabaseConfigured) {
    await Supabase.initialize(url: _supabaseUrl, publishableKey: _supabaseKey);
  }
  runApp(const ZakaryApp());
}

class ZakaryApp extends StatefulWidget {
  const ZakaryApp({super.key});
  @override
  State<ZakaryApp> createState() => _ZakaryAppState();
}

class _ZakaryAppState extends State<ZakaryApp> {
  bool french = false;
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ZAKARY GRD',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF247A5A)),
        scaffoldBackgroundColor: const Color(0xFFF3F7F6),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF173650),
          foregroundColor: Colors.white,
          centerTitle: false,
        ),
        cardTheme: CardThemeData(
          elevation: 1,
          color: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
      ),
      home: supabaseConfigured
          ? AuthGate(french: french, onLanguage: () => setState(() => french = !french))
          : SetupRequired(french: french, onLanguage: () => setState(() => french = !french)),
    );
  }
}

class SetupRequired extends StatelessWidget {
  const SetupRequired({super.key, required this.french, required this.onLanguage});
  final bool french;
  final VoidCallback onLanguage;
  @override
  Widget build(BuildContext context) => Directionality(
    textDirection: french ? TextDirection.ltr : TextDirection.rtl,
    child: Scaffold(
      appBar: AppBar(title: const Text('ZAKARY GRD'), actions: [
        TextButton(onPressed: onLanguage, child: Text(french ? 'العربية' : 'Français', style: const TextStyle(color: Colors.white))),
      ]),
      body: Center(child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 520),
        child: Card(child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.recycling, size: 72, color: Color(0xFF247A5A)),
            const SizedBox(height: 16),
            Text(french ? 'Configuration de connexion requise' : 'يلزم إعداد الاتصال بقاعدة البيانات',
              style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            Text(french
              ? 'La structure de l’application est prête. Ajoutez l’URL Supabase et la clé publishable au moment de la compilation pour activer la connexion sécurisée.'
              : 'هيكل التطبيق جاهز. يلزم تمرير رابط مشروع Supabase والمفتاح العام المخصص للتطبيق أثناء البناء لتفعيل تسجيل الدخول والبيانات المشتركة.',
              textAlign: TextAlign.center),
            const SizedBox(height: 12),
            Text(french
              ? 'Aucune clé secrète ou service_role ne doit être intégrée dans cette application.'
              : 'لن نضع أي مفتاح سري أو service_role داخل التطبيق.',
              style: const TextStyle(color: Colors.redAccent), textAlign: TextAlign.center),
          ]),
        )),
      )),
    ),
  );
}

class AuthGate extends StatelessWidget {
  const AuthGate({super.key, required this.french, required this.onLanguage});
  final bool french;
  final VoidCallback onLanguage;
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<AuthState>(
      stream: Supabase.instance.client.auth.onAuthStateChange,
      builder: (context, snapshot) {
        final session = Supabase.instance.client.auth.currentSession;
        if (session == null) { return LoginPage(french: french, onLanguage: onLanguage); }
        return ActivationGate(
          key: ValueKey(session.user.id),
          french: french,
          onLanguage: onLanguage,
        );
      },
    );
  }
}

class ActivationGate extends StatefulWidget {
  const ActivationGate({super.key, required this.french, required this.onLanguage});
  final bool french;
  final VoidCallback onLanguage;
  @override
  State<ActivationGate> createState() => _ActivationGateState();
}

class _ActivationGateState extends State<ActivationGate> {
  late Future<bool> active;
  @override
  void initState() {
    super.initState();
    active = _checkActivation();
  }

  Future<bool> _checkActivation() async {
    final user = Supabase.instance.client.auth.currentUser;
    if (user == null) { return false; }
    final row = await Supabase.instance.client
        .from('profiles')
        .select('is_active')
        .eq('id', user.id)
        .maybeSingle();
    return row?['is_active'] == true;
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<bool>(
    future: active,
    builder: (context, snapshot) {
      if (snapshot.connectionState != ConnectionState.done) {
        return const Scaffold(body: Center(child: CircularProgressIndicator()));
      }
      if (snapshot.hasError || snapshot.data != true) {
        return Directionality(
          textDirection: widget.french ? TextDirection.ltr : TextDirection.rtl,
          child: Scaffold(
            appBar: AppBar(title: const Text('ZAKARY GRD'), actions: [
              TextButton(onPressed: widget.onLanguage, child: Text(widget.french ? 'العربية' : 'Français', style: const TextStyle(color: Colors.white))),
            ]),
            body: Center(child: Card(child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.lock_clock, size: 56, color: Colors.orange),
                const SizedBox(height: 12),
                Text(widget.french ? 'Compte en attente d’activation' : 'الحساب في انتظار تفعيل المسؤول',
                  style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
                const SizedBox(height: 10),
                Text(widget.french
                  ? 'Votre compte a été créé, mais l’administrateur doit l’activer avant l’accès aux données de l’usine.'
                  : 'تم إنشاء الحساب، لكن يجب على المسؤول تفعيله قبل الوصول إلى بيانات المصنع.',
                  textAlign: TextAlign.center),
                ...(snapshot.hasError
                    ? <Widget>[
                        Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: Text(
                            widget.french ? 'Vérifiez le schéma de la base de données.' : 'يرجى التحقق من إعداد جداول قاعدة البيانات.',
                            style: const TextStyle(color: Colors.red),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ]
                    : const <Widget>[]),
                const SizedBox(height: 16),
                OutlinedButton(
                  onPressed: () => Supabase.instance.client.auth.signOut(),
                  child: Text(widget.french ? 'Se déconnecter' : 'تسجيل الخروج'),
                ),
              ]),
            ))),
          ),
        );
      }
      return MainShell(french: widget.french, onLanguage: widget.onLanguage);
    },
  );
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.french, required this.onLanguage});
  final bool french;
  final VoidCallback onLanguage;
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final email = TextEditingController();
  final password = TextEditingController();
  bool busy = false;
  bool createAccount = false;
  String? error;

  Future<void> submit() async {
    setState(() { busy = true; error = null; });
    try {
      final auth = Supabase.instance.client.auth;
      if (createAccount) {
        await auth.signUp(email: email.text.trim(), password: password.text);
        if (mounted) {
          setState(() => error = widget.french
            ? 'Demande créée. Un administrateur doit activer le compte.'
            : 'تم إرسال طلب إنشاء الحساب. يجب على المسؤول تفعيل الحساب.');
        }
      } else {
        await auth.signInWithPassword(email: email.text.trim(), password: password.text);
      }
    } on AuthException catch (e) {
      if (mounted) { setState(() => error = e.message); }
    } catch (_) {
      if (mounted) { setState(() => error = widget.french ? 'Connexion impossible.' : 'تعذر الاتصال.'); }
    } finally {
      if (mounted) { setState(() => busy = false); }
    }
  }

  @override
  void dispose() { email.dispose(); password.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) => Directionality(
    textDirection: widget.french ? TextDirection.ltr : TextDirection.rtl,
    child: Scaffold(
      appBar: AppBar(title: const Text('ZAKARY GRD'), actions: [
        TextButton(onPressed: widget.onLanguage, child: Text(widget.french ? 'العربية' : 'Français', style: const TextStyle(color: Colors.white))),
      ]),
      body: Center(child: SingleChildScrollView(child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 440),
        child: Card(child: Padding(padding: const EdgeInsets.all(24), child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Icon(Icons.recycling, size: 68, color: Color(0xFF247A5A)),
          const SizedBox(height: 10),
          const Text('HENKEL ALGÉRIE – Mila', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 20),
          Text(widget.french ? 'Gestion et récupération des déchets' : 'تسيير واسترجاع النفايات', textAlign: TextAlign.center),
          const SizedBox(height: 20),
          TextField(controller: email, keyboardType: TextInputType.emailAddress, decoration: InputDecoration(labelText: widget.french ? 'E-mail' : 'البريد الإلكتروني', border: const OutlineInputBorder())),
          const SizedBox(height: 12),
          TextField(controller: password, obscureText: true, decoration: InputDecoration(labelText: widget.french ? 'Mot de passe' : 'كلمة المرور', border: const OutlineInputBorder())),
          if (error != null) Padding(padding: const EdgeInsets.only(top: 12), child: Text(error!, style: const TextStyle(color: Colors.red), textAlign: TextAlign.center)),
          const SizedBox(height: 16),
          SizedBox(width: double.infinity, child: FilledButton(
            onPressed: busy ? null : submit,
            child: busy ? const CircularProgressIndicator() : Text(createAccount ? (widget.french ? 'Créer une demande de compte' : 'إنشاء طلب حساب') : (widget.french ? 'Se connecter' : 'تسجيل الدخول')),
          )),
          TextButton(onPressed: () => setState(() { createAccount = !createAccount; error = null; }),
            child: Text(createAccount ? (widget.french ? 'J’ai déjà un compte' : 'لدي حساب بالفعل') : (widget.french ? 'Demander un compte' : 'طلب إنشاء حساب'))),
        ]))),
      ))),
    ),
  );
}

class MainShell extends StatefulWidget {
  const MainShell({super.key, required this.french, required this.onLanguage});
  final bool french;
  final VoidCallback onLanguage;
  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int selected = 0;
  List<String> get labels => widget.french
    ? ['Tableau de bord', 'Registre des déchets', 'Coûts et revenus', 'SEVIRA', 'Rapports']
    : ['لوحة التحكم', 'سجل النفايات', 'التكاليف والمداخيل', 'SEVIRA', 'التقارير'];
  List<IconData> get icons => [Icons.dashboard_outlined, Icons.recycling, Icons.account_balance_wallet_outlined, Icons.table_chart_outlined, Icons.assessment_outlined];

  @override
  Widget build(BuildContext context) {
    final pages = [
      DashboardPage(french: widget.french),
      InfoPage(title: labels[1], subtitle: widget.french ? 'Registre unifié des 11 catégories. Les unités seront définies par catégorie.' : 'السجل الموحد للأنواع الـ11. ستُحدد وحدة القياس حسب كل نوع.', icon: Icons.recycling),
      InfoPage(title: labels[2], subtitle: widget.french ? 'Calcul indépendant des coûts et revenus pour chaque catégorie.' : 'حساب مستقل للتكاليف والمداخيل لكل نوع نفايات.', icon: Icons.payments_outlined),
      InfoPage(title: 'SEVIRA', subtitle: widget.french ? 'Registre séparé, indépendant du registre principal.' : 'قسم مستقل عن سجل النفايات الرئيسي.', icon: Icons.table_chart_outlined),
      InfoPage(title: labels[4], subtitle: widget.french ? 'Rapports, export CSV et sauvegarde.' : 'التقارير وتصدير CSV والنسخ الاحتياطي.', icon: Icons.assessment_outlined),
    ];
    return Directionality(
      textDirection: widget.french ? TextDirection.ltr : TextDirection.rtl,
      child: LayoutBuilder(builder: (context, constraints) {
        final desktop = constraints.maxWidth >= 760;
        return Scaffold(
          appBar: AppBar(
            title: Text('ZAKARY GRD  •  ${labels[selected]}'),
            actions: [
              TextButton(onPressed: widget.onLanguage, child: Text(widget.french ? 'العربية' : 'Français', style: const TextStyle(color: Colors.white))),
              IconButton(tooltip: widget.french ? 'Déconnexion' : 'تسجيل الخروج', onPressed: () => Supabase.instance.client.auth.signOut(), icon: const Icon(Icons.logout)),
            ],
          ),
          body: Row(children: [
            if (desktop) NavigationRail(
              selectedIndex: selected,
              onDestinationSelected: (i) => setState(() => selected = i),
              labelType: NavigationRailLabelType.all,
              destinations: [for (var i=0;i<labels.length;i++) NavigationRailDestination(icon: Icon(icons[i]), label: Text(labels[i]))],
            ),
            Expanded(child: pages[selected]),
          ]),
          bottomNavigationBar: desktop ? null : NavigationBar(
            selectedIndex: selected,
            onDestinationSelected: (i) => setState(() => selected = i),
            destinations: [for (var i=0;i<labels.length;i++) NavigationDestination(icon: Icon(icons[i]), label: labels[i])],
          ),
        );
      }),
    );
  }
}

class DashboardPage extends StatelessWidget {
  const DashboardPage({super.key, required this.french});
  final bool french;
  @override
  Widget build(BuildContext context) {
    final items = french
      ? [('Déchets générés', '—'), ('Quantité récupérée', '—'), ('Taux de récupération', '—'), ('Nombre d’opérations', '—')]
      : [('إجمالي النفايات المنتجة', '—'), ('الكمية المسترجعة', '—'), ('نسبة الاسترجاع', '—'), ('عدد العمليات', '—')];
    return ListView(padding: const EdgeInsets.all(20), children: [
      Text(french ? 'HENKEL ALGÉRIE – Mila' : 'HENKEL ALGÉRIE – Mila', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold)),
      const SizedBox(height: 6),
      Text(french ? 'Vue d’ensemble de la gestion des déchets' : 'نظرة عامة على تسيير النفايات'),
      const SizedBox(height: 18),
      LayoutBuilder(builder: (context, c) {
        final count = c.maxWidth > 850 ? 4 : c.maxWidth > 520 ? 2 : 1;
        return GridView.count(shrinkWrap: true, physics: const NeverScrollableScrollPhysics(), crossAxisCount: count, crossAxisSpacing: 12, mainAxisSpacing: 12, childAspectRatio: 2.0,
          children: [for (final item in items) Card(child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
            Text(item.$1, style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            Text(item.$2, style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: const Color(0xFF247A5A), fontWeight: FontWeight.bold)),
          ])))],
        );
      }),
      const SizedBox(height: 12),
      Card(child: Padding(padding: const EdgeInsets.all(18), child: Text(french
        ? 'La connexion à la base partagée sera activée après vérification du schéma et des règles de sécurité Supabase.'
        : 'سيتم تفعيل البيانات المشتركة بعد مراجعة جداول Supabase وسياسات الحماية.'))),
    ]);
  }
}

class InfoPage extends StatelessWidget {
  const InfoPage({super.key, required this.title, required this.subtitle, required this.icon});
  final String title, subtitle;
  final IconData icon;
  @override
  Widget build(BuildContext context) => Center(child: ConstrainedBox(
    constraints: const BoxConstraints(maxWidth: 680),
    child: Card(child: Padding(padding: const EdgeInsets.all(28), child: Column(mainAxisSize: MainAxisSize.min, children: [
      Icon(icon, size: 58, color: const Color(0xFF247A5A)),
      const SizedBox(height: 16),
      Text(title, style: Theme.of(context).textTheme.headlineSmall, textAlign: TextAlign.center),
      const SizedBox(height: 12),
      Text(subtitle, textAlign: TextAlign.center),
      const SizedBox(height: 12),
      const Text('مرحلة البناء: ربط قاعدة البيانات والتحقق من الصلاحيات', style: TextStyle(color: Colors.blueGrey), textAlign: TextAlign.center),
    ]))),
  ));
}
