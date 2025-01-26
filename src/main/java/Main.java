import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import analysis.CovidDataAnalyzer;
import config.AppConfig;
import dataIngestion.DataCleaner;
import dataIngestion.DataImporter;
import exception.DataImportException;
import exception.DatabaseException;
import model.DescriptiveStats;
import storage.DataStorage;
import visualization.CovidDataVisualizer;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            // Initialize components
            DataStorage storage = new DataStorage(
                AppConfig.getDatabaseUrl(),
                AppConfig.getDatabaseUser(),
                AppConfig.getDatabasePassword()
            );
            CovidDataAnalyzer analyzer = new CovidDataAnalyzer();
            CovidDataVisualizer visualizer = new CovidDataVisualizer();

            // Import and clean data
            List<CSVRecord> covidData = DataImporter.importCSV("data/covid.csv");
            List<CSVRecord> cleanedData = DataCleaner.cleanCSVData(covidData);

            // Store data
            storage.insertData(cleanedData);

            // Analyze data
            // Example: Calculate statistics for total cases
            List<Double> totalCases = cleanedData.stream()
                .map(record -> Double.parseDouble(record.get("TotalCases")))
                .collect(Collectors.toList());
            
            DescriptiveStats stats = analyzer.calculateDescriptiveStats(totalCases);
            System.out.println("COVID-19 Cases Statistics:");
            System.out.println("Mean: " + stats.getMean());
            System.out.println("Median: " + stats.getMedian());
            System.out.println("Standard Deviation: " + stats.getStandardDeviation());

            // Create visualizations
            // Example: Create histogram of total cases
            visualizer.createHistogram("Distribution of COVID-19 Cases", totalCases, 50);
            visualizer.exportChart(
                visualizer.createHistogram("Distribution of COVID-19 Cases", totalCases, 50),
                "output/covid_cases_histogram.png"
            );

        } catch (IOException e) {
            logger.error("Failed to read input files: {}", e.getMessage(), e);
            throw new DataImportException("Data import failed", e);
        } catch (SQLException e) {
            logger.error("Database operation failed: {}", e.getMessage(), e);
            throw new DatabaseException("Database operation failed", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred: {}", e.getMessage(), e);
            throw e;
        }
    }
}