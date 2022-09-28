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
import java.util.Collections;
import java.util.List;

public class Transliterator {
    private static final String URL = "https://www.google.com/transliterate?langpair=ja-Hira|ja&text=";

    @NotNull
    public static List<@NotNull String> transliterate(@NotNull String text) {
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

        return Collections.singletonList(text);
    }

    private static @NotNull List<@NotNull String> parseJson(String json) {
        List<String> list = new java.util.ArrayList<>();
        for (JsonElement response : new Gson().fromJson(json, JsonArray.class)) {
            JsonArray suggestions = response.getAsJsonArray().get(1).getAsJsonArray();
            suggestions.forEach(el -> list.add(el.getAsString()));
        }
        return list;
    }
}
