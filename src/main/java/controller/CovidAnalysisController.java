package controller;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import analysis.CovidDataAnalyzer;
import dataIngestion.DataImporter;
import model.CovidDeath;
import model.DescriptiveStats;
import model.RegressionModel;
import visualization.AdvancedCovidVisualizer;
import visualization.CovidDataVisualizer;

@Controller
public class CovidAnalysisController {
    private final CovidDataAnalyzer analyzer;
    private final CovidDataVisualizer visualizer;
    private final AdvancedCovidVisualizer advancedVisualizer;
    private List<CSVRecord> currentRecords;
    private Map<String, List<Double>> dataMap;

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

            if ("json".equals(fileType)) {
                List<CovidDeath> deathStats = processDeathData(file.getInputStream());
                Map<String, Object> chartData = new HashMap<>();
                
                // Existing calculations
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
                
                double totalDeaths = ageGroups.values().stream().mapToDouble(Double::doubleValue).sum();
                
                // New insights calculations
                
                // 1. Top 5 conditions by death count
                Map<String, Double> top5Conditions = deathStats.stream()
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
                    ))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ));

                // 2. Age group mortality rate (deaths per 100k population)
                // Note: This would require population data, using percentages instead
                Map<String, Double> ageGroupPercentages = new HashMap<>();
                ageGroups.forEach((group, deaths) -> 
                    ageGroupPercentages.put(group, (deaths / totalDeaths) * 100));

                // Debug print some sample dates
                System.out.println("Sample date format: " + 
                    deathStats.stream()
                        .map(CovidDeath::getStartWeek)
                        .findFirst()
                        .orElse("No date found"));
                
                // Improved monthly trend calculation with date parsing
                TreeMap<String, Double> monthlyTrend = deathStats.stream()
                    .filter(d -> d.getStartWeek() != null && !d.getStartWeek().isEmpty())
                    .collect(Collectors.groupingBy(
                        d -> {
                            try {
                                String[] dateParts = d.getStartWeek().split("/");
                                String month = dateParts[0];
                                String year = "20" + dateParts[2]; // Assuming 2-digit year
                                return String.format("%s-%02d", year, Integer.parseInt(month));
                            } catch (Exception e) {
                                return "Unknown";
                            }
                        },
                        TreeMap::new,
                        Collectors.summingDouble(d -> {
                            try {
                                return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                            } catch (Exception e) {
                                return 0.0;
                            }
                        })
                    ));

                // Remove any invalid entries
                monthlyTrend.remove("Unknown");
                
                System.out.println("Monthly trend data points: " + monthlyTrend.size());
                monthlyTrend.forEach((month, deaths) -> 
                    System.out.println(month + ": " + deaths));
                
                chartData.put("monthlyTrend", monthlyTrend);
                
                // Calculate age group mortality comparison
                Map<String, Double> ageGroupComparison = deathStats.stream()
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

                // Sort by death count and get top 10
                Map<String, Double> top10AgeGroups = ageGroupComparison.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ));

                chartData.put("ageGroupComparison", top10AgeGroups);
                
                // 4. Calculate comorbidity statistics
                double covidOnlyDeaths = deathStats.stream()
                    .filter(d -> "Coronavirus Disease 2019".equals(d.getConditionGroup()))
                    .mapToDouble(d -> {
                        try {
                            return Double.parseDouble(d.getNumberOfDeaths().replace(",", ""));
                        } catch (Exception e) {
                            return 0.0;
                        }
                    })
                    .sum();

                // Add all data to chartData
                chartData.putAll(Map.of(
                    "ageGroups", ageGroups,
                    "ageGroupPercentages", ageGroupPercentages,
                    "conditionGroups", deathStats.stream()
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
                        )),
                    "totalDeaths", totalDeaths,
                    "totalCovidDeaths", covidOnlyDeaths,
                    "analysisPeriod", deathStats.stream()
                        .map(CovidDeath::getStartWeek)
                        .min(String::compareTo)
                        .orElse("Unknown") + " to " + 
                        deathStats.stream()
                        .map(CovidDeath::getEndWeek)
                        .max(String::compareTo)
                        .orElse("Unknown"),
                    "top5Conditions", top5Conditions
                ));
                
                model.addAttribute("chartData", chartData);
                return "json-analysis";
            }
            
            // ... rest of the code for CSV handling
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error processing file: " + e.getMessage());
            return "error";
        }
        return "index";
    }

    @PostMapping("/upload-multiple")
    public String uploadFiles(
        @RequestParam("currentData") MultipartFile currentData,
        @RequestParam("timeSeriesData") MultipartFile timeSeriesData,
        @RequestParam("deathData") MultipartFile deathData,
        Model model) {
        try {
            // Process data
            List<CSVRecord> currentRecords = DataImporter.importCSV(currentData.getInputStream());
            Map<String, List<Double>> currentStats = processCurrentData(currentRecords);
            
            List<CSVRecord> timeSeriesRecords = DataImporter.importCSV(timeSeriesData.getInputStream());
            Map<String, List<Double>> timeSeriesStats = processTimeSeriesData(timeSeriesRecords);
            
            // Calculate global statistics
            Map<String, Double> globalStats = new HashMap<>();
            List<Double> cases = timeSeriesStats.get("confirmed");
            List<Double> deaths = timeSeriesStats.get("deaths");
            
            globalStats.put("Total Cases", cases.get(cases.size() - 1));
            globalStats.put("Total Deaths", deaths.get(deaths.size() - 1));
            globalStats.put("Growth Rate", Math.round(calculateGrowthRate(cases) * 100.0) / 100.0);
            globalStats.put("Mortality Rate", Math.round((deaths.get(deaths.size() - 1) / cases.get(cases.size() - 1) * 100) * 100.0) / 100.0);
            
            // Calculate daily new cases and deaths
            List<Double> dailyNewCases = calculateDailyNew(cases);
            List<Double> dailyNewDeaths = calculateDailyNew(deaths);
            
            // Round daily growth rates
            List<Double> roundedGrowthRates = calculateDailyGrowthRates(cases).stream()
                .map(rate -> Math.round(rate * 100.0) / 100.0)
                .collect(Collectors.toList());
            
            model.addAttribute("globalStats", globalStats);
            model.addAttribute("casesData", cases);
            model.addAttribute("deathsData", deaths);
            model.addAttribute("dailyNewCases", dailyNewCases);
            model.addAttribute("dailyNewDeaths", dailyNewDeaths);
            model.addAttribute("growthRateData", roundedGrowthRates);

            return "comprehensive-analysis";
        } catch (Exception e) {
            model.addAttribute("error", "Error processing files: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/analysis")
    public String showAnalysis(Model model) {
        if (currentRecords == null || currentRecords.isEmpty()) {
            model.addAttribute("error", "No data available. Please upload a file first.");
            return "index";
        }

        // Basic statistics
        DescriptiveStats casesStats = analyzer.calculateDescriptiveStats(dataMap.get("cases"));
        model.addAttribute("casesStats", casesStats);

        // Correlation analysis
        double correlation = analyzer.calculateCorrelation(
            dataMap.get("cases"),
            dataMap.get("deaths")
        );
        model.addAttribute("correlation", correlation);

        // Regression analysis
        RegressionModel regression = analyzer.performRegression(
            dataMap.get("cases"),
            dataMap.get("deaths")
        );
        model.addAttribute("regression", regression);

        // Charts
        try {
            model.addAttribute("casesHistogram", generateChart("histogram"));
            model.addAttribute("scatterPlot", generateChart("scatter"));
            model.addAttribute("timeSeriesPlot", generateChart("timeseries"));
        } catch (IOException e) {
            model.addAttribute("chartError", "Failed to generate charts: " + e.getMessage());
        }

        // Add map data
        Map<String, Object> mapData = new HashMap<>();
        
        // Example coordinates for some countries
        Map<String, double[]> coordinates = new HashMap<>();
        coordinates.put("USA", new double[]{37.0902, -95.7129});
        coordinates.put("Brazil", new double[]{-14.2350, -51.9253});
        coordinates.put("India", new double[]{20.5937, 78.9629});

        CSVRecord header = currentRecords.get(0);
        String casesColumn = header.isMapped("Confirmed") ? "Confirmed" : "TotalCases";
        String deathsColumn = header.isMapped("Deaths") ? "Deaths" : "TotalDeaths";

        for (CSVRecord record : currentRecords) {
            String country = record.get("Country/Region");
            if (coordinates.containsKey(country)) {
                Map<String, Object> countryData = new HashMap<>();
                countryData.put("coordinates", coordinates.get(country));
                try {
                    String casesValue = record.get(casesColumn);
                    String deathsValue = record.get(deathsColumn);
                    countryData.put("cases", Double.parseDouble(casesValue.isEmpty() ? "0" : casesValue));
                    countryData.put("deaths", Double.parseDouble(deathsValue.isEmpty() ? "0" : deathsValue));
                    mapData.put(country, countryData);
                } catch (IllegalArgumentException e) {
                    continue; // Skip this record if parsing fails
                }
            }
        }
        
        model.addAttribute("mapData", mapData);
        return "analysis";
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

    private void processJsonData(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<CovidDeath> deaths = mapper.readValue(inputStream, 
            mapper.getTypeFactory().constructCollectionType(List.class, CovidDeath.class));

        if (deaths == null || deaths.isEmpty()) {
            throw new IllegalArgumentException("No valid data found in the JSON file");
        }

        // Group deaths by condition and age group
        Map<String, List<CovidDeath>> byCondition = deaths.stream()
            .collect(Collectors.groupingBy(CovidDeath::getConditionGroup));

        // For visualization, we'll use the first condition's age distribution
        List<CovidDeath> firstConditionData = byCondition.values().iterator().next();

        dataMap.put("cases", firstConditionData.stream()
            .filter(d -> !d.getAgeGroup().equals("All ages"))
            .map(CovidDeath::getNumberOfDeaths)
            .map(Double::valueOf)
            .collect(Collectors.toList()));

        // For deaths, we'll use the same data
        dataMap.put("deaths", firstConditionData.stream()
            .filter(d -> !d.getAgeGroup().equals("All ages"))
            .map(CovidDeath::getNumberOfDeaths)
            .map(Double::valueOf)
            .collect(Collectors.toList()));

        // For dates, we'll use indices since this is a snapshot
        dataMap.put("dates", firstConditionData.stream()
            .filter(d -> !d.getAgeGroup().equals("All ages"))
            .map(firstConditionData::indexOf)
            .map(Double::valueOf)
            .collect(Collectors.toList()));
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

    private Map<String, List<Double>> processCurrentData(List<CSVRecord> records) {
        Map<String, List<Double>> stats = new HashMap<>();
        stats.put("cases", records.stream()
            .map(record -> record.get("TotalCases"))
            .filter(value -> !value.isEmpty())
            .map(Double::parseDouble)
            .collect(Collectors.toList()));
        stats.put("deaths", records.stream()
            .map(record -> record.get("TotalDeaths"))
            .filter(value -> !value.isEmpty())
            .map(Double::parseDouble)
            .collect(Collectors.toList()));
        return stats;
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
            if (value == null || value.trim().isEmpty()) {
                return 0.0;
            }
            // Remove any commas and spaces
            value = value.replaceAll(",", "").trim();
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.out.println("Error parsing value '" + value + "': " + e.getMessage());
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
            System.out.println("Processing " + deathStats.size() + " records");
            
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

            // Calculate total deaths
            double totalDeaths = ageGroupDeaths.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

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
            processedData.put("totalDeaths", totalDeaths);
            
            if (!deathStats.isEmpty()) {
                processedData.put("timeRange", deathStats.get(0).getStartWeek() + " to " + deathStats.get(0).getEndWeek());
            }

            System.out.println("Processed data structure: " + processedData);
            
        } catch (Exception e) {
            System.err.println("Error processing data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return processedData;
    }

    @GetMapping("/comprehensive")
    public String showComprehensiveAnalysis(Model model) {
        System.out.println("=====================");
        System.out.println("ENDPOINT HIT: /comprehensive");
        System.out.println("=====================");
        
        try {
            System.out.println("\n=== Starting Comprehensive Analysis ===");
            List<CovidDeath> deathStats = readJSONFile();
            System.out.println("Death stats loaded: " + (deathStats != null ? deathStats.size() : "null") + " records");
            
            try {
                // Create and log base charts
                System.out.println("\nCreating base charts...");
                JFreeChart casesChart = visualizer.createLineChart(
                    "Cases Over Time", "Date", "Cases", createCasesDataset());
                JFreeChart deathsChart = visualizer.createLineChart(
                    "Deaths Over Time", "Date", "Deaths", createDeathsDataset());
                
                // Log base64 conversion
                String casesBase64 = chartToBase64(casesChart);
                String deathsBase64 = chartToBase64(deathsChart);
                System.out.println("Base charts created and converted: " + 
                    (casesBase64 != null && deathsBase64 != null));
                
                model.addAttribute("casesChart", casesBase64);
                model.addAttribute("deathsChart", deathsBase64);
                
                // Create and log advanced charts
                System.out.println("\nCreating advanced charts...");
                
                // Daily New Cases
                System.out.println("Creating daily new cases chart...");
                JFreeChart dailyNewCasesChart = advancedVisualizer.createDailyChangeChart(
                    createCasesDataset(), "Daily New Cases");
                String dailyNewCasesBase64 = chartToBase64(dailyNewCasesChart);
                System.out.println("Daily new cases chart created: " + (dailyNewCasesBase64 != null));
                model.addAttribute("dailyNewCasesChart", dailyNewCasesBase64);
                
                // Daily New Deaths
                System.out.println("Creating daily new deaths chart...");
                JFreeChart dailyNewDeathsChart = advancedVisualizer.createDailyChangeChart(
                    createDeathsDataset(), "Daily New Deaths");
                String dailyNewDeathsBase64 = chartToBase64(dailyNewDeathsChart);
                System.out.println("Daily new deaths chart created: " + (dailyNewDeathsBase64 != null));
                model.addAttribute("dailyNewDeathsChart", dailyNewDeathsBase64);
                
                // Growth Rate
                System.out.println("Creating growth rate chart...");
                List<Double> cases = extractCases();
                List<Double> dates = extractDates();
                System.out.println("Data extracted - Cases: " + cases.size() + ", Dates: " + dates.size());
                JFreeChart growthRateChart = advancedVisualizer.createGrowthRateChart(cases, dates);
                String growthRateBase64 = chartToBase64(growthRateChart);
                System.out.println("Growth rate chart created: " + (growthRateBase64 != null));
                model.addAttribute("growthRateChart", growthRateBase64);
                
                // Mortality Correlation
                System.out.println("Creating mortality correlation chart...");
                JFreeChart mortalityCorrelationChart = advancedVisualizer.createMortalityCorrelationChart(
                    createCasesDataset(), createDeathsDataset());
                String correlationBase64 = chartToBase64(mortalityCorrelationChart);
                System.out.println("Mortality correlation chart created: " + (correlationBase64 != null));
                model.addAttribute("correlationChart", correlationBase64);
                
                // Age Distribution
                System.out.println("Creating age distribution chart...");
                JFreeChart ageDistributionChart = advancedVisualizer.createAgeDistributionChart(deathStats);
                String ageDistributionBase64 = chartToBase64(ageDistributionChart);
                System.out.println("Age distribution chart created: " + (ageDistributionBase64 != null));
                model.addAttribute("ageImpactChart", ageDistributionBase64);
                
                System.out.println("\n=== Comprehensive Analysis Complete ===");
                
            } catch (Exception e) {
                System.err.println("Error in chart creation: " + e.getMessage());
                e.printStackTrace();
            }
            
            return "comprehensive-analysis";
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error processing data: " + e.getMessage());
            return "error";
        }
    }

    private String chartToBase64(JFreeChart chart) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 800, 400);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private XYDataset createCasesDataset() {
        XYSeries series = new XYSeries("Cases");
        List<String[]> csvData = readCSVFile("covid_grouped.csv");
        
        for (int i = 0; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            series.add(i, parseDoubleOrZero(row[2])); // Confirmed cases are in column 2
        }
        
        return new XYSeriesCollection(series);
    }

    private XYDataset createDeathsDataset() {
        XYSeries series = new XYSeries("Deaths");
        List<String[]> csvData = readCSVFile("covid_grouped.csv");
        
        for (int i = 0; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            series.add(i, parseDoubleOrZero(row[3])); // Deaths are in column 3
        }
        
        return new XYSeriesCollection(series);
    }

    private List<Double> extractCases() {
        List<String[]> csvData = readCSVFile("covid_grouped.csv");
        return csvData.stream()
            .map(row -> parseDoubleOrZero(row[2])) // Confirmed cases column
            .collect(Collectors.toList());
    }

    private List<Double> extractDates() {
        List<String[]> csvData = readCSVFile("covid_grouped.csv");
        return IntStream.range(0, csvData.size())
            .mapToDouble(i -> (double) i)
            .boxed()
            .collect(Collectors.toList());
    }

    private List<String[]> readCSVFile(String filename) {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/static/data/" + filename)))) {
            String line;
            while ((line = br.readLine()) != null) {
                records.add(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }
} 