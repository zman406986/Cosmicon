package data.scripts;

import com.fs.starfarer.api.Global;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {
    private Strings() {}

    private static final String STRINGS_PATH = "data/config/strings.json";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");
    private static JSONObject strings = null;
    private static final Map<String, String> cache = new HashMap<>();

    public static void loadStrings() {
        try {
            strings = Global.getSettings().loadJSON(STRINGS_PATH, CosmiconConfig.MOD_ID);
            cache.clear();
            Global.getLogger(Strings.class).info("Cosmicon strings loaded successfully");
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Strings from " + STRINGS_PATH + " could not be loaded.", e);
        }
    }

    public static String get(String key) {
        if (strings == null) { loadStrings(); }

        String cached = cache.get(key);
        if (cached != null) return cached;

        final String[] parts = key.split("\\.");
        try {
            JSONObject current = strings;
            for (int i = 0; i < parts.length - 1; i++) {
                current = current.getJSONObject(parts[i]);
            }
            String value = current.getString(parts[parts.length - 1]);
            cache.put(key, value);
            return value;

        } catch (JSONException e) {
            throw new MissingResourceException("Missing translation for key: " + key, "Strings", key);
        }
    }

    public static String format(String key, Object... args) {
        String template = get(key);
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String replacement = index < args.length ? String.valueOf(args[index]) : matcher.group();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
