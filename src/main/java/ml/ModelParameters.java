package ml;

import java.util.Map;

public class ModelParameters {
    private final Map<String, Object> params;

    public ModelParameters(Map<String, Object> params) {
        this.params = params;
    }

    public Object get(String key) { return params.get(key); }
} 