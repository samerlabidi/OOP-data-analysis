package ml;

public class Model {
    private final String modelType;
    private final Object modelInstance;

    public Model(String modelType, Object modelInstance) {
        this.modelType = modelType;
        this.modelInstance = modelInstance;
    }

    public String getModelType() { return modelType; }
    public Object getModelInstance() { return modelInstance; }
} 