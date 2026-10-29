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

    // The API answered, but not with success; the message includes the API's reason when it gives one
    public static final class HttpException extends IOException {
        public final int code;

        HttpException(int code, String apiMessage) {
            super(apiMessage == null || apiMessage.isEmpty() ? "HTTP " + code : "HTTP " + code + ": " + apiMessage);
            this.code = code;
        }
    }

    // True when the machine is already where the action would take it; the API rejects those with HTTP 400
    public static boolean isAlreadyDone(String action, String state) {
        return ("stop".equals(action) && "off".equals(state))
                || ("start".equals(action) && "ready".equals(state));
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
                try (response) {
                    if (response.isSuccessful()) {
                        main.post(callback::onSuccess);
                    } else {
                        String reason = httpError(response).getMessage();
                        main.post(() -> callback.onFailure(reason));
                    }
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
                throw httpError(response);
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
            throw httpError(response);
        }
        try {
            return new JSONObject(body.string()).getString("state");
        } catch (JSONException e) {
            throw new HttpException(response.code(), null);
        }
    }

    // Reads the API's reason from an error body such as {"message": "..."} or {"error": {"message": "..."}}
    private static HttpException httpError(Response response) {
        String message = null;
        try {
            ResponseBody body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body.string());
                message = json.isNull("message") ? null : json.optString("message");
                Object error = json.opt("error");
                if (message == null && error instanceof JSONObject) {
                    JSONObject errorJson = (JSONObject) error;
                    message = errorJson.isNull("message") ? null : errorJson.optString("message");
                } else if (message == null && error instanceof String) {
                    message = (String) error;
                }
            }
        } catch (IOException | JSONException e) {
            // No readable reason; the HTTP code alone will have to do
        }
        return new HttpException(response.code(), message);
    }
}
