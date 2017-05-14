package com.stardust.scriptdroid;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.widget.Toast;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.http.ClientRequest;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.http;
import com.squareup.leakcanary.LeakCanary;
import com.stardust.app.SimpleActivityLifecycleCallbacks;
import com.stardust.app.VolumeChangeObserver;
import com.stardust.scriptdroid.autojs.AutoJs;
import com.stardust.scriptdroid.external.floating_window.OverlayPermissionChecker;
import com.stardust.scriptdroid.service.AccessibilityWatchDogService;
import com.stardust.scriptdroid.statics.ScriptStatics;
import com.stardust.scriptdroid.tool.CrashHandler;
import com.stardust.scriptdroid.tool.JsBeautifierFactory;
import com.stardust.scriptdroid.ui.error.ErrorReportActivity;
import com.stardust.theme.ThemeColor;
import com.stardust.theme.ThemeColorManager;
import com.stardust.util.ScreenMetrics;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.lang.ref.WeakReference;

/**
 * Created by Stardust on 2017/1/27.
 */

public class App extends MultiDexApplication {

    private static final String TAG = "App";

    private static WeakReference<App> instance;

    public static App getApp() {
        return instance.get();
    }

    private VolumeChangeObserver mVolumeChangeObserver = new VolumeChangeObserver();

    public void onCreate() {
        super.onCreate();
        instance = new WeakReference<>(this);
        setUpStaticsTool();
        setUpDebugEnvironment();
        init();
        registerActivityLifecycleCallback();
    }

    private void setUpStaticsTool() {
        ScriptStatics.init(this);
    }


    private void setUpDebugEnvironment() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
        if (!BuildConfig.DEBUG)
            Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(ErrorReportActivity.class));
    }

    private void init() {
        ThemeColorManager.setDefaultThemeColor(new ThemeColor(getResources().getColor(R.color.colorPrimary), getResources().getColor(R.color.colorPrimaryDark), getResources().getColor(R.color.colorAccent)));
        ThemeColorManager.init(this);
        AutoJs.initInstance(this);
        JsBeautifierFactory.initJsBeautify(this, "js/jsbeautify.js");
        initVolumeChangeObserver();
    }

    private void initVolumeChangeObserver() {
        registerReceiver(mVolumeChangeObserver, new IntentFilter(VolumeChangeObserver.ACTION_VOLUME_CHANGE));
        mVolumeChangeObserver.addOnVolumeChangeListener(new VolumeChangeObserver.OnVolumeChangeListener() {
            @Override
            public void onVolumeChange() {
                if (Pref.isRunningVolumeControlEnabled()) {
                    AutoJs.getInstance().getScriptEngineService().stopAllAndToast();
                }
            }
        });
    }

    private void registerActivityLifecycleCallback() {
        registerActivityLifecycleCallbacks(new SimpleActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                super.onActivityCreated(activity, savedInstanceState);
                AutoJs.getInstance().getAppUtils().setCurrentActivity(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                AutoJs.getInstance().getAppUtils().setCurrentActivity(null);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                ScreenMetrics.initIfNeeded(activity);
                AutoJs.getInstance().getAppUtils().setCurrentActivity(activity);
            }

        });
    }

    public static String getResString(int id) {
        return getApp().getString(id);
    }

    public VolumeChangeObserver getVolumeChangeObserver() {
        return mVolumeChangeObserver;
    }
}
