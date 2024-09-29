import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openinstall_flutter_global/openinstall_flutter_global.dart';

void main() {
  const MethodChannel channel = MethodChannel('openinstall_flutter_global');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });
}
