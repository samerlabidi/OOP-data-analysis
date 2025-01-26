package ml;


public interface ModelTrainer {
    Model trainModel(Dataset trainingData, ModelParameters params);
    Predictions predict(Model model, Dataset testData);
    double evaluateModel(Model model, Dataset validationData);
} 