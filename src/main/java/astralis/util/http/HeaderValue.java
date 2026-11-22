package astralis.util.http;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeaderValue {
    public static final String EMPTY = "";
    private Map<String, String> parameters = new LinkedHashMap<>();
    private String value;

    private HeaderValue() {}

    public static HeaderValue parse(String header) {
        HeaderValue result = new HeaderValue();
        String[] parts = header.split(";");
        result.value = parts[0].trim();

        for (int i = 1; i < parts.length; i++) {
            String[] keyAndValue = parts[i].split("=");
            String parameterValue = "";
            if (keyAndValue.length > 1) {
                String rawValue = keyAndValue[1].trim();
                if (rawValue.startsWith("\"") && rawValue.endsWith("\"")
                        || rawValue.startsWith("'")
                                && rawValue.endsWith("'")
                                && rawValue.length() > 2) {
                    rawValue = rawValue.substring(1, rawValue.length() - 1);
                }

                parameterValue = rawValue;
            }

            result.parameters.put(keyAndValue[0].trim(), parameterValue);
        }

        return result;
    }

    public String getValue() {
        return this.value;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }
}
