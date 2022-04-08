package so.engage.android.sdk;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Engage {

    private static final int PUT = 1;
    private static final int POST = 2;

    private static String PUBLIC_KEY;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private static final String ROOT = "https://api.engage.so/v1";

    public static void init (String publicKey) {
        // Set keys
        PUBLIC_KEY = publicKey;
    }

    public static void identify (String id, HashMap<String, Object> properties) {
        // Normalize and Send to api
        HashMap<String, Object> data = new HashMap<>();
        HashMap<String, Object> meta = new HashMap<>();
        String[] standardAttributes = {"first_name", "last_name", "email", "number", "created_at"};
        Gson gson = new Gson();

        for (Map.Entry<String, Object> item : properties.entrySet()) {
            String key = (String) item.getKey();
            boolean isStandard = Arrays.asList(standardAttributes).contains(key);
            if (isStandard) {
                data.put(key, item.getValue());
            } else {
                meta.put(key, item.getValue());
            }
        }

        if (!meta.isEmpty()) {
            data.put("meta", meta);
        }

        try {
            _request("/users/" + id, PUT, gson.toJson(data));
        } catch (IOException e) {
            Log.e("Engage", e.toString());
        }
    }

    public static void setDeviceToken (String id, String token) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("device_token", token);
        data.put("device_platform", "android");

        try {
            Gson gson = new Gson();
            _request("/users/" + id, PUT, gson.toJson(data));
        } catch (IOException e) {}
    }

    public static void addAttributes (String id, HashMap<String, Object> attributes) {
        identify(id, attributes);
    }

    public static void trackEvents (String id, String event) {
        trackEvents(id, event, "", new Date());
    }
    public static void trackEvents (String id, String event, String value) {
        trackEvents(id, event, value, new Date());
    }
    public static void trackEvents (String id, String event, Date date) {
        trackEvents(id, event, "", date);
    }
    public static void trackEvents (String id, String event, HashMap properties) {
        trackEvents(id, event, properties, new Date());
    }
    public static void trackEvents (String id, String event, String value, Date date) {
        Gson gson = new Gson();
        HashMap<String, Object> data = new HashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        data.put("event", event);

        if (!value.isEmpty()) {
            data.put("value", value);
        }
        data.put("timestamp", dateFormat.format(date));

        try {
            _request("/users/" + id + "/events", POST, gson.toJson(data));
        } catch (IOException e) {
            Log.e("Engage", e.toString());
        }
    }
    public static void trackEvents (String id, String event, HashMap<String, Object> properties,
                                    Date date) {
        Gson gson = new Gson();
        HashMap<String, Object> data = new HashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        data.put("event", event);
        data.put("timestamp", dateFormat.format(date));
        data.put("properties", properties);

        try {
            _request("/users/" + id + "/events", POST, gson.toJson(data));
        } catch (IOException e) {
            Log.e("Engage", e.toString());
        }
    }

    private static void _request(String url, int type, String data)
            throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(data, JSON);
        String credential = Credentials.basic(PUBLIC_KEY, "");
        Request request = type == 1 ? new Request.Builder()
                .url(ROOT + url)
                .header("Authorization", credential)
                .put(body)
                .build() : new Request.Builder()
                .url(ROOT + url)
                .header("Authorization", credential)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {}

            @Override
            public void onFailure(Call call, IOException e) {}
        });
    }
}

