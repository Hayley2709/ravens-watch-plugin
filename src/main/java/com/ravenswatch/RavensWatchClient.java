package com.ravenswatch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Singleton
public class RavensWatchClient {
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    @Inject
    public RavensWatchClient(OkHttpClient okHttpClient, Gson gson) {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
    }

    // --- Inner Models ---

    public static class CalendarResponse {
        public List<CalendarEvent> items;
    }

    public static class CalendarEvent {
        public String summary;
        public EventTime start;
    }

    public static class EventTime {
        public String dateTime;
    }

    public static class WomCompetition {
        public int id;
        public String title;
        public String metric;
        public String startsAt;
        public String endsAt;
        public List<WomParticipation> participations = new ArrayList<>();

        public boolean isActive() {
            if (endsAt == null) {
                return false;
            }
            try {
                Instant endInstant = Instant.parse(endsAt);
                return endInstant.isAfter(Instant.now());
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class WomParticipation {
        public WomPlayer player;
        public WomProgress progress;
    }

    public static class WomPlayer {
        public String username;
    }

    public static class WomProgress {
        public double gained;
    }

    // --- Callback Interfaces ---

    public interface WomMemberCountCallback {
        void onSuccess(int count);

        void onError(String error);
    }

    public interface BroadcastsCallback {
        void onSuccess(List<String> broadcasts);

        void onError(String error);
    }

    public interface WomCompetitionsCallback {
        void onSuccess(List<WomCompetition> competitions);

        void onError(String error);
    }

    public interface WomCompetitionDetailsCallback {
        void onSuccess(WomCompetition competition);

        void onError(String error);
    }

    public interface CalendarCallback {
        void onSuccess(CalendarResponse response);

        void onError(String error);
    }

    public interface MotmCallback {
        void onSuccess(List<String> ravensList);

        void onError(String error);
    }

    // --- API Requests ---

    public void fetchWomMemberCount(int groupId, String apiKey, WomMemberCountCallback callback) {
        HttpUrl url = HttpUrl.parse("https://api.wiseoldman.net/v2/groups/" + groupId);
        if (url == null) return;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .header("x-api-key", apiKey)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP Error: " + response.code());
                    response.close();
                    return;
                }

                try {
                    JsonObject root = new JsonParser().parse(response.body().string()).getAsJsonObject();
                    int memberCount = root.has("memberCount") ? root.get("memberCount").getAsInt() : 0;
                    callback.onSuccess(memberCount);
                } catch (Exception e) {
                    callback.onError("Parsing failed: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void fetchEvents(String apiKey, String calendarId, CalendarCallback callback) {
        HttpUrl url = HttpUrl.parse("https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events")
                .newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("singleEvents", "true")
                .addQueryParameter("orderBy", "startTime")
                .addQueryParameter("timeMin", java.time.Instant.now().toString())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Server responded with code " + response.code());
                    response.close();
                    return;
                }

                try {
                    CalendarResponse calendarResponse = gson.fromJson(response.body().charStream(), CalendarResponse.class);
                    callback.onSuccess(calendarResponse);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void fetchBroadcasts(BroadcastsCallback callback) {
        String apiUrl = "https://api.github.com/gists/a817e90cf303915a38b8e55229227608";

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .header("Accept", "application/vnd.github+json")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP Error: " + response.code());
                    response.close();
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();

                    JsonObject filesObject = root.get("files").getAsJsonObject();
                    JsonObject fileData = filesObject.get("rw-broadcasts.json").getAsJsonObject();
                    String rawJsonContent = fileData.get("content").getAsString();

                    Type listType = new TypeToken<List<String>>() {
                    }.getType();
                    List<String> broadcasts = gson.fromJson(rawJsonContent, listType);

                    callback.onSuccess(broadcasts);
                } catch (Exception e) {
                    callback.onError("Parsing failed: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void fetchWomCompetitions(int groupId, String apiKey, WomCompetitionsCallback callback) {
        HttpUrl url = HttpUrl.parse("https://api.wiseoldman.net/v2/groups/" + groupId + "/competitions");
        if (url == null) return;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .header("x-api-key", apiKey)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP Error: " + response.code());
                    response.close();
                    return;
                }

                try {
                    Type listType = new TypeToken<List<WomCompetition>>() {
                    }.getType();
                    List<WomCompetition> competitions = gson.fromJson(response.body().charStream(), listType);
                    callback.onSuccess(competitions);
                } catch (Exception e) {
                    callback.onError("Parsing failed: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void fetchCompetitionDetails(int competitionId, String apiKey, WomCompetitionDetailsCallback callback) {
        HttpUrl url = HttpUrl.parse("https://api.wiseoldman.net/v2/competitions/" + competitionId);
        if (url == null) return;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .header("x-api-key", apiKey)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP Error: " + response.code());
                    response.close();
                    return;
                }

                try {
                    WomCompetition competition = gson.fromJson(response.body().charStream(), WomCompetition.class);
                    callback.onSuccess(competition);
                } catch (Exception e) {
                    callback.onError("Parsing failed: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void fetchMotm(String motmApiUrl, MotmCallback callback) {
        Request request = new Request.Builder()
                .url(motmApiUrl)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP Error: " + response.code());
                    response.close();
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();

                    JsonObject filesObject = root.get("files").getAsJsonObject();
                    JsonObject fileData = filesObject.get("ravenswatch-motm.json").getAsJsonObject();
                    String rawJsonContent = fileData.get("content").getAsString();

                    Type listType = new TypeToken<List<String>>() {
                    }.getType();
                    List<String> ravensList = gson.fromJson(rawJsonContent, listType);
                    callback.onSuccess(ravensList);
                } catch (Exception e) {
                    callback.onError("Parsing failed: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
}