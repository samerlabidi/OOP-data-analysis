package visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

import model.CovidDeath;

@Component
public class AdvancedCovidVisualizer {
    
    private void customizeChartAppearance(JFreeChart chart) {
        // Set background
        chart.setBackgroundPaint(Color.WHITE);
        
        // Customize plot
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(245, 245, 250));
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlinePaint(Color.DARK_GRAY);
        
        // Customize renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(41, 128, 185)); // Blue
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        plot.setRenderer(renderer);
        
        // Customize title
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));
        
        // Customize legend
        LegendTitle legend = chart.getLegend();
        legend.setBackgroundPaint(new Color(245, 245, 245, 200));
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setBorder(0, 0, 0, 0);
        
        // Customize axes
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 14));
        
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 14));
    }

    public JFreeChart createAgeDistributionChart(List<CovidDeath> deaths) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        deaths.stream()
            .filter(d -> d.getAgeGroup() != null && !d.getAgeGroup().equals("All ages"))
            .forEach(d -> {
                try {
                    String deathsStr = d.getNumberOfDeaths();
                    if (deathsStr != null && !deathsStr.trim().isEmpty()) {
                        double deathCount = Double.parseDouble(deathsStr.replace(",", ""));
                        dataset.addValue(deathCount, "Deaths", d.getAgeGroup());
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing death count for age group " + d.getAgeGroup() + ": " + e.getMessage());
                }
            });
            
        return ChartFactory.createBarChart(
            "COVID-19 Deaths by Age Group",
            "Age Group",
            "Number of Deaths",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
    }
    
    public JFreeChart createGrowthRateChart(List<Double> cases, List<Double> dates) {
        XYSeries series = new XYSeries("Growth Rate");
        
        for (int i = 1; i < cases.size(); i++) {
            double current = cases.get(i);
            double previous = cases.get(i-1);
            if (previous > 0) {
                double growthRate = ((current - previous) / previous) * 100;
                series.add(Double.valueOf(dates.get(i)), Double.valueOf(growthRate));
            }
        }
        
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Daily Growth Rate (%)",
            "Date",
            "Growth Rate (%)",
            new XYSeriesCollection(series),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        customizeChartAppearance(chart);
        return chart;
    }

    public JFreeChart createDailyChangeChart(XYDataset baseData, String title) {
        System.out.println("Creating daily change chart: " + title);
        
        if (baseData == null) {
            System.err.println("baseData is null!");
            return null;
        }
        
        XYSeriesCollection baseDataset = (XYSeriesCollection) baseData;
        if (baseDataset.getSeriesCount() == 0) {
            System.err.println("baseDataset has no series!");
            return null;
        }
        
        XYSeries baseSeries = baseDataset.getSeries(0);
        System.out.println("Base series item count: " + baseSeries.getItemCount());
        
        XYSeries series = new XYSeries("Daily Change");
        
        for (int i = 1; i < baseSeries.getItemCount(); i++) {
            double current = baseSeries.getY(i).doubleValue();
            double previous = baseSeries.getY(i-1).doubleValue();
            series.add(i, current - previous);
        }
        
        System.out.println("Created daily change series with " + series.getItemCount() + " items");
        
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            "Date",
            "Daily Change",
            new XYSeriesCollection(series),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        customizeChartAppearance(chart);
        return chart;
    }

    public JFreeChart createMortalityCorrelationChart(XYDataset casesData, XYDataset deathsData) {
        System.out.println("Creating mortality correlation chart...");
        
        if (casesData == null || deathsData == null) {
            System.err.println("Cases or deaths data is null");
            return null;
        }
        
        XYSeriesCollection casesDataset = (XYSeriesCollection) casesData;
        XYSeriesCollection deathsDataset = (XYSeriesCollection) deathsData;
        
        if (casesDataset.getSeriesCount() == 0 || deathsDataset.getSeriesCount() == 0) {
            System.err.println("Cases or deaths dataset has no series");
            return null;
        }
        
        XYSeries series = new XYSeries("Mortality Correlation");
        
        int size = Math.min(casesDataset.getSeries(0).getItemCount(), 
                           deathsDataset.getSeries(0).getItemCount());
                           
        System.out.println("Processing " + size + " data points");
        
        int validPoints = 0;
        for (int i = 0; i < size; i++) {
            try {
                double cases = casesDataset.getSeries(0).getY(i).doubleValue();
                double deaths = deathsDataset.getSeries(0).getY(i).doubleValue();
                if (cases > 0) {
                    series.add(cases, deaths);
                    validPoints++;
                }
            } catch (Exception e) {
                System.err.println("Error processing point at index " + i + ": " + e.getMessage());
            }
        }
        
        System.out.println("Added " + validPoints + " valid points to correlation chart");
        
        if (validPoints == 0) {
            System.err.println("No valid points for correlation chart");
            return null;
        }
        
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Mortality vs Case Rate Correlation",
            "Cases",
            "Deaths",
            new XYSeriesCollection(series),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        customizeChartAppearance(chart);
        return chart;
    }
} 