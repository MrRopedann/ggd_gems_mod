package su.ggd.ggd_gems_mod.passive.api;

import java.util.List;
import java.util.Map;

public final class JsonParamUtil {
    private JsonParamUtil() {}

    // ===== Map<String,Object> support =====

    public static double getDouble(Map<String, Object> params, String key, double def) {
        if (params == null || key == null) return def;
        Object v = params.get(key);
        if (v == null) return def;

        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof Boolean b) return b ? 1.0 : 0.0;

        if (v instanceof String s) {
            try { return Double.parseDouble(s.replace(',', '.')); }
            catch (Exception ignored) { return def; }
        }
        return def;
    }

    public static List<String> getStringList(Map<String, Object> params, String key) {
        return getStringList(params, key, List.of());
    }


    public static int getInt(Map<String, Object> params, String key, int def) {
        if (params == null || key == null) return def;
        Object v = params.get(key);
        if (v == null) return def;

        if (v instanceof Number n) return n.intValue();
        if (v instanceof Boolean b) return b ? 1 : 0;

        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); }
            catch (Exception ignored) { return def; }
        }
        return def;
    }

    public static boolean getBool(Map<String, Object> params, String key, boolean def) {
        if (params == null || key == null) return def;
        Object v = params.get(key);
        if (v == null) return def;

        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;

        if (v instanceof String s) {
            String t = s.trim().toLowerCase();
            if (t.equals("true") || t.equals("1") || t.equals("yes")) return true;
            if (t.equals("false") || t.equals("0") || t.equals("no")) return false;
        }
        return def;
    }

    public static List<String> getStringList(Map<String, Object> params, String key, List<String> def) {
        if (params == null || key == null) return def;
        Object v = params.get(key);
        if (v == null) return def;

        if (v instanceof List<?> list) {
            // Gson обычно даёт ArrayList<...> уже строками, но страхуемся
            return list.stream().map(String::valueOf).toList();
        }
        return def;
    }

    public static void putIfMissing(Map<String, Object> params, String key, Object value) {
        if (params == null || key == null) return;
        if (!params.containsKey(key)) params.put(key, value);
    }
}
