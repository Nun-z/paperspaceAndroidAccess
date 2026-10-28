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

// Talks to the Paperspace v1 machines API; async callbacks run on the main thread
public final class PaperspaceClient {
    private static final String API_BASE = "https://api.paperspace.com/v1/machines/";
    private static final OkHttpClient HTTP = new OkHttpClient();

    // The API answered, but not with success
    public static final class HttpException extends IOException {
        public final int code;

        HttpException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }

    public interface StatusCallback {
        void onState(String state);
        void onHttpError(int code);
        void onUnreachable();
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String reason);
    }

    private final Handler main = new Handler(Looper.getMainLooper());

    // Start, restart, or shutdown the machine; action is "start", "stop" or "restart"
    public void send(String machineId, String apiKey, String action, ActionCallback callback) {
        HTTP.newCall(actionRequest(machineId, apiKey, action)).enqueue(new Callback() {
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
        HTTP.newCall(statusRequest(machineId, apiKey)).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                main.post(callback::onUnreachable);
            }

            @Override
            public void onResponse(Call call, Response response) {
                int code = response.code();
                try (response) {
                    String state = parseState(response);
                    main.post(() -> callback.onState(state));
                } catch (IOException e) {
                    main.post(() -> callback.onHttpError(code));
                }
            }
        });
    }

    // Blocking version of send(), for background work such as the home-screen widget
    public void sendBlocking(String machineId, String apiKey, String action) throws IOException {
        try (Response response = HTTP.newCall(actionRequest(machineId, apiKey, action)).execute()) {
            if (!response.isSuccessful()) {
                throw new HttpException(response.code());
            }
        }
    }

    // Blocking version of getStatus(); returns the machine's state
    public String getStateBlocking(String machineId, String apiKey) throws IOException {
        try (Response response = HTTP.newCall(statusRequest(machineId, apiKey)).execute()) {
            return parseState(response);
        }
    }

    private static Request actionRequest(String machineId, String apiKey, String action) {
        return new Request.Builder()
                .url(API_BASE + machineId + "/" + action)
                .header("Authorization", "Bearer " + apiKey)
                .patch(RequestBody.create(new byte[0], null))
                .build();
    }

    private static Request statusRequest(String machineId, String apiKey) {
        return new Request.Builder()
                .url(API_BASE + machineId)
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();
    }

    private static String parseState(Response response) throws IOException {
        ResponseBody body = response.body();
        if (response.code() != 200 || body == null) {
            throw new HttpException(response.code());
        }
        try {
            return new JSONObject(body.string()).getString("state");
        } catch (JSONException e) {
            throw new HttpException(response.code());
        }
    }
}
