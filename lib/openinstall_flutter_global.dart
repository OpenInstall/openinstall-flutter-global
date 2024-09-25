import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

typedef Future EventHandler(Map<String, Object> data);

class OpeninstallFlutterPlugin {
  // 单例
  static final OpeninstallFlutterPlugin _instance = new OpeninstallFlutterPlugin._internal();

  factory OpeninstallFlutterPlugin() => _instance;

  OpeninstallFlutterPlugin._internal();

  Future defaultHandler() async {}

  late EventHandler _wakeupHandler;
  late EventHandler _installHandler;

  static const MethodChannel _channel = const MethodChannel('openinstall_flutter_global');

  void setDebug(bool enabled) {
    if (Platform.isAndroid) {
      var args = new Map();
      args["enabled"] = enabled;
      _channel.invokeMethod('setDebug', args);
    } else {
      // 仅适用于 Android 平台
    }
  }

  // 关闭剪切板读取
  void disableFetchClipData() {
    if (Platform.isAndroid) {
      _channel.invokeMethod('disableFetchClipData');
    } else {
      // 仅适用于 Android 平台
    }
  }

  // wakeupHandler 拉起回调.
  void init(EventHandler wakeupHandler) {
    if (Platform.isAndroid) {
      _wakeupHandler = wakeupHandler;
      _channel.setMethodCallHandler(_handleMethod);
      _channel.invokeMethod("registerWakeup"); // unused
      _channel.invokeMethod("init");
    }else{
      // 仅适用于 Android 平台
    }
  }

  // SDK内部将会一直保存安装数据，每次调用install方法都会返回值。
  // 如果调用install获取到数据并处理了自己的业务，后续不想再被触发，那么可以自己在业务调用成功时，设置一个标识，不再调用install方法
  void install(EventHandler installHandler, [int seconds = 10]) {
    if (Platform.isAndroid) {
      var args = new Map();
      args["seconds"] = seconds;
      this._installHandler = installHandler;
      _channel.invokeMethod('getInstall', args);
    }else{
      // 仅适用于 Android 平台
    }
  }


  void reportRegister() {
    if (Platform.isAndroid) {
      _channel.invokeMethod('reportRegister');
    }else{
      // 仅适用于 Android 平台
    }
  }

  void reportEffectPoint(String pointId, int pointValue, [Map<String, String>? extraMap]) {
    if (Platform.isAndroid) {
      var args = new Map();
      args["pointId"] = pointId;
      args["pointValue"] = pointValue;
      if (extraMap != null) {
        args["extras"] = extraMap;
      }
      _channel.invokeMethod('reportEffectPoint', args);
    }else{
      // 仅适用于 Android 平台
    }
  }

  Future<Map<Object?, Object?>> reportShare(String shareCode, String platform) async {
    if (Platform.isAndroid) {
      var args = new Map();
      args["shareCode"] = shareCode;
      args["platform"] = platform;
      Map<Object?, Object?> data = await _channel.invokeMethod('reportShare', args);
      return data;
    }else{
      // 仅适用于 Android 平台
      return new Map();
    }
  }

  Future _handleMethod(MethodCall call) async {
    print(call.method);
    switch (call.method) {
      case "onWakeupNotification":
        return _wakeupHandler(call.arguments.cast<String, Object>());
      case "onInstallNotification":
        return _installHandler(call.arguments.cast<String, Object>());
      default:
        throw new UnsupportedError("Unrecognized Event");
    }
  }
}
