package com.ravenswatch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.*;

@Singleton
public class RavensWatchClient
{
    @Inject
    private OkHttpClient okHttpClient;

    public interface CalendarCallback
    {
        void onSuccess(CalendarResponse response);
        void onError(String error);
    }

    public interface MotmCallback
    {
        void onSuccess(MotmResponse response);
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

    public static class MotmResponse
    {
        public String name;
        public String reason;
    }

    public void fetchEvents(String apiKey, String calendarId, CalendarCallback callback)
    {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("www.googleapis.com")
                .addPathSegments("calendar/v3/calendars")
                .addPathSegment(calendarId)
                .addPathSegment("events")
                .addQueryParameter("key", apiKey)
                .addQueryParameter("singleEvents", "true")
                .addQueryParameter("orderBy", "startTime")
                .build();

        Request request = new Request.Builder()
                .url(url)
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
                    callback.onError("HTTP " + response.code());
                    response.close();
                    return;
                }

                try
                {
                    String body = response.body().string();
                    CalendarResponse calResponse = new Gson().fromJson(body, CalendarResponse.class);
                    callback.onSuccess(calResponse);
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

    public void fetchMotm(String apiUrl, MotmCallback callback)
    {
        Request request = new Request.Builder()
                .url(apiUrl)
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

                    MotmResponse motm = new Gson().fromJson(rawJsonContent, MotmResponse.class);
                    callback.onSuccess(motm);
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
}