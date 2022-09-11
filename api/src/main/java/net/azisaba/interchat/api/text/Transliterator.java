package net.azisaba.interchat.api.text;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Transliterator {
    private static final String URL = "https://www.google.com/transliterate?langpair=ja-Hira|ja&text=";

    @NotNull
    public static String transliterate(@NotNull String text) {
        HttpURLConnection conn = null;

        try {
            String baseurl = URL + URLEncoder.encode(text, "UTF-8");
            URL url = new URL(baseurl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            try (InputStreamReader is = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(is)) {
                StringBuilder builder = new StringBuilder();
                String s;
                while ((s = reader.readLine()) != null) builder.append(s);

                return parseJson(builder.toString());
            }
        } catch (IOException ignored) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return text;
    }

    private static @NotNull String parseJson(String json) {
        StringBuilder result = new StringBuilder();
        for (JsonElement response : new Gson().fromJson(json, JsonArray.class)) {
            result.append(response.getAsJsonArray().get(1).getAsJsonArray().get(0).getAsString());
        }
        return result.toString();
    }
}
