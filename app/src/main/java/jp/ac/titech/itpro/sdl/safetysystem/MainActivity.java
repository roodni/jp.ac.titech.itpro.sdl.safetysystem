package jp.ac.titech.itpro.sdl.safetysystem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button buttonTokenWebPage;
    private EditText editTextToken;
    private Button buttonTokenUpdate;
    private Button buttonTestSend;

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonTokenWebPage = findViewById(R.id.buttonTokenWebPage);
        editTextToken = findViewById(R.id.editTextToken);
        buttonTokenUpdate = findViewById(R.id.buttonTokenUpdate);
        buttonTestSend = findViewById(R.id.buttonTestSend);

        sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);

        buttonTokenWebPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://notify-bot.line.me/my/");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(i);
            }
        });

        buttonTokenUpdate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                v.setEnabled(false);
                String accessToken = editTextToken.getText().toString();
                Runnable invalid = () -> {
                    // トークンが無効だった場合
                    Log.d(TAG, "token update: invalid");
                    v.setEnabled(true);
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.dialog_token_update_invalid)
                            .show();
                };
                Runnable failed = () -> {
                    // トークンの確認に失敗した場合
                    Log.d(TAG, "token update: fail");
                    v.setEnabled(true);
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.dialog_token_update_fail)
                            .show();
                };

                OkHttpClient client = new OkHttpClient();
                try {
                    Request request = new Request.Builder()
                            .url("https://notify-api.line.me/api/status")
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .get()
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        Handler handler = new Handler(Looper.getMainLooper());
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            handler.post(failed);
                        }
                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            String body = response.body().string();
                            try {
                                JSONObject json = new JSONObject(body);
                                int status = json.getInt("status");
                                if (status == 200) {
                                    // トークンが有効だった場合
                                    String target = json.getString("target");
                                    Log.d(TAG, "token update: valid, " + target);
                                    // トークンの保存
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString(getString(R.string.pref_key_access_token), accessToken);
                                    editor.apply();
                                    // UIの更新
                                    handler.post(() -> {
                                        editTextToken.setText("");
                                        v.setEnabled(true);
                                        updateViewTokenState();
                                        new AlertDialog.Builder(MainActivity.this)
                                                .setMessage(R.string.dialog_token_update_valid)
                                                .show();
                                    });
                                } else {
                                    // トークンが無効だった場合
                                    handler.post(invalid);
                                }
                            } catch (JSONException e) {
                                handler.post(failed);
                            }
                        }
                    });
                } catch (IllegalArgumentException e) {
                    // accessToken がヘッダに載らない場合
                    invalid.run();
                }
            }
        });

        buttonTestSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String accessToken = sharedPref.getString(getString(R.string.pref_key_access_token), "");

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new FormBody.Builder()
                        .add("message", "送信テストです")
                        .build();
                try {
                    Request request = new Request.Builder()
                            .url("https://notify-api.line.me/api/notify")
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .post(requestBody)
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.e(TAG, "notify failed");
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            Log.d(TAG, "notify");
                            Log.d(TAG, "code: " + response.code());
                        }
                    });
                } catch(Exception e) {
                    Log.d(TAG, "notify error");
                }
            }
        });

        updateViewTokenState();
    }

    private void updateViewTokenState() {
        String accessToken = sharedPref.getString(getString(R.string.pref_key_access_token), "");
        TextView textView = findViewById(R.id.textView);
        textView.setText(accessToken);
    }
}