package jp.ac.titech.itpro.sdl.safetysystem;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SafetyNotifyAlarmReceiver extends BroadcastReceiver {
    private final static String TAG = "SafetyNotifyAlarmR";
    private final static int REQUEST_CODE = 2;
    private final static String CHANNEL_ID = "safety_notify";
    private final static int NOTIFICATION_ID_NORMAL = 1;
    private final static int NOTIFICATION_ID_FAILED = 2;

    private final static long TIME_LIMIT = 24 * 60 * 60 * 1000;  // 24 h

    public static void setAlarmLoop(Context context, long nextMillis) {
        Log.d(TAG, "setAlarmLoop");
        Intent intent = new Intent(context, SafetyNotifyAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + nextMillis, sender);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        Log.d(TAG, "onReceive");
        SharedPreferences sharedPref = c.getSharedPreferences(c.getString(R.string.pref_name), Context.MODE_PRIVATE);

        Boolean isSafetyNotifyActive = sharedPref.getBoolean(c.getString(R.string.pref_key_is_safety_notify_active), false);
        if (isSafetyNotifyActive) {
            DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
            Long currentTime = System.currentTimeMillis();
            Long latestTime = sharedPref.getLong(c.getString(R.string.pref_key_latest_user_present_time), 0);
            String latestTimeText = df.format(new Date(latestTime));

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(c.getString(R.string.pref_key_latest_notify_time), currentTime);
            editor.apply();

            // 通知の準備
            NotificationManager notificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                CharSequence name = c.getString(R.string.notify_channel_name);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                notificationManager.createNotificationChannel(channel);
            }
            int notifyMessage = R.string.notify_message_default;

            // Line Notify
            Boolean isAccessTokenRegistered = sharedPref.getBoolean(c.getString(R.string.pref_key_is_access_token_registered), false);
            if (isAccessTokenRegistered) {
                if (currentTime - latestTime > TIME_LIMIT) {
                    String accessToken = sharedPref.getString(c.getString(R.string.pref_key_access_token), "");
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = new FormBody.Builder()
                            .add("message",
                                    c.getString(R.string.line_notify_emergency_message) + "\n生存確認時刻: " + latestTimeText)
                            .build();
                    Request request = new Request.Builder()
                            .url(c.getString(R.string.url_line_notify_api_notify))
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .post(body)
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.d(TAG, "onFailure");
                        }
                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            Log.d(TAG, "onResponse");
                        }
                    });
                }
            } else {
                Log.d(TAG, "access_token NOT registered");
                notifyMessage = R.string.notify_message_token_not_registered;
            }

            // 通知を出す
            Notification notification = new NotificationCompat.Builder(c, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(c.getString(R.string.notify_title))
                    .setContentText(c.getString(notifyMessage) + "\n生存確認時刻: " + latestTimeText)
                    .build();
            notificationManager.notify(NOTIFICATION_ID_NORMAL, notification);

            // 繰り返し
            setAlarmLoop(c, 60 * 60 * 1000);    // 1 h
        } else {
            Log.d(TAG, "safety notify is no longer active");
        }
    }
}