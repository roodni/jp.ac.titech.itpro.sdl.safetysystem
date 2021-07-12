package jp.ac.titech.itpro.sdl.safetysystem;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class KeyguardCheckAlarmReceiver extends BroadcastReceiver {
    private final static String TAG = "KeyguardCheckAlarmR";
    private final static int REQUEST_CODE = 1;

    public static void setAlarmLoop(Context context, long triggerAtMillis) {
        Intent intent = new Intent(context, KeyguardCheckAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAtMillis, sender);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE);
        Boolean is_safety_notify_active = sharedPref.getBoolean(context.getString(R.string.pref_key_is_safety_notify_active), false);
        if (is_safety_notify_active) {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardLocked()) {
                // ロックされている
                Log.d(TAG, "locked. try again");
                setAlarmLoop(context, SystemClock.elapsedRealtime() + 3000);    // 3 sec
            } else {
                // ロックが解除されている
                Log.d(TAG, "unlocked. save current time");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(context.getString(R.string.pref_key_latest_user_present_time), System.currentTimeMillis());
                editor.apply();
                setAlarmLoop(context, SystemClock.elapsedRealtime() + 60 * 1000); // 1 min
            }
        } else {
            Log.d(TAG, "safety notify is no longer active");
        }
    }
}