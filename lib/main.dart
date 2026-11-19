import 'package:flutter/material.dart';
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
      WasteRecordsPage(french: widget.french),
      FinanceRecordsPage(french: widget.french),
      SeviraRecordsPage(french: widget.french),
      ReportsRecordsPage(french: widget.french),
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


class WasteRecordsPage extends StatefulWidget {
  const WasteRecordsPage({super.key, required this.french});
  final bool french;
  @override State<WasteRecordsPage> createState()=>_WasteRecordsPageState();
}
class _WasteRecordsPageState extends State<WasteRecordsPage> {
  Future<List<Map<String,dynamic>>> load() async => List<Map<String,dynamic>>.from(await Supabase.instance.client.from('waste_records').select('*,waste_types(name)').order('operation_date',ascending:false));
  Future<void> add() async {
    final db=Supabase.instance.client;
    final types=List<Map<String,dynamic>>.from(await db.from('waste_types').select('id,name').eq('is_active',true).order('name'));
    if(!mounted)return;
    final qty=TextEditingController(), recovered=TextEditingController(text:'0'), dept=TextEditingController(), dest=TextEditingController(), method=TextEditingController(), cost=TextEditingController(text:'0'), income=TextEditingController(text:'0'), notes=TextEditingController();
    String? type=types.isEmpty?null:types.first['id'].toString(); String unit='kg'; DateTime date=DateTime.now();
    final ok=await showDialog<bool>(context:context,builder:(ctx)=>StatefulBuilder(builder:(ctx,setD)=>AlertDialog(
      title:Text(widget.french?'Nouvel enregistrement':'تسجيل نفايات جديد'),
      content:SizedBox(width:460,child:SingleChildScrollView(child:Column(mainAxisSize:MainAxisSize.min,children:[
        DropdownButtonFormField<String>(value:type,decoration:InputDecoration(labelText:widget.french?'Type de déchet':'نوع النفايات'),items:types.map((t)=>DropdownMenuItem(value:t['id'].toString(),child:Text(t['name'].toString(),overflow:TextOverflow.ellipsis))).toList(),onChanged:(v)=>setD(()=>type=v)),
        TextButton(onPressed:()async{final d=await showDatePicker(context:ctx,initialDate:date,firstDate:DateTime(2020),lastDate:DateTime(2100));if(d!=null)setD(()=>date=d);},child:Text(date.toIso8601String().substring(0,10))),
        _field(qty,widget.french?'Quantité':'الكمية',true),
        DropdownButtonFormField<String>(value:unit,decoration:InputDecoration(labelText:widget.french?'Unité':'الوحدة'),items:['kg','tonne','litre','unit'].map((u)=>DropdownMenuItem(value:u,child:Text(u))).toList(),onChanged:(v)=>setD(()=>unit=v??'kg')),
        _field(recovered,widget.french?'Quantité récupérée':'الكمية المسترجعة',true),
        _field(dept,widget.french?'Département':'القسم المنتج'),
        _field(dest,widget.french?'Destination / prestataire':'الوجهة / المتعامل'),
        _field(method,widget.french?'Traitement / valorisation':'المعالجة / التثمين'),
        _field(cost,widget.french?'Coût DZD':'التكلفة دج',true),_field(income,widget.french?'Revenu DZD':'المداخيل دج',true),_field(notes,widget.french?'Notes':'ملاحظات')
      ]))),
      actions:[TextButton(onPressed:()=>Navigator.pop(ctx,false),child:Text(widget.french?'Annuler':'إلغاء')),FilledButton(onPressed:()=>Navigator.pop(ctx,true),child:Text(widget.french?'Enregistrer':'حفظ'))]
    )));
    if(ok==true){
      final q=double.tryParse(qty.text.replaceAll(',','.'))??0, r=double.tryParse(recovered.text.replaceAll(',','.'))??0;
      if(type==null||q<=0||r<0||r>q){if(mounted)ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(widget.french?'Quantités invalides':'الكمية غير صحيحة')));}
      else {try{await db.from('waste_records').insert({'operation_date':date.toIso8601String().substring(0,10),'waste_type_id':type,'quantity':q,'recovered_quantity':r,'unit':unit,'department':dept.text,'destination':dest.text,'treatment_method':method.text,'cost_dzd':double.tryParse(cost.text)??0,'income_dzd':double.tryParse(income.text)??0,'notes':notes.text,'created_by':db.auth.currentUser!.id});if(mounted)setState((){});}catch(e){if(mounted)ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(e.toString())));}}
    }
    for(final x in [qty,recovered,dept,dest,method,cost,income,notes]){x.dispose();}
  }
  @override Widget build(BuildContext context)=>Scaffold(
    floatingActionButton:FloatingActionButton.extended(onPressed:add,icon:const Icon(Icons.add),label:Text(widget.french?'Ajouter':'إضافة سجل')),
    body:FutureBuilder<List<Map<String,dynamic>>>(future:load(),builder:(ctx,s){
      if(s.hasError)return Center(child:Text(s.error.toString()));
      if(!s.hasData)return const Center(child:CircularProgressIndicator());
      if(s.data!.isEmpty)return Center(child:Text(widget.french?'Aucun enregistrement':'لا توجد سجلات'));
      return ListView.builder(padding:const EdgeInsets.all(12),itemCount:s.data!.length,itemBuilder:(ctx,i){
        final r=s.data![i]; final n=(r['waste_types'] as Map?)?['name']?.toString()??'';
        return Card(child:ListTile(title:Text(n,style:const TextStyle(fontWeight:FontWeight.bold)),subtitle:Text(r['operation_date'].toString()+' • '+r['quantity'].toString()+' '+r['unit'].toString()+' • '+(widget.french?'Récupéré: ':'مسترجع: ')+r['recovered_quantity'].toString()+'\\n'+(r['department']??'')+' → '+(r['destination']??'')),isThreeLine:true,trailing:IconButton(icon:const Icon(Icons.delete_outline),onPressed:()async{try{await Supabase.instance.client.from('waste_records').delete().eq('id',r['id']);if(mounted)setState((){});}catch(e){if(mounted)ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(e.toString())));}})));
      });
    }));
}
Widget _field(TextEditingController c,String label,[bool number=false])=>Padding(padding:const EdgeInsets.only(bottom:9),child:TextField(controller:c,keyboardType:number?const TextInputType.numberWithOptions(decimal:true):TextInputType.text,decoration:InputDecoration(labelText:label,border:const OutlineInputBorder(),isDense:true)));

class SeviraRecordsPage extends StatefulWidget {
  const SeviraRecordsPage({super.key,required this.french}); final bool french;
  @override State<SeviraRecordsPage> createState()=>_SeviraRecordsPageState();
}
class _SeviraRecordsPageState extends State<SeviraRecordsPage>{
  Future<List<Map<String,dynamic>>> load()async=>List<Map<String,dynamic>>.from(await Supabase.instance.client.from('sevira_records').select().order('created_at',ascending:false));
  Future<void> add()async{
    final t=TextEditingController(),q=TextEditingController(),u=TextEditingController(text:'kg'),h=TextEditingController(),d=TextEditingController(),n=TextEditingController();
    final ok=await showDialog<bool>(context:context,builder:(ctx)=>AlertDialog(title:const Text('SEVIRA'),content:SizedBox(width:420,child:SingleChildScrollView(child:Column(mainAxisSize:MainAxisSize.min,children:[_field(t,widget.french?'Type de déchet':'نوع النفايات'),_field(q,widget.french?'Quantité':'الكمية',true),_field(u,widget.french?'Unité':'الوحدة'),_field(h,widget.french?'Mode de gestion':'طريقة التسيير'),_field(d,widget.french?'Destination':'الوجهة'),_field(n,widget.french?'Notes':'ملاحظات')])),actions:[TextButton(onPressed:()=>Navigator.pop(ctx,false),child:Text(widget.french?'Annuler':'إلغاء')),FilledButton(onPressed:()=>Navigator.pop(ctx,true),child:Text(widget.french?'Enregistrer':'حفظ'))]));
    if(ok==true){try{await Supabase.instance.client.from('sevira_records').insert({'waste_type':t.text,'quantity':double.tryParse(q.text.replaceAll(',','.'))??0,'unit':u.text,'handling_method':h.text,'destination':d.text,'notes':n.text,'created_by':Supabase.instance.client.auth.currentUser!.id});if(mounted)setState((){});}catch(e){if(mounted)ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(e.toString())));}}
    for(final x in [t,q,u,h,d,n]){x.dispose();}
  }
  @override Widget build(BuildContext context)=>Scaffold(floatingActionButton:FloatingActionButton.extended(onPressed:add,icon:const Icon(Icons.add),label:Text(widget.french?'Ajouter':'إضافة')),
    body:FutureBuilder<List<Map<String,dynamic>>>(future:load(),builder:(ctx,s){if(s.hasError)return Center(child:Text(s.error.toString()));if(!s.hasData)return const Center(child:CircularProgressIndicator());if(s.data!.isEmpty)return Center(child:Text(widget.french?'Registre SEVIRA vide':'سجل SEVIRA فارغ'));return ListView(children:[for(final r in s.data!)Card(child:ListTile(title:Text(r['waste_type'].toString()),subtitle:Text(r['quantity'].toString()+' '+r['unit'].toString()+' • '+r['handling_method'].toString()+'\\n'+r['destination'].toString()),isThreeLine:true))]);}));
}

class FinanceRecordsPage extends StatefulWidget {const FinanceRecordsPage({super.key,required this.french});final bool french;@override State<FinanceRecordsPage> createState()=>_FinanceRecordsPageState();}
class _FinanceRecordsPageState extends State<FinanceRecordsPage>{
 @override Widget build(BuildContext context)=>FutureBuilder<List<Map<String,dynamic>>>(future:Supabase.instance.client.from('waste_records').select('cost_dzd,income_dzd,waste_types(name)'),builder:(ctx,s){
  if(s.hasError)return Center(child:Text(s.error.toString()));if(!s.hasData)return const Center(child:CircularProgressIndicator());
  final totals=<String,List<double>>{};for(final r in s.data!){final name=(r['waste_types'] as Map?)?['name']?.toString()??'—';final v=totals.putIfAbsent(name,()=>[0,0]);v[0]+=(double.tryParse(r['cost_dzd'].toString())??0);v[1]+=(double.tryParse(r['income_dzd'].toString())??0);}
  return ListView(padding:const EdgeInsets.all(12),children:[Text(widget.french?'Coûts et revenus par catégorie':'التكاليف والمداخيل حسب كل نوع',style:Theme.of(context).textTheme.titleLarge),for(final e in totals.entries)Card(child:ListTile(title:Text(e.key),subtitle:Text((widget.french?'Coûts: ':'التكاليف: ')+e.value[0].toStringAsFixed(2)+' DZD\\n'+(widget.french?'Revenus: ':'المداخيل: ')+e.value[1].toStringAsFixed(2)+' DZD'),isThreeLine:true,trailing:Text((e.value[1]-e.value[0]).toStringAsFixed(2)+'\\nDZD')))]);
 });
}
class ReportsRecordsPage extends StatefulWidget {const ReportsRecordsPage({super.key,required this.french});final bool french;@override State<ReportsRecordsPage> createState()=>_ReportsRecordsPageState();}
class _ReportsRecordsPageState extends State<ReportsRecordsPage>{
 @override Widget build(BuildContext context)=>FutureBuilder<List<Map<String,dynamic>>>(future:Supabase.instance.client.from('waste_records').select('quantity,recovered_quantity,cost_dzd,income_dzd,waste_types(name)'),builder:(ctx,s){
  if(s.hasError)return Center(child:Text(s.error.toString()));if(!s.hasData)return const Center(child:CircularProgressIndicator());final rows=s.data!;
  double sum(String k)=>rows.fold(0.0,(a,r)=>a+(double.tryParse(r[k].toString())??0));
  final q=sum('quantity'),rec=sum('recovered_quantity');
  return ListView(padding:const EdgeInsets.all(16),children:[
   Text(widget.french?'Rapport général':'التقرير العام',style:Theme.of(context).textTheme.headlineSmall),
   _reportTile(widget.french?'Nombre d’opérations':'عدد العمليات',rows.length.toString()),
   _reportTile(widget.french?'Quantité totale':'إجمالي الكمية',q.toStringAsFixed(2)),
   _reportTile(widget.french?'Quantité récupérée':'الكمية المسترجعة',rec.toStringAsFixed(2)),
   _reportTile(widget.french?'Taux de récupération':'نسبة الاسترجاع',(q==0?0:rec/q*100).toStringAsFixed(1)+'%'),
   _reportTile(widget.french?'Coûts totaux':'إجمالي التكاليف',sum('cost_dzd').toStringAsFixed(2)+' DZD'),
   _reportTile(widget.french?'Revenus totaux':'إجمالي المداخيل',sum('income_dzd').toStringAsFixed(2)+' DZD'),
  ]);
 });
}
Widget _reportTile(String a,String b)=>Card(child:ListTile(title:Text(a),trailing:Text(b,style:const TextStyle(fontWeight:FontWeight.bold))));

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
