package jp.ac.titech.itpro.sdl.safetysystem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private final static String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE);
            Boolean isSafetyNotifyActive = sharedPref.getBoolean(context.getString(R.string.pref_key_is_safety_notify_active), false);
            if (isSafetyNotifyActive) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong(context.getString(R.string.pref_key_latest_user_present_time), System.currentTimeMillis());
                editor.apply();
                KeyguardCheckAlarmReceiver.setAlarmLoop(context, 0);
                SafetyNotifyAlarmReceiver.setAlarmLoop(context, 0);
            }
        }
    }
}