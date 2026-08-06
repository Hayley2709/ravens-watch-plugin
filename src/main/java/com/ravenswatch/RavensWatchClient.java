package com.ravenswatch;

import javax.inject.Inject;
import okhttp3.*;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;

public class RavensWatchClient
{
    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private Gson gson;

    public void fetchEvents(String apiKey, String calendarId, CalendarCallback callback)
    {
        String url = "https://www.googleapis.com/calendar/v3/calendars/" + calendarId
                + "/events?key=" + apiKey + "&timeMin=" + java.time.Instant.now().toString()
                + "&singleEvents=true&orderBy=startTime";

        Request request = new Request.Builder().url(url).build();

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
                if (response.isSuccessful() && response.body() != null)
                {
                    String json = response.body().string();
                    CalendarResponse data = gson.fromJson(json, CalendarResponse.class);
                    callback.onSuccess(data);
                } else {
                    callback.onError("Failed with code: " + response.code());
                }
            }
        });
    }

    public interface CalendarCallback
    {
        void onSuccess(CalendarResponse response);
        void onError(String error);
    }

    // JSON Data Mapping Helper Classes
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
}
