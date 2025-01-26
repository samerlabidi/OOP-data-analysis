import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ml.Dataset;
import ml.LinearRegressionTrainer;
import ml.Model;
import ml.ModelParameters;
import ml.ModelTrainer;
import ml.Predictions;

@Service
public class CovidPredictionService {
    
    private final ModelTrainer trainer;
    
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
        
        // Prepare prediction data (next 7 days)
        double lastDay = timePoints.get(timePoints.size() - 1);
        double[][] predictionFeatures = new double[7][1];
        for (int i = 0; i < 7; i++) {
            predictionFeatures[i][0] = lastDay + i + 1;
        }
        
        Dataset predictionData = new Dataset(predictionFeatures, new double[7]);
        
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