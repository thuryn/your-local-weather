package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CustomWeatherUrlAnalyzer {

    private static final String TAG = "CustomWeatherUrlAnalyzer";
    private static final String GEMMA_MODEL_PATH = "gemma-2b-it-gpu-int4.bin";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public interface AnalysisCallback {
        void onSuccess(String result);
        void onFailure(Throwable t);
        void onStatusUpdate(String status);
    }

    public static void fetchAndAnalyzeUrl(Context context, String customUrlTemplate, AnalysisCallback callback) {
        callback.onStatusUpdate("Stahování ukázkových dat ze zadané URL...");
        
        String sampleUrl = customUrlTemplate;
        if (customUrlTemplate.contains("{0}") || customUrlTemplate.contains("{1}")) {
            // Default sample coordinates: Prague (50.08, 14.42)
            sampleUrl = MessageFormat.format(customUrlTemplate, "50.08", "14.42");
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(sampleUrl)
                .header("User-Agent", "YourLocalWeather/6.6.1 (Android; https://github.com/thuryn/your-local-weather)")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    callback.onFailure(new Exception("Chyba při stahování dat z URL: " + e.getLocalizedMessage()))
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure(new Exception("Chyba serveru (" + response.code() + ") při stahování z URL")));
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    mainHandler.post(() -> callback.onFailure(new Exception("Prázdná odpověď ze zadané URL")));
                    return;
                }
                String jsonResponse = body.string();
                mainHandler.post(() -> analyzeResponse(context, jsonResponse, callback));
            }
        });
    }

    public static void analyzeResponse(Context context, String jsonResponse, AnalysisCallback callback) {
        callback.onStatusUpdate("Probíhá chytrá analýza struktury JSON...");

        // 1. Try smart heuristic analysis first (100% local, instant, no API key or download required)
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject heuristicRules = analyzeHeuristically(jsonObject);
            if (heuristicRules.length() > 0) {
                callback.onSuccess(heuristicRules.toString());
                return;
            }
        } catch (Exception e) {
            // fallthrough to AI methods if heuristics fail to parse JSON
        }

        // 2. Try Gemini Nano (Placeholder for system-level AI)
        if (isGeminiNanoAvailable(context)) {
            callback.onStatusUpdate("Zkouším lokální Gemini Nano...");
        }

        // 3. Try Gemma (On-device, via MediaPipe)
        if (isGemmaModelDownloaded(context)) {
            callback.onStatusUpdate("Používám lokální model Gemma...");
            analyzeWithGemma(context, jsonResponse, callback);
            return;
        }

        // 4. Fallback to Online Gemini API
        String apiKey = AppPreference.getGeminiApiKey(context);
        if (!apiKey.isEmpty()) {
            callback.onStatusUpdate("Používám online službu Gemini...");
            analyzeWithOnlineService(context, jsonResponse, callback);
            return;
        }

        callback.onFailure(new Exception("Nepodařilo se automaticky rozpoznat strukturu JSON. Zkontrolujte URL adresu."));
    }

    public static JSONObject analyzeHeuristically(JSONObject json) {
        JSONObject rules = new JSONObject();
        try {
            findPath(json, "", new String[]{"temp", "temperature"}, rules, "temp_path");
            findPath(json, "", new String[]{"hum", "humidity", "relative_humidity"}, rules, "hum_path");
            findPath(json, "", new String[]{"pres", "pressure", "pressure_msl", "surface_pressure"}, rules, "pres_path");
            findPath(json, "", new String[]{"wind_speed", "wind_spd", "windspeed"}, rules, "wind_speed_path");
            findPath(json, "", new String[]{"wind_dir", "wind_direction", "wind_from_direction", "winddir"}, rules, "wind_dir_path");
        } catch (Exception e) {
            // ignore
        }
        return rules;
    }

    private static boolean findPath(Object current, String currentPath, String[] targetKeywords, JSONObject resultRules, String resultKey) throws JSONException {
        if (resultRules.has(resultKey)) {
            return true;
        }
        if (current instanceof JSONObject) {
            JSONObject obj = (JSONObject) current;
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = obj.get(key);
                String newPath = currentPath.isEmpty() ? key : currentPath + "." + key;

                for (String kw : targetKeywords) {
                    if (key.toLowerCase().contains(kw.toLowerCase())) {
                        if (val instanceof Number || (val instanceof String && isNumeric((String) val))) {
                            resultRules.put(resultKey, newPath);
                            return true;
                        }
                    }
                }
                if (findPath(val, newPath, targetKeywords, resultRules, resultKey)) {
                    return true;
                }
            }
        } else if (current instanceof JSONArray) {
            JSONArray arr = (JSONArray) current;
            if (arr.length() > 0) {
                Object first = arr.get(0);
                String newPath = currentPath + ".0";
                if (findPath(first, newPath, targetKeywords, resultRules, resultKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isGeminiNanoAvailable(Context context) {
        return false;
    }

    public static boolean isGemmaModelDownloaded(Context context) {
        File modelFile = new File(context.getFilesDir(), GEMMA_MODEL_PATH);
        return modelFile.exists();
    }

    private static void analyzeWithGemma(Context context, String jsonResponse, AnalysisCallback callback) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(new File(context.getFilesDir(), GEMMA_MODEL_PATH).getAbsolutePath())
                        .setMaxTokens(1024)
                        .build();

                try (LlmInference llmInference = LlmInference.createFromOptions(context, options)) {
                    String prompt = createPrompt(jsonResponse);
                    String result = llmInference.generateResponse(prompt);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        });
    }

    private static void analyzeWithOnlineService(Context context, String jsonResponse, AnalysisCallback callback) {
        String apiKey = AppPreference.getGeminiApiKey(context);
        if (apiKey.isEmpty()) {
            callback.onFailure(new Exception("Pro online AI analýzu je vyžadován klíč Gemini API Key. Zadejte jej v nastavení AI."));
            return;
        }

        OkHttpClient client = new OkHttpClient();
        try {
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", createPrompt(jsonResponse));
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + apiKey)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> callback.onFailure(new Exception("Chyba serveru: " + response.code())));
                        return;
                    }
                    ResponseBody responseBodyRef = response.body();
                    if (responseBodyRef == null) {
                        mainHandler.post(() -> callback.onFailure(new Exception("Prázdná odpověď serveru")));
                        return;
                    }
                    try {
                        String responseData = responseBodyRef.string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        String text = jsonObject.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                        mainHandler.post(() -> callback.onSuccess(text));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure(e));
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static String createPrompt(String jsonResponse) {
        return "Analyze this weather API JSON response and provide a mapping to extract weather data. " +
                "I need JSON paths for: current temperature (temp_path), humidity (hum_path), " +
                "pressure (pres_path), wind speed (wind_speed_path), wind direction (wind_dir_path). " +
                "Return ONLY a valid JSON object with these paths.\n\n" +
                "Response:\n" + jsonResponse;
    }
}