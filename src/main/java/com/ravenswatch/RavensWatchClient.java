package com.ravenswatch;

import com.google.gson.Gson;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.*;
import java.io.IOException;
import java.util.List;

@Singleton
public class RavensWatchClient
{
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    @Inject
    private RavensWatchClient(OkHttpClient okHttpClient, Gson gson)
    {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
    }

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

    public void fetchEvents(String apiKey, String calendarId, CalendarCallback callback)
    {
        String url = "https://www.googleapis.com/calendar/v3/calendars/" +
                calendarId + "/events?key=" + apiKey + "&singleEvents=true&orderBy=startTime";

        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (ResponseBody responseBody = response.body())
                {
                    if (!response.isSuccessful() || responseBody == null)
                    {
                        callback.onError("Unexpected code " + response);
                        return;
                    }
                    CalendarResponse calendarResponse = gson.fromJson(responseBody.charStream(), CalendarResponse.class);
                    callback.onSuccess(calendarResponse);
                }
                catch (Exception e) { callback.onError(e.getMessage()); }
            }
        });
    }

    public void fetchMotm(String jsonUrl, MotmCallback callback)
    {
        if (jsonUrl == null || jsonUrl.isEmpty()) {
            return;
        }

        Request request = new Request.Builder().url(jsonUrl).build();

        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (ResponseBody responseBody = response.body())
                {
                    if (!response.isSuccessful() || responseBody == null)
                    {
                        return;
                    }
                    MotmResponse motmResponse = gson.fromJson(responseBody.charStream(), MotmResponse.class);
                    callback.onSuccess(motmResponse);
                }
                catch (Exception e) { callback.onError(e.getMessage()); }
            }
        });
    }

    public static class CalendarResponse {
        public List<CalendarEvent> items;
    }

    public static class CalendarEvent {
        public String summary;
        public EventTime start;
    }

    public static class EventTime {
        public String dateTime;
        public String date;
    }

    public static class MotmResponse {
        public String name;
        public String reason;
    }
}