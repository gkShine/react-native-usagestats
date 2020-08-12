package com.usagestats.reactnative;

import android.app.AppOpsManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

public class UsageStatsModule extends ReactContextBaseJavaModule {

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";
    private Map<String, UsageStats> aggregateStats;

    UsageStatsModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "UsageStats";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        // TODO: Add any necessary constants to the module here
        constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
        constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
        return constants;
    }

    private static List getDates(int durationInDays) {
      return getDateRangeFromNow(Calendar.DATE, -(durationInDays));
    }

    private static List getDateRangeFromNow(int field, int amount) {
        List dates = new ArrayList();
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(field, amount);
        long startTime = calendar.getTimeInMillis();

        dates.add(startTime);
        dates.add(endTime);

        return dates;
    }

    private static Map<String, UsageStats> getAggregateStatsMap(Context context, int durationInDays) {
        UsageStatsManager usm = getUsageStatsManager(context);
        List dates = getDates(durationInDays);
        long startTime = (long) dates.get(0);
        long endTime = (long) dates.get(1);

      return usm.queryAndAggregateUsageStats(startTime, endTime);
    }

    @SuppressWarnings("ResourceType")
    private static UsageStatsManager getUsageStatsManager(Context context) {
      return (UsageStatsManager) context.getSystemService("usagestats");
    }

    private static JSONArray getStatsJSON(Map<String, UsageStats> aggregateStats, List excludes) throws JSONException {
        JSONArray statsCollection = new JSONArray();
        for (Map.Entry<String, UsageStats> entry : aggregateStats.entrySet()) {
            String packageName = entry.getValue().getPackageName();
            if (excludes.contains("system") && (packageName.indexOf("com.android") == 0 || packageName == "android" )) {
                continue;
            }
            if (excludes.contains("google") && packageName.indexOf("com.google.android") == 0) {
                continue;
            }
            JSONObject stats = new JSONObject();
            stats.put("package", packageName).put("time", entry.getValue().getTotalTimeInForeground());
            statsCollection.put(stats);
        }
        return statsCollection;
    }

    @ReactMethod
    public void getStats(int durationInDays, final ReadableArray excludes, final Promise promise) {
        if (durationInDays > 0) {
            try {
                ArrayList<String> list = new ArrayList<>();
                if (excludes != null && excludes.size() != 0) {
                    for (int i = 0; i < excludes.size(); i++) {
                        String exclude = excludes.getString(i);
                        list.add(exclude);
                    }
                }
                JSONArray stats = getStatsJSON(getAggregateStatsMap(getReactApplicationContext(), durationInDays), list);

                // List dates = getDates(durationInDays);
                promise.resolve(stats.toString());
            } catch (Exception e) {
                promise.reject(e);
            }
        } else {
            promise.reject("403", "Enter an integer greater than 0!");
        }
    }

    @ReactMethod
    public void getAppStats(String packageName, int durationInDays, final Promise promise) {
        try {
            Map<String, UsageStats> aggregateStats = getAggregateStatsMap(getReactApplicationContext(), durationInDays);
            for (Map.Entry<String, UsageStats> entry : aggregateStats.entrySet()) {
                String _packageName = entry.getValue().getPackageName();
                if (_packageName.equals(packageName)) {
                    JSONObject stats = new JSONObject();
                    stats.put("package", packageName).put("time", entry.getValue().getTotalTimeInForeground());
                    promise.resolve(stats.toString());
                    break;
                }
            }
            promise.reject("404", "Not Found");
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void checkPermission(final Promise promise) {
        try {
            boolean granted = false;
            ReactApplicationContext context = getReactApplicationContext();
            AppOpsManager appOps = (AppOpsManager) context
                    .getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName());

            if (mode == AppOpsManager.MODE_DEFAULT) {
                granted = (context.checkCallingOrSelfPermission(
                        android.Manifest.permission.PACKAGE_USAGE_STATS)
                        == PackageManager.PERMISSION_GRANTED);
            } else {
                granted = (mode == AppOpsManager.MODE_ALLOWED);
            }
            promise.resolve(granted);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void requestPermission() {
      ReactApplicationContext context = getReactApplicationContext();
      Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    }
}
