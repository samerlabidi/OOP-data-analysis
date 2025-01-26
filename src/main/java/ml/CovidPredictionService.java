package ml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class CovidPredictionService {
    
    private final ModelTrainer trainer;
    private static final int PREDICTION_DAYS = 30; // Increased from 7 to 30 days
    
    public CovidPredictionService() {
        this.trainer = new LinearRegressionTrainer();
    }
    
    public Map<String, Object> trainAndPredict(List<Double> timePoints, List<Double> cases) {
        // Prepare training data
        double[][] features = new double[timePoints.size()][1];
        double[] labels = new double[cases.size()];
        
        for (int i = 0; i < timePoints.size(); i++) {
            features[i][0] = timePoints.get(i);
            labels[i] = cases.get(i);
        }
        
        Dataset trainingData = new Dataset(features, labels);
        
        // Train model
        Model model = trainer.trainModel(trainingData, new ModelParameters(new HashMap<>()));
        
        // Prepare prediction data (next 30 days)
        double lastDay = timePoints.get(timePoints.size() - 1);
        double[][] predictionFeatures = new double[PREDICTION_DAYS][1];
        for (int i = 0; i < PREDICTION_DAYS; i++) {
            predictionFeatures[i][0] = lastDay + i + 1;
        }
        
        Dataset predictionData = new Dataset(predictionFeatures, new double[PREDICTION_DAYS]);
        
        // Make predictions
        Predictions predictions = trainer.predict(model, predictionData);
        
        // Evaluate model
        double accuracy = trainer.evaluateModel(model, trainingData);
        
        // Prepare results
        Map<String, Object> results = new HashMap<>();
        results.put("predictions", predictions.getPredictedValues());
        results.put("accuracy", accuracy);
        results.put("predictionDates", predictionFeatures);
        
        return results;
    }
} 