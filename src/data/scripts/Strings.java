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
    private static final Map<String, String> cache = new HashMap<>();

    public static void loadStrings() {
        try {
            JSONObject strings = Global.getSettings().loadJSON(STRINGS_PATH, CosmiconConfig.MOD_ID);
            cache.clear();
            flattenJson("", strings, cache);
            Global.getLogger(Strings.class).info("Cosmicon strings loaded successfully");
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Strings from " + STRINGS_PATH + " could not be loaded.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void flattenJson(String prefix, JSONObject node, Map<String, String> target) {
        for (java.util.Iterator<String> it = node.keys(); it.hasNext(); ) {
            String key = it.next();
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value;
            try {
                value = node.get(key);
            } catch (JSONException e) {
                throw new RuntimeException("Failed to read JSON key: " + fullKey, e);
            }
            if (value instanceof JSONObject) {
                flattenJson(fullKey, (JSONObject) value, target);
            } else {
                target.put(fullKey, String.valueOf(value));
            }
        }
    }

    public static String get(String key) {
        String value = cache.get(key);
        if (value != null) return value;

        throw new MissingResourceException("Missing translation for key: " + key, "Strings", key);
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
