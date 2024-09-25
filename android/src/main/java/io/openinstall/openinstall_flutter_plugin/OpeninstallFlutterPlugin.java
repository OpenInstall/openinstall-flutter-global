package io.openinstall.openinstall_flutter_plugin;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.openinstall.api.OpData;
import io.openinstall.api.OpError;
import io.openinstall.api.OpenInstall;
import io.openinstall.api.ResultCallBack;

/**
 * OpeninstallFlutterPlugin
 */
public class OpeninstallFlutterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.NewIntentListener {

    private static final String TAG = "flutter_global";

    private static final String METHOD_WAKEUP_NOTIFICATION = "onWakeupNotification";
    private static final String METHOD_INSTALL_NOTIFICATION = "onInstallNotification";

    private MethodChannel channel = null;
    private ActivityPluginBinding activityPluginBinding;
    private FlutterPluginBinding flutterPluginBinding;
    private Intent intentHolder = null;
    private volatile boolean initialized = false;

    private boolean debuggable = true;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        flutterPluginBinding = binding;
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "openinstall_flutter_global");
        channel.setMethodCallHandler(this);
        OpenInstall.initialize(flutterPluginBinding.getApplicationContext());
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
        activityPluginBinding.addOnNewIntentListener(this);
        wakeup(activityPluginBinding.getActivity().getIntent());
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activityPluginBinding = binding;
        activityPluginBinding.addOnNewIntentListener(this);
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull final Result result) {
        debugLog("invoke " + call.method);
        if ("setDebug".equalsIgnoreCase(call.method)) {
            Boolean enabled = call.argument("enabled");
            debuggable = enabled == null || enabled;
            OpenInstall.setDebug(debuggable);
            result.success("OK");
        } else if ("disableFetchClipData".equalsIgnoreCase(call.method)) {
            OpenInstall.getInstance().disableFetchClipData();
            result.success("OK");
        } else if ("init".equalsIgnoreCase(call.method)) {
            init();
            result.success("OK");
        } else if ("registerWakeup".equalsIgnoreCase(call.method)) {
            // iOS 使用此接口初始化，继续保留
            result.success("OK");
        } else if ("getInstall".equalsIgnoreCase(call.method)) {
            Integer seconds = call.argument("seconds");
            OpenInstall.getInstance().getInstallParam(seconds == null ? 0 : seconds,
                    new ResultCallBack<OpData>() {
                        @Override
                        public void onResult(OpData opData) {
                            channel.invokeMethod(METHOD_INSTALL_NOTIFICATION, data2Map(opData));
                        }

                        @Override
                        public void onError(OpError opError) {
                            channel.invokeMethod(METHOD_INSTALL_NOTIFICATION, error2Map(opError));
                        }
                    });
            result.success("OK");
        } else if ("reportRegister".equalsIgnoreCase(call.method)) {
            OpenInstall.getInstance().register();
            result.success("OK");
        } else if ("reportEffectPoint".equalsIgnoreCase(call.method)) {
            String pointId = call.argument("pointId");
            Integer pointValue = call.argument("pointValue");
            if (TextUtils.isEmpty(pointId) || pointValue == null) {
                Log.w(TAG, "pointId is empty or pointValue is null");
            } else {
                Map<String, String> extraMap = call.argument("extras");
                OpenInstall.getInstance().saveEvent(pointId, pointValue, extraMap);
            }
            result.success("OK");
        } else if ("reportShare".equalsIgnoreCase(call.method)) {
            String shareCode = call.argument("shareCode");
            String sharePlatform = call.argument("platform");
            final Map<String, Object> data = new HashMap<>();
            if (TextUtils.isEmpty(shareCode) || TextUtils.isEmpty(sharePlatform)) {
                data.put("message", "shareCode or platform is empty");
                data.put("shouldRetry", false);
                result.success(data);
            } else {
                OpenInstall.getInstance().reportShare(shareCode, sharePlatform, new ResultCallBack<Boolean>() {
                    @Override
                    public void onResult(Boolean unused) {
                        data.put("shouldRetry", false);
                        result.success(data);
                    }

                    @Override
                    public void onError(OpError opError) {
                        result.success(error2Map(opError));
                    }
                });
            }
        } else {
            result.notImplemented();
        }
    }

    private void init() {
        Activity activity = activityPluginBinding.getActivity();
        OpenInstall.getInstance().start(activity);
        initialized = true;
        if (intentHolder != null) {
            wakeup(intentHolder);
            intentHolder = null;
        }
    }

    @Override
    public boolean onNewIntent(Intent intent) {
        debugLog("onNewIntent");
        wakeup(intent);
        return false;
    }


    private void wakeup(Intent intent) {
        if (initialized) {
            OpenInstall.getInstance().handleDeepLink(intent, new ResultCallBack<OpData>() {
                @Override
                public void onResult(OpData opData) {
                    channel.invokeMethod(METHOD_WAKEUP_NOTIFICATION, data2Map(opData));
                }

                @Override
                public void onError(OpError opError) {
                    channel.invokeMethod(METHOD_WAKEUP_NOTIFICATION, error2Map(opError));
                }
            });
        } else {
            intentHolder = intent;
        }
    }

    private static Map<String, Object> data2Map(OpData data) {
        Map<String, Object> result = new HashMap<>();
        result.put("shouldRetry", false);
        if (data != null) {
            result.put("channelCode", data.getChannelCode());
            result.put("bindData", data.getBindData());
        }
        return result;
    }

    private static Map<String, Object> error2Map(OpError error) {
        Map<String, Object> result = new HashMap<>();
        result.put("shouldRetry", error.shouldRetry());
        result.put("message", error.getErrorMsg());
        return result;
    }

    private void debugLog(String message) {
        if (debuggable) {
            Log.d(TAG, message);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onDetachedFromActivity() {
        activityPluginBinding.removeOnNewIntentListener(this);
        activityPluginBinding = null;
    }
}
