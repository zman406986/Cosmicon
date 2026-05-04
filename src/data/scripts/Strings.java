package data.scripts;

import com.fs.starfarer.api.Global;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.MissingResourceException;

public class Strings {
    private static final String STRINGS_PATH = "data/config/strings.json";
    private static JSONObject strings = null;

    static { }

    public static void loadStrings() {
        try {
            strings = Global.getSettings().loadJSON(STRINGS_PATH, CosmiconConfig.MOD_ID);
            Global.getLogger(Strings.class).info("Cosmicon strings loaded successfully");
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Strings from " + STRINGS_PATH + " could not be loaded.", e);
        }
    }

    public static String get(String key) {
        if (strings == null) { loadStrings(); }

        final String[] parts = key.split("\\.");
        try {
            JSONObject current = strings;
            for (int i = 0; i < parts.length - 1; i++) {
                current = current.getJSONObject(parts[i]);
            }
            return current.getString(parts[parts.length - 1]);

        } catch (JSONException e) {
            throw new MissingResourceException("Missing translation for key: " + key, "Strings", key);
        }
    }

    public static String format(String key, Object... args) {
        String template = get(key);
        StringBuilder sb = new StringBuilder(template);
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";
            int idx = sb.indexOf(placeholder);
            if (idx >= 0) {
                sb.replace(idx, idx + placeholder.length(), String.valueOf(args[i]));
            }
        }
        return sb.toString();
    }
}