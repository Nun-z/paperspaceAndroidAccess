package com.nunz.paperspaceaccess;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

// Talks to the Paperspace v1 machines API; callbacks run on the main thread
public final class PaperspaceClient {
    private static final String API_BASE = "https://api.paperspace.com/v1/machines/";

    public interface StatusCallback {
        void onState(String state);
        void onHttpError(int code);
        void onUnreachable();
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String reason);
    }

    private final OkHttpClient http = new OkHttpClient();
    private final Handler main = new Handler(Looper.getMainLooper());

    // Start, restart, or shutdown the machine; action is "start", "stop" or "restart"
    public void send(String machineId, String apiKey, String action, ActionCallback callback) {
        Request request = new Request.Builder()
                .url(API_BASE + machineId + "/" + action)
                .header("Authorization", "Bearer " + apiKey)
                .patch(RequestBody.create(new byte[0], null))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                main.post(() -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                int code = response.code();
                response.close();
                if (code >= 200 && code < 300) {
                    main.post(callback::onSuccess);
                } else {
                    main.post(() -> callback.onFailure("HTTP " + code));
                }
            }
        });
    }

    // Get the status of the machine
    public void getStatus(String machineId, String apiKey, StatusCallback callback) {
        Request request = new Request.Builder()
                .url(API_BASE + machineId)
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                main.post(callback::onUnreachable);
            }

            @Override
            public void onResponse(Call call, Response response) {
                int code = response.code();
                try (ResponseBody body = response.body()) {
                    if (code != 200 || body == null) {
                        main.post(() -> callback.onHttpError(code));
                        return;
                    }
                    String state = new JSONObject(body.string()).getString("state");
                    main.post(() -> callback.onState(state));
                } catch (IOException | JSONException e) {
                    main.post(() -> callback.onHttpError(code));
                }
            }
        });
    }
}
