package coral.util;

/**
 * @author Ricky Fung
 * @version 1.0
 * @since 2018-06-24 22:43
 */
public class SystemPropertiesUtils {

    public static String getString(String propName) {
        return System.getProperty(propName);
    }

    public static Integer getInt(String propName) {
        return Integer.parseInt(getString(propName));
    }

    public static Long getLong(String propName) {
        return Long.parseLong(getString(propName));
    }
}
