package com.ravenswatch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
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
public class RavensWatchClient
{
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    @Inject
    public RavensWatchClient(OkHttpClient okHttpClient, Gson gson)
    {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
    }

    public void fetchEvents(String apiKey, String calendarId, CalendarCallback callback)
    {
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

        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful())
                {
                    callback.onError("Server responded with code " + response.code());
                    response.close();
                    return;
                }

                try
                {
                    CalendarResponse calendarResponse = gson.fromJson(response.body().charStream(), CalendarResponse.class);
                    callback.onSuccess(calendarResponse);
                }
                catch (Exception e)
                {
                    callback.onError(e.getMessage());
                }
                finally
                {
                    response.close();
                }
            }
        });
    }

    public void fetchMotm(String motmApiUrl, MotmCallback callback)
    {
        Request request = new Request.Builder()
                .url(motmApiUrl)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "RavensWatch-RuneLite-Plugin")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful())
                {
                    callback.onError("HTTP Error: " + response.code());
                    response.close();
                    return;
                }

                try
                {
                    String responseBody = response.body().string();
                    JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();

                    JsonObject filesObject = root.get("files").getAsJsonObject();
                    JsonObject fileData = filesObject.get("ravenswatch-motm.json").getAsJsonObject();
                    String rawJsonContent = fileData.get("content").getAsString();

                    Type listType = new TypeToken<List<String>>(){}.getType();
                    List<String> ravensList = gson.fromJson(rawJsonContent, listType);
                    callback.onSuccess(ravensList);
                }
                catch (Exception e)
                {
                    callback.onError("Parsing failed: " + e.getMessage());
                }
                finally
                {
                    response.close();
                }
            }
        });
    }

    public interface CalendarCallback
    {
        void onSuccess(CalendarResponse response);
        void onError(String error);
    }

    public interface MotmCallback
    {
        void onSuccess(List<String> ravensList);
        void onError(String error);
    }

    public static class CalendarResponse
    {
        public List<CalendarEvent> items;
    }

    public static class CalendarEvent
    {
        public String summary;
        public EventTime start;
    }

    public static class EventTime
    {
        public String dateTime;
    }
}