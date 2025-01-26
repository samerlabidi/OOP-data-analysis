package controller;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import analysis.CovidDataAnalyzer;
import model.CovidDeath;
import visualization.AdvancedCovidVisualizer;
import visualization.CovidDataVisualizer;

@Controller
public class CovidAnalysisController {
    private final CovidDataAnalyzer analyzer;
    private final CovidDataVisualizer visualizer;
    private final AdvancedCovidVisualizer advancedVisualizer;
    private List<CSVRecord> currentRecords;
    private Map<String, List<Double>> dataMap;

    @Autowired
    public CovidAnalysisController(CovidDataAnalyzer analyzer, CovidDataVisualizer visualizer, AdvancedCovidVisualizer advancedVisualizer) {
        this.analyzer = analyzer;
        this.visualizer = visualizer;
        this.advancedVisualizer = advancedVisualizer;
        this.dataMap = new HashMap<>();
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                             @RequestParam("fileType") String fileType,
                             Model model) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Please select a file to upload");
            }

            if ("csv".equals(fileType)) {
                // Process CSV file
                Reader reader = new InputStreamReader(file.getInputStream());
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
                List<CSVRecord> records = csvParser.getRecords();
                
                Map<String, Object> processedData = processCSVData(records);
                
                // Check data type and return appropriate view
                if ("timeSeries".equals(processedData.get("type"))) {
                    model.addAttribute("data", processedData);
                    return "timeseries-analysis";
                } else {
                    model.addAttribute("data", processedData);
                    return "csv-analysis";
                }
            } else if ("json".equals(fileType)) {
                // Process JSON file from resources folder
                InputStream inputStream = getClass().getResourceAsStream("/coviddeath.json");
                List<CovidDeath> deathStats = processDeathData(inputStream);
                Map<String, Object> viewData = processJSONData(deathStats);
                
                // Debug prints
                System.out.println("Death Stats size: " + deathStats.size());
                System.out.println("View Data keys: " + viewData.keySet());
                System.out.println("Condition Groups: " + viewData.get("conditionGroups"));
                System.out.println("Age Groups: " + viewData.get("ageGroups"));
                
                model.addAttribute("chartData", viewData);
                return "json-analysis";
            }
            
            return "index";
        } catch (Exception e) {
            model.addAttribute("error", "Error processing file: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/upload-multiple")
    public String uploadFiles(
        @RequestParam("currentData") MultipartFile currentData,
        @RequestParam("timeSeriesData") MultipartFile timeSeriesData,
        @RequestParam("deathData") MultipartFile deathData,
        Model model) {
        
        System.out.println("Starting comprehensive analysis...");
        
        try {
            // Process the uploaded files directly instead of reading from resources
            List<String[]> csvData = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(timeSeriesData.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    csvData.add(line.split(","));
                }
            }

            if (csvData.isEmpty()) {
                throw new RuntimeException("No CSV data available");
            }

            // Skip header row and process data
            List<Double> cases = new ArrayList<>();
            List<Double> deaths = new ArrayList<>();
            List<String> dates = new ArrayList<>();

            for (int i = 1; i < csvData.size(); i++) {
                String[] row = csvData.get(i);
                if (row.length > 3) {
                    cases.add(parseDoubleOrZero(row[2]));
                    deaths.add(parseDoubleOrZero(row[3]));
                    dates.add(row[0]); // Assuming date is in first column
                }
            }

            // Calculate daily changes
            List<Double> dailyNewCases = calculateDailyChanges(cases);
            List<Double> dailyNewDeaths = calculateDailyChanges(deaths);
            
            // Calculate growth rates
            List<Double> growthRates = calculateGrowthRates(cases);

            // Process death data from the uploaded JSON file
            List<CovidDeath> deathStats = new ArrayList<>();
            if (!deathData.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                deathStats = Arrays.asList(mapper.readValue(deathData.getInputStream(), CovidDeath[].class));
            }

            // Process age distribution data
            Map<String, Double> ageDistribution = new TreeMap<>();
            if (!deathStats.isEmpty()) {
                ageDistribution = deathStats.stream()
                    .filter(d -> d.getAgeGroup() != null && !d.getAgeGroup().equals("All ages"))
                    .collect(Collectors.groupingBy(
                        CovidDeath::getAgeGroup,
                        Collectors.summingDouble(d -> {
                            try {
                                return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                            } catch (Exception e) {
                                return 0.0;
                            }
                        })
                    ));
            }

            // Calculate age-adjusted risk
            double ageAdjustedRisk = 0.0;
            if (!ageDistribution.isEmpty()) {
                double totalDeaths = ageDistribution.values().stream().mapToDouble(Double::doubleValue).sum();
                double totalCases = cases.get(cases.size() - 1);
                
                // Base mortality rate
                double baseMortalityRate = (totalDeaths / totalCases);
                
                // Calculate weighted risk by age group
                double weightedRisk = 0.0;
                double totalWeight = 0.0;
                
                for (Map.Entry<String, Double> entry : ageDistribution.entrySet()) {
                    String ageGroup = entry.getKey();
                    double ageGroupDeaths = entry.getValue();
                    
                    // Assign weights based on age groups
                    double weight;
                    if (ageGroup.startsWith("0")) weight = 0.2;      // 0-17
                    else if (ageGroup.startsWith("1")) weight = 0.4; // 18-29
                    else if (ageGroup.startsWith("3")) weight = 0.6; // 30-39
                    else if (ageGroup.startsWith("4")) weight = 0.8; // 40-49
                    else if (ageGroup.startsWith("5")) weight = 1.0; // 50-64
                    else if (ageGroup.startsWith("6")) weight = 1.2; // 65-74
                    else if (ageGroup.startsWith("7")) weight = 1.4; // 75-84
                    else weight = 1.6;                               // 85+
                    
                    weightedRisk += (ageGroupDeaths / totalDeaths) * weight;
                    totalWeight += weight;
                }
                
                // Calculate final risk
                if (totalWeight > 0) {
                    ageAdjustedRisk = (weightedRisk / totalWeight) * baseMortalityRate * 100;
                    ageAdjustedRisk = Math.round(ageAdjustedRisk * 10.0) / 10.0; // Round to 1 decimal place
                }
            }

            // Add to model
            model.addAttribute("ageAdjustedRisk", ageAdjustedRisk);

            // Add all data to model
            model.addAttribute("dates", dates);
            model.addAttribute("casesData", cases);
            model.addAttribute("deathsData", deaths);
            model.addAttribute("dailyNewCases", dailyNewCases);
            model.addAttribute("dailyNewDeaths", dailyNewDeaths);
            model.addAttribute("growthRateData", growthRates);
            model.addAttribute("ageDistribution", ageDistribution);

            return "comprehensive-analysis";
        } catch (Exception e) {
            System.err.println("Error in comprehensive analysis: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error processing data: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/analysis")
    public String showAnalysis(Model model) {
        if (currentRecords == null || currentRecords.isEmpty()) {
            model.addAttribute("error", "No data available for analysis");
            return "error";
        }

        try {
            List<Double> cases = new ArrayList<>();
            List<Double> deaths = new ArrayList<>();
            List<String> dates = new ArrayList<>();

            for (CSVRecord record : currentRecords) {
                cases.add(Double.parseDouble(record.get("TotalCases")));
                deaths.add(Double.parseDouble(record.get("TotalDeaths")));
                // Using index for date since it might not be present
                dates.add(String.valueOf(dates.size() + 1));
            }

            // Add raw data to model
            model.addAttribute("casesData", cases);
            model.addAttribute("deathsData", deaths);
            model.addAttribute("dates", dates);

            return "analysis";
        } catch (Exception e) {
            model.addAttribute("error", "Error processing data: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/analyze-json")
    public String analyzeJSON(Model model) {
        try {
            List<CovidDeath> deathStats = readJSONFile();
            
            if (deathStats == null || deathStats.isEmpty()) {
                throw new IllegalStateException("No data found in JSON file");
            }

            // Create a simple map for the view
            Map<String, Map<String, Double>> viewData = new HashMap<>();
            
            // Process age group deaths
            Map<String, Double> ageGroups = deathStats.stream()
                .filter(d -> d.getAgeGroup() != null && !d.getAgeGroup().equals("All ages"))
                .collect(Collectors.groupingBy(
                    CovidDeath::getAgeGroup,
                    Collectors.summingDouble(d -> {
                        try {
                            return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                        } catch (Exception e) {
                            return 0.0;
                        }
                    })
                ));
            
            viewData.put("ageGroups", ageGroups);
            
            // Debug print
            System.out.println("View data: " + viewData);
            
            // Add to model
            model.addAttribute("chartData", viewData);
            
            return "json-analysis";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    private void processData() {
        CSVRecord header = currentRecords.get(0);
        
        // First check which format we have
        String casesColumn = header.isMapped("Confirmed") ? "Confirmed" : "TotalCases";
        String deathsColumn = header.isMapped("Deaths") ? "Deaths" : "TotalDeaths";

        dataMap.put("cases", currentRecords.stream()
            .mapToDouble(record -> {
                try {
                    String value = record.get(casesColumn);
                    return Double.parseDouble(value.isEmpty() ? "0" : value);
                } catch (IllegalArgumentException e) {
                    return 0.0;
                }
            })
            .boxed()
            .collect(Collectors.toList()));

        dataMap.put("deaths", currentRecords.stream()
            .mapToDouble(record -> {
                try {
                    String value = record.get(deathsColumn);
                    return Double.parseDouble(value.isEmpty() ? "0" : value);
                } catch (IllegalArgumentException e) {
                    return 0.0;
                }
            })
            .boxed()
            .collect(Collectors.toList()));

        dataMap.put("dates", currentRecords.stream()
            .map(currentRecords::indexOf)
            .map(Double::valueOf)
            .collect(Collectors.toList()));
    }

    private Map<String, Object> processJsonData(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<CovidDeath> deaths = mapper.readValue(inputStream, 
            mapper.getTypeFactory().constructCollectionType(List.class, CovidDeath.class));

        Map<String, Object> viewData = new HashMap<>();
        
        // Group deaths by condition and age group
        Map<String, List<CovidDeath>> byCondition = deaths.stream()
            .collect(Collectors.groupingBy(CovidDeath::getConditionGroup));

        viewData.put("conditionGroups", byCondition);
        viewData.put("ageGroups", deaths.stream()
            .collect(Collectors.groupingBy(CovidDeath::getAgeGroup)));
        
        return viewData;
    }

    private String generateChart(String type) throws IOException {
        JFreeChart chart;
        switch (type) {
            case "histogram":
                chart = visualizer.createHistogram("COVID-19 Cases Distribution", 
                    dataMap.get("cases"), 50);
                break;
            case "scatter":
                chart = visualizer.createScatterPlot("Cases vs Deaths", 
                    dataMap.get("cases"), dataMap.get("deaths"));
                break;
            case "timeseries":
                XYSeries series = new XYSeries("Cases over time");
                List<Double> dates = dataMap.get("dates");
                List<Double> cases = dataMap.get("cases");
                for (int i = 0; i < dates.size(); i++) {
                    series.add(dates.get(i), cases.get(i));
                }
                chart = visualizer.createLineChart("Cases Timeline", "Day", "Cases", 
                    new XYSeriesCollection(series));
                break;
            default:
                throw new IllegalArgumentException("Unknown chart type: " + type);
        }

        ByteArrayOutputStream chartImage = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartImage, chart, 600, 400);
        return Base64.getEncoder().encodeToString(chartImage.toByteArray());
    }

    private Map<String, Object> processCurrentData(List<CSVRecord> records) {
        Map<String, Object> data = new HashMap<>();
        
        List<Double> cases = new ArrayList<>();
        List<Double> deaths = new ArrayList<>();
        List<String> countries = new ArrayList<>();
        
        // Skip header
        for (int i = 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            try {
                String casesStr = record.get("TotalCases").trim().replace(",", "");
                String deathsStr = record.get("TotalDeaths").trim().replace(",", "");
                String country = record.get("Country/Region").trim();
                
                // Only add if we have valid data
                if (!casesStr.isEmpty() && !deathsStr.isEmpty()) {
                    cases.add(Double.parseDouble(casesStr));
                    deaths.add(Double.parseDouble(deathsStr));
                    countries.add(country);
                }
            } catch (Exception e) {
                System.out.println("Error processing row " + i + ": " + e.getMessage());
            }
        }
        
        data.put("cases", cases);
        data.put("deaths", deaths);
        data.put("countries", countries);
        
        return data;
    }

    private Map<String, List<Double>> processTimeSeriesData(List<CSVRecord> records) {
        Map<String, List<Double>> stats = new HashMap<>();
        
        List<Double> confirmed = new ArrayList<>();
        List<Double> deaths = new ArrayList<>();

        for (CSVRecord record : records) {
            try {
                // Get confirmed cases, handle empty or invalid values
                String confirmedStr = record.get("Confirmed").trim();
                double confirmedValue = confirmedStr.isEmpty() ? 0.0 : Double.parseDouble(confirmedStr);
                confirmed.add(confirmedValue);
                
                // Get deaths
                String deathStr = record.get("Deaths").trim();
                double deathValue = deathStr.isEmpty() ? 0.0 : Double.parseDouble(deathStr);
                deaths.add(deathValue);
            } catch (Exception e) {
                confirmed.add(0.0);
                deaths.add(0.0);
            }
        }

        // Debug prints
        System.out.println("Number of records: " + records.size());
        System.out.println("First few confirmed values: " + confirmed.subList(0, Math.min(5, confirmed.size())));
        
        stats.put("confirmed", confirmed);
        stats.put("deaths", deaths);
        return stats;
    }

    private List<CovidDeath> processDeathData(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, 
            mapper.getTypeFactory().constructCollectionType(List.class, CovidDeath.class));
    }

    private List<Double> calculateDailyNew(List<Double> cumulative) {
        List<Double> daily = new ArrayList<>();
        for (int i = 1; i < cumulative.size(); i++) {
            daily.add(Math.max(0, cumulative.get(i) - cumulative.get(i-1)));
        }
        return daily;
    }

    private List<Double> calculateDailyGrowthRates(List<Double> data) {
        List<Double> rates = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            double previous = data.get(i-1);
            double current = data.get(i);
            if (previous > 0) {
                rates.add(((current - previous) / previous) * 100);
            } else {
                rates.add(0.0);
            }
        }
        return rates;
    }

    private double calculateGrowthRate(List<Double> data) {
        if (data == null || data.size() < 2) return 0.0;
        
        // Get first non-zero value
        double first = data.stream()
            .filter(value -> value > 0)
            .findFirst()
            .orElse(0.0);
            
        // Get last value
        double last = data.get(data.size() - 1);
        
        // Debug prints
        System.out.println("First non-zero value: " + first);
        System.out.println("Last value: " + last);
        
        if (first <= 0) return 0.0;
        
        double rate = ((last - first) / first) * 100;
        System.out.println("Calculated growth rate: " + rate + "%");
        
        return rate;
    }

    private Map<String, Object> calculateGlobalStatistics(Map<String, Object> data) {
        Map<String, Object> stats = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof List<?>) {
                List<?> list = (List<?>) entry.getValue();
                if (!list.isEmpty() && list.get(0) instanceof Number) {
                    DoubleSummaryStatistics summary = list.stream()
                        .map(num -> ((Number) num).doubleValue())
                        .filter(num -> !Double.isNaN(num) && !Double.isInfinite(num) && num != 0.0)
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();
                    
                    if (summary.getCount() > 0) {
                        stats.put(entry.getKey() + "_mean", summary.getAverage());
                        stats.put(entry.getKey() + "_max", summary.getMax());
                        stats.put(entry.getKey() + "_total", summary.getSum());
                    }
                }
            }
        }
        return stats;
    }

    private Map<String, Object> analyzeTrends(Map<String, List<Double>> timeSeriesData) {
        Map<String, Object> trends = new HashMap<>();
        List<Double> confirmed = timeSeriesData.get("confirmed");
        List<Double> dailyNew = timeSeriesData.get("dailyNew");
        
        trends.put("growth_rate", calculateGrowthRate(confirmed));
        trends.put("peak_daily", Collections.max(dailyNew));
        trends.put("current_trajectory", analyzeTrendDirection(dailyNew));
        
        return trends;
    }

    private Map<String, Object> analyzeMortality(Map<String, List<Double>> currentStats, 
                                               List<CovidDeath> deathStats) {
        Map<String, Object> mortality = new HashMap<>();
        
        // Calculate overall mortality rate
        double totalCases = currentStats.get("cases").stream().mapToDouble(Double::doubleValue).sum();
        double totalDeaths = currentStats.get("deaths").stream().mapToDouble(Double::doubleValue).sum();
        mortality.put("overall_rate", (totalDeaths / totalCases) * 100);
        
        // Calculate age-specific mortality
        Map<String, Double> ageGroupRates = deathStats.stream()
            .filter(d -> !d.getAgeGroup().equals("All ages"))
            .collect(Collectors.groupingBy(
                CovidDeath::getAgeGroup,
                Collectors.summingDouble(d -> Double.parseDouble(d.getNumberOfDeaths().replace(",", "")))
            ));
        mortality.put("age_group_rates", ageGroupRates);
        
        return mortality;
    }

    private String createCurrentSnapshotChart(Map<String, List<Double>> data) throws IOException {
        JFreeChart chart = ChartFactory.createBarChart(
            "Current COVID-19 Snapshot",
            "Category",
            "Count",
            createDatasetFromMap(data),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        return convertChartToBase64(chart);
    }

    private String createTimeSeriesChart(Map<String, List<Double>> data) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        for (Map.Entry<String, List<Double>> entry : data.entrySet()) {
            XYSeries series = new XYSeries(entry.getKey());
            List<Double> values = entry.getValue();
            for (int i = 0; i < values.size(); i++) {
                series.add(i, values.get(i));
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Time Series Analysis",
            "Time",
            "Count",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        return convertChartToBase64(chart);
    }

    private String createAgeDistributionChart(List<CovidDeath> deaths) throws IOException {
        JFreeChart chart = advancedVisualizer.createAgeDistributionChart(deaths);
        return convertChartToBase64(chart);
    }

    private String createGrowthRateChart(Map<String, List<Double>> data) throws IOException {
        List<Double> cases = data.get("confirmed");
        List<Double> dates = IntStream.range(0, cases.size())
            .mapToDouble(i -> (double) i)
            .boxed()
            .collect(Collectors.toList());
            
        JFreeChart chart = advancedVisualizer.createGrowthRateChart(cases, dates);
        return convertChartToBase64(chart);
    }

    private DefaultCategoryDataset createDatasetFromMap(Map<String, List<Double>> data) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, List<Double>> entry : data.entrySet()) {
            double sum = entry.getValue().stream().mapToDouble(Double::doubleValue).sum();
            dataset.addValue(sum, "Total", entry.getKey());
        }
        return dataset;
    }

    private String convertChartToBase64(JFreeChart chart) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 600, 400);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String analyzeTrendDirection(List<Double> data) {
        if (data.size() < 2) return "Insufficient data";
        
        double recent = data.get(data.size() - 1);
        double previous = data.get(data.size() - 2);
        
        if (recent > previous) return "Increasing";
        if (recent < previous) return "Decreasing";
        return "Stable";
    }

    private Map<String, Double> prepareAgeDistributionData(List<CovidDeath> deaths) {
        return deaths.stream()
            .filter(d -> !d.getAgeGroup().equals("All ages"))
            .collect(Collectors.groupingBy(
                CovidDeath::getAgeGroup,
                Collectors.summingDouble(d -> Double.parseDouble(d.getNumberOfDeaths().replace(",", "")))
            ));
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        model.addAttribute("error", "An error occurred");
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    private Map<String, Object> processCSVData(List<CSVRecord> records) {
        Map<String, Object> processedData = new HashMap<>();
        
        try {
            // Check if it's covid_grouped.csv (has 'Date' column)
            if (records.get(0).isMapped("Date")) {
                List<String> dates = new ArrayList<>();
                List<Double> confirmed = new ArrayList<>();
                List<Double> deaths = new ArrayList<>();
                List<Double> recovered = new ArrayList<>();
                List<Double> active = new ArrayList<>();
                List<Double> dailyNew = new ArrayList<>();
                List<Double> dailyDeaths = new ArrayList<>();

                // Skip header
                for (int i = 1; i < records.size(); i++) {
                    CSVRecord record = records.get(i);
                    try {
                        dates.add(record.get("Date"));
                        double confirmedCases = parseDoubleOrZero(record.get("Confirmed"));
                        double deathCases = parseDoubleOrZero(record.get("Deaths"));
                        double recoveredCases = parseDoubleOrZero(record.get("Recovered"));
                        double activeCases = parseDoubleOrZero(record.get("Active"));
                        
                        confirmed.add(confirmedCases);
                        deaths.add(deathCases);
                        recovered.add(recoveredCases);
                        active.add(activeCases);

                        // Calculate daily new cases and deaths
                        if (i > 1) {
                            double prevConfirmed = parseDoubleOrZero(records.get(i-1).get("Confirmed"));
                            double prevDeaths = parseDoubleOrZero(records.get(i-1).get("Deaths"));
                            dailyNew.add(Math.max(0, confirmedCases - prevConfirmed));
                            dailyDeaths.add(Math.max(0, deathCases - prevDeaths));
                        } else {
                            dailyNew.add(0.0);
                            dailyDeaths.add(0.0);
                        }
                    } catch (Exception e) {
                        System.out.println("Error processing row " + i + ": " + e.getMessage());
                    }
                }

                processedData.put("type", "timeSeries");
                processedData.put("dates", dates);
                processedData.put("confirmed", confirmed);
                processedData.put("deaths", deaths);
                processedData.put("recovered", recovered);
                processedData.put("active", active);
                processedData.put("dailyNew", dailyNew);
                processedData.put("dailyDeaths", dailyDeaths);
                
                System.out.println("Processed " + dates.size() + " days of time series data");
            } 
            // Process covid.csv (snapshot data)
            else {
                List<String> countries = new ArrayList<>();
                List<Double> cases = new ArrayList<>();
                List<Double> deaths = new ArrayList<>();

                System.out.println("\n=== Processing CSV Data ===");
                
                // Skip header
                for (int i = 1; i < records.size(); i++) {
                    CSVRecord record = records.get(i);
                    try {
                        String country = record.get("Country/Region").trim();
                        String casesStr = record.get("TotalCases").trim();
                        String deathsStr = record.get("TotalDeaths").trim();

                        System.out.println("\nProcessing " + country + ":");
                        System.out.println("Cases (raw): '" + casesStr + "'");
                        System.out.println("Deaths (raw): '" + deathsStr + "'");

                        // Clean and parse the values
                        casesStr = casesStr.replace(",", "");
                        deathsStr = deathsStr.replace(",", "");

                        // Handle empty strings
                        double casesVal = casesStr.isEmpty() ? 0 : Double.parseDouble(casesStr);
                        double deathsVal = deathsStr.isEmpty() ? 0 : Double.parseDouble(deathsStr);

                        // Only add if we have valid data
                        if (casesVal > 0 || deathsVal > 0) {
                            countries.add(country);
                            cases.add(casesVal);
                            deaths.add(deathsVal);
                            System.out.println("Added - Cases: " + casesVal + ", Deaths: " + deathsVal);
                        }

                    } catch (Exception e) {
                        System.out.println("Error processing row " + i + ": " + e.getMessage());
                    }
                }

                // Print totals
                double totalCases = cases.stream().mapToDouble(Double::doubleValue).sum();
                double totalDeaths = deaths.stream().mapToDouble(Double::doubleValue).sum();
                System.out.println("\n=== Final Results ===");
                System.out.println("Countries processed: " + countries.size());
                System.out.println("Total cases: " + totalCases);
                System.out.println("Total deaths: " + totalDeaths);

                processedData.put("type", "snapshot");
                processedData.put("countries", countries);
                processedData.put("cases", cases);
                processedData.put("deaths", deaths);
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error processing CSV: " + e.getMessage());
        }

        return processedData;
    }

    private double parseDoubleOrZero(String value) {
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private List<CovidDeath> readJSONFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getResourceAsStream("/static/data/covid_deaths.json");
        return mapper.readValue(inputStream, 
            mapper.getTypeFactory().constructCollectionType(List.class, CovidDeath.class));
    }

    private Map<String, Object> processJSONData(List<CovidDeath> deathStats) {
        Map<String, Object> processedData = new HashMap<>();
        
        try {
            System.out.println("\n=== Processing Death Stats ===");
            System.out.println("Total records: " + deathStats.size());
            
            // Print some sample records
            System.out.println("\nSample records:");
            deathStats.stream().limit(5).forEach(d -> 
                System.out.println("Age: " + d.getAgeGroup() + 
                                 ", Condition: " + d.getConditionGroup() + 
                                 ", Deaths: " + d.getNumberOfDeaths()));
            
            double totalDeaths = deathStats.stream()
                .filter(d -> d.getAgeGroup() != null && !d.getAgeGroup().equals("All ages"))
                .mapToDouble(d -> {
                    try {
                        return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .sum();
                
            double covidDeaths = deathStats.stream()
                .filter(d -> d.getConditionGroup() != null && 
                            d.getConditionGroup().equals("Coronavirus Disease 2019") &&
                            !d.getAgeGroup().equals("All ages"))
                .mapToDouble(d -> {
                    try {
                        return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .sum();

            System.out.println("\nCalculated values:");
            System.out.println("Total Deaths: " + totalDeaths);
            System.out.println("COVID Deaths: " + covidDeaths);
            System.out.println("COVID Impact: " + ((covidDeaths/totalDeaths) * 100) + "%");
            
            // Calculate comorbidity deaths
            double comorbidityDeaths = totalDeaths - covidDeaths;

            processedData.put("totalDeaths", totalDeaths);
            processedData.put("totalCovidDeaths", covidDeaths);
            processedData.put("comorbidityRate", (comorbidityDeaths / totalDeaths) * 100);
            
            // Group deaths by age groups
            Map<String, Double> ageGroupDeaths = deathStats.stream()
                .filter(d -> d.getAgeGroup() != null && !d.getAgeGroup().equals("All ages"))
                .collect(Collectors.groupingBy(
                    CovidDeath::getAgeGroup,
                    Collectors.summingDouble(d -> {
                        try {
                            return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                        } catch (Exception e) {
                            return 0.0;
                        }
                    })
                ));

            // Calculate percentages
            Map<String, Double> ageGroupPercentages = new HashMap<>();
            ageGroupDeaths.forEach((group, deaths) -> 
                ageGroupPercentages.put(group, (deaths / totalDeaths) * 100));

            // Group by condition
            Map<String, Double> conditionGroupDeaths = deathStats.stream()
                .filter(d -> d.getConditionGroup() != null)
                .collect(Collectors.groupingBy(
                    CovidDeath::getConditionGroup,
                    Collectors.summingDouble(d -> {
                        try {
                            return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                        } catch (Exception e) {
                            return 0.0;
                        }
                    })
                ));

            // Add all data to the result map
            processedData.put("ageGroupDeaths", ageGroupDeaths);
            processedData.put("ageGroupPercentages", ageGroupPercentages);
            processedData.put("conditionGroupDeaths", conditionGroupDeaths);
            processedData.put("analysisPeriod", deathStats.get(0).getStartWeek() + " to " + deathStats.get(0).getEndWeek());
            
            // Fix the key names to match what the template expects
            processedData.put("conditionGroups", processedData.get("conditionGroupDeaths"));
            processedData.put("ageGroups", processedData.get("ageGroupDeaths"));
            
            return processedData;
        } catch (Exception e) {
            System.err.println("Error processing data: " + e.getMessage());
            e.printStackTrace();
            return processedData;
        }
    }

    private List<Double> calculateDailyChanges(List<Double> data) {
        List<Double> changes = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            changes.add(data.get(i) - data.get(i-1));
        }
        return changes;
    }

    private List<Double> calculateGrowthRates(List<Double> data) {
        List<Double> rates = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            double previous = data.get(i-1);
            double current = data.get(i);
            double rate = previous > 0 ? ((current - previous) / previous) * 100 : 0;
            rates.add(Math.round(rate * 100.0) / 100.0); // Round to 2 decimal places
        }
        return rates;
    }

    private List<String[]> readCSVFile(String filename) {
        List<String[]> records = new ArrayList<>();
        System.out.println("Attempting to read CSV file: " + filename);
        
        try {
            InputStream is = getClass().getResourceAsStream("/" + filename);
            if (is == null) {
                System.err.println("Could not find file: /" + filename);
                return records;
            }
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int lineCount = 0;
                while ((line = br.readLine()) != null) {
                    records.add(line.split(","));
                    lineCount++;
                }
                System.out.println("Successfully read " + lineCount + " lines from " + filename);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }

    @GetMapping("/api/covid-data")
    @ResponseBody
    public Map<String, Object> getCovidData() {
        Map<String, Object> data = new HashMap<>();
        
        if (currentRecords != null && !currentRecords.isEmpty()) {
            List<Double> cases = new ArrayList<>();
            List<Double> deaths = new ArrayList<>();
            List<String> countries = new ArrayList<>();

            for (CSVRecord record : currentRecords) {
                String casesStr = record.get("TotalCases").trim();
                String deathsStr = record.get("TotalDeaths").trim();
                String country = record.get("Country/Region");
                
                if (!casesStr.isEmpty() && !deathsStr.isEmpty()) {
                    cases.add(Double.parseDouble(casesStr.replace(",", "")));
                    deaths.add(Double.parseDouble(deathsStr.replace(",", "")));
                    countries.add(country);
                }
            }
            
            data.put("cases", cases);
            data.put("deaths", deaths);
            data.put("labels", countries);
        }
        
        return data;
    }
} 