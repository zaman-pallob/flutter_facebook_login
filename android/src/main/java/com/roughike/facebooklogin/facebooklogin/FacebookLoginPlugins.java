package android.src.main.java.com.roughike.facebooklogin.facebooklogin;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;

import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import android.os.Bundle;

import androidx.annotation.NonNull;

import io.flutter.embedding.android.FlutterActivity;



public class FacebookLoginPlugins extends FlutterActivity {


    private static final String CHANNEL_NAME = "com.roughike/flutter_facebook_login";

    private static final String ERROR_UNKNOWN_LOGIN_BEHAVIOR = "unknown_login_behavior";

    private static final String METHOD_LOG_IN = "logIn";
    private static final String METHOD_LOG_OUT = "logOut";
    private static final String METHOD_GET_CURRENT_ACCESS_TOKEN = "getCurrentAccessToken";

    private static final String ARG_LOGIN_BEHAVIOR = "behavior";
    private static final String ARG_PERMISSIONS = "permissions";

    private static final String LOGIN_BEHAVIOR_NATIVE_WITH_FALLBACK = "nativeWithFallback";
    private static final String LOGIN_BEHAVIOR_NATIVE_ONLY = "nativeOnly";
    private static final String LOGIN_BEHAVIOR_WEB_ONLY = "webOnly";
    private static final String LOGIN_BEHAVIOR_WEB_VIEW_ONLY = "webViewOnly";

    private final FacebookSignInDelegate delegate;


    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            String loginBehaviorStr;
                            LoginBehavior loginBehavior;

                            switch (call.method) {
                                case METHOD_LOG_IN:
                                    loginBehaviorStr = call.argument(ARG_LOGIN_BEHAVIOR);
                                    loginBehavior = loginBehaviorFromString(loginBehaviorStr, result);
                                    List<String> permissions = call.argument(ARG_PERMISSIONS);

                                    delegate.logIn(loginBehavior, permissions, result);
                                    break;
                                case METHOD_LOG_OUT:
                                    delegate.logOut(result);
                                    break;
                                case METHOD_GET_CURRENT_ACCESS_TOKEN:
                                    delegate.getCurrentAccessToken(result);
                                    break;
                                default:
                                    result.notImplemented();
                                    break;
                            }
                        }
                );

    }

    private FacebookLoginPlugin(Registrar registrar) {
        delegate = new FacebookSignInDelegate(registrar);
    }

    public static void registerWith(Registrar registrar) {
        final FacebookLoginPlugin plugin = new FacebookLoginPlugin(registrar);
        final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
        channel.setMethodCallHandler(plugin);
    }



    private LoginBehavior loginBehaviorFromString(String loginBehavior, Result result) {
        switch (loginBehavior) {
            case LOGIN_BEHAVIOR_NATIVE_WITH_FALLBACK:
                return LoginBehavior.NATIVE_WITH_FALLBACK;
            case LOGIN_BEHAVIOR_NATIVE_ONLY:
                return LoginBehavior.NATIVE_ONLY;
            case LOGIN_BEHAVIOR_WEB_ONLY:
                return LoginBehavior.WEB_ONLY;
            case LOGIN_BEHAVIOR_WEB_VIEW_ONLY:
                return LoginBehavior.WEB_VIEW_ONLY;
            default:
                result.error(
                        ERROR_UNKNOWN_LOGIN_BEHAVIOR,
                        "setLoginBehavior called with unknown login behavior: "
                                + loginBehavior,
                        null
                );
                return null;
        }
    }

    public static final class FacebookSignInDelegate {
        private final Registrar registrar;
        private final CallbackManager callbackManager;
        private final LoginManager loginManager;
        private final FacebookLoginResultDelegate resultDelegate;

        public FacebookSignInDelegate(Registrar registrar) {
            this.registrar = registrar;
            this.callbackManager = CallbackManager.Factory.create();
            this.loginManager = LoginManager.getInstance();
            this.resultDelegate = new FacebookLoginResultDelegate(callbackManager);

            loginManager.registerCallback(callbackManager, resultDelegate);
            registrar.addActivityResultListener(resultDelegate);
        }

        public void logIn(
                LoginBehavior loginBehavior, List<String> permissions, Result result) {
            resultDelegate.setPendingResult(METHOD_LOG_IN, result);

            loginManager.setLoginBehavior(loginBehavior);
            loginManager.logIn(registrar.activity(), permissions);
        }

        public void logOut(Result result) {
            loginManager.logOut();
            result.success(null);
        }

        public void getCurrentAccessToken(Result result) {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            Map<String, Object> tokenMap = FacebookLoginResults.accessToken(accessToken);

            result.success(tokenMap);
        }
    }
}

