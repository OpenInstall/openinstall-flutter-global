# openinstall global flutter plugin


openinstall_flutter_global 插件封装了openinstall 国际站原生SDK，集成了 **渠道统计,携带参数安装,快速安装与一键拉起** 功能。  


## 一、安装

### 1. 添加依赖
在项目的 `pubspec.yaml` 文件中添加以下内容:

``` json 
dependencies:
  openinstall_flutter_plugin: 
    git:
      url: https://github.com/OpenInstall/openinstall-flutter-global
```

### 2. 安装插件
使用命令行获取

``` shell
$ flutter pub get
```

或者使用开发工具的 `flutter pub get`

### 3. 导入
在 `Dart` 代码中使用以下代码导入:

``` dart
import 'package:openinstall_flutter_plugin/openinstall_flutter_global.dart';
```

## 二、配置

前往 [openinstall控制台](https://developer.openinstallglobal.com/) 创建应用并获取 openinstall 为应用分配的` appkey` 和 `scheme` 以及 iOS的关联域名（Associated Domains）  

### Android 平台配置

#### 配置 appkey
在 `/android/app/build.gradle` 中添加代码设置appkey：
``` groovy
android: {
  ....
  defaultConfig {
    ...
    manifestPlaceholders += [
        OPENINSTALL_APPKEY : "openinstallglobal平台为应用分配的appkey",
    ]
  }    
}
```

#### 配置 scheme
修改 `/android/app/src/main/AndroidMenifest.xml` 文件，在跳转 `Activity` 标签内添加 `intent-filter`
``` xml
<intent-filter>
    <action android:name="android.intent.action.VIEW"/>

    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>

    <data android:scheme="openinstallglobal平台为应用分配的scheme"/>
</intent-filter>
```

### iOS 平台配置


## 三、使用

### 初始化
`init(EventHandler wakeupHandler)`

初始化时，需要传入**拉起回调** 获取 web 端传过来的动态参数

示例：
``` dart
Future wakeupHandler(Map<String, Object> data) async {
    setState(() {
        debugLog = "wakeup result : channel=" +
            data['channelCode'] +
            ", data=" +
            data['bindData'];
    });
}

_openinstallFlutterPlugin.init(wakeupHandler);
```
### 获取安装参数
`install(EventHandler installHandler, [int seconds = 10])`

在 APP 需要安装参数时（由 web 网页中传递过来的，如邀请码、游戏房间号等动态参数），调用此接口，在回调中获取参数

示例：
``` dart

Future installHandler(Map<String, Object> data) async {
    setState(() {
        debugLog = "install result : channel=" +
            data['channelCode'] +
            ", data=" +
            data['bindData'] +
            ", shouldRetry" +
            data['shouldRetry'];
    });
}

_openinstallFlutterPlugin.install(installHandler);

```
#### 注册统计
`reportRegister()`

如需统计每个渠道的注册量（对评估渠道质量很重要），可根据自身的业务规则，在确保用户完成 APP 注册的情况下调用此接口

示例：
``` dart
_openinstallFlutterPlugin.reportRegister();
```
#### 效果点统计
`reportEffectPoint(String pointId, int pointValue)`  

效果点建立在渠道基础之上，主要用来统计终端用户对某些特殊业务的使用效果。调用此接口时，请使用后台创建的 “效果点ID” 作为 pointId 

示例：
``` dart
_openinstallFlutterPlugin.reportEffectPoint("effect_test", 1);
```

#### 效果点明细统计
`reportEffectPoint(String pointId, int pointValue, Map<String, String> extraMap)`  

效果点建立在渠道基础之上，主要用来统计终端用户对某些特殊业务的使用效果。调用此接口时，请使用后台创建的 “效果点ID” 作为 pointId  

示例：
``` dart
Map<String, String> extraMap = {
    "key1": "value1",
    "key2": "value2"
};
_openinstallFlutterPlugin.reportEffectPoint("effect_detail", 1, extraMap);
```

备注：效果点明细统计需要原生iOS SDK >=2.6.0，请从CocoaPods拉取、更新、确认版本。

#### 裂变分享上报
`reportShare(String shareCode, String platform)`  

分享上报主要是统计某个具体用户在某次分享中，分享给了哪个平台，再通过JS端绑定被分享的用户信息，进一步统计到被分享用户的激活回流等情况

示例：
``` dart
_openinstallFlutterPlugin.reportShare("123456", "WechatSession")
    .then((data) => print("reportShare : " + data.toString()));
```
可以通过返回的data中的`shouldRetry`决定是否需要重试，以及`message`查看失败的原因


---
