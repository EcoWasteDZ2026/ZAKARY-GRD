import 'package:flutter_test/flutter_test.dart';
import 'package:zakary_grd/main.dart';

void main() {
  testWidgets('shows setup screen when Supabase build settings are absent',
      (tester) async {
    await tester.pumpWidget(const ZakaryApp());
    expect(find.text('ZAKARY GRD'), findsOneWidget);
    expect(find.text('يلزم إعداد الاتصال بقاعدة البيانات'), findsOneWidget);
  });
}
