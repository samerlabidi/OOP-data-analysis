package ml;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class LinearRegressionTrainer implements ModelTrainer {
    
    @Override
    public Model trainModel(Dataset trainingData, ModelParameters params) {
        SimpleRegression regression = new SimpleRegression();
        
        // Add data points to the model
        double[][] features = trainingData.getFeatures();
        double[] labels = trainingData.getLabels();
        
        for (int i = 0; i < features.length; i++) {
            regression.addData(features[i][0], labels[i]);
        }
        
        return new Model("LinearRegression", regression);
    }
    
    @Override
    public Predictions predict(Model model, Dataset testData) {
        if (!"LinearRegression".equals(model.getModelType())) {
            throw new IllegalArgumentException("Invalid model type");
        }
        
        SimpleRegression regression = (SimpleRegression) model.getModelInstance();
        double[][] features = testData.getFeatures();
        double[] predictions = new double[features.length];
        
        for (int i = 0; i < features.length; i++) {
            predictions[i] = regression.predict(features[i][0]);
        }
        
        return new Predictions(predictions);
    }
    
    @Override
    public double evaluateModel(Model model, Dataset validationData) {
        if (!"LinearRegression".equals(model.getModelType())) {
            throw new IllegalArgumentException("Invalid model type");
        }
        
        SimpleRegression regression = (SimpleRegression) model.getModelInstance();
        return regression.getRSquare(); // Return R-squared as evaluation metric
    }
} 