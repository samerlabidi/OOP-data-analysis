package visualization;

import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

import model.CovidDeath;

@Component
public class AdvancedCovidVisualizer {
    
    public JFreeChart createAgeDistributionChart(List<CovidDeath> deaths) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        deaths.stream()
            .filter(d -> !d.getAgeGroup().equals("All ages"))
            .forEach(d -> dataset.addValue(
                Double.parseDouble(d.getNumberOfDeaths().replace(",", "")), 
                "Deaths", 
                d.getAgeGroup()
            ));
            
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
        
        return ChartFactory.createXYLineChart(
            "Daily Growth Rate (%)",
            "Date",
            "Growth Rate (%)",
            new XYSeriesCollection(series),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
    }

    public JFreeChart createDailyChangeChart(XYDataset baseData, String title) {
        XYSeries series = new XYSeries("Daily Change");
        XYSeriesCollection baseDataset = (XYSeriesCollection) baseData;
        XYSeries baseSeries = baseDataset.getSeries(0);
        
        for (int i = 1; i < baseSeries.getItemCount(); i++) {
            double current = baseSeries.getY(i).doubleValue();
            double previous = baseSeries.getY(i-1).doubleValue();
            series.add(i, current - previous);
        }
        
        return ChartFactory.createXYLineChart(
            title,
            "Date",
            "Daily Change",
            new XYSeriesCollection(series),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
    }

    public JFreeChart createMortalityCorrelationChart(XYDataset casesData, XYDataset deathsData) {
        XYSeries series = new XYSeries("Mortality Correlation");
        XYSeriesCollection casesDataset = (XYSeriesCollection) casesData;
        XYSeriesCollection deathsDataset = (XYSeriesCollection) deathsData;
        
        int size = Math.min(casesDataset.getSeries(0).getItemCount(), 
                           deathsDataset.getSeries(0).getItemCount());
        
        for (int i = 0; i < size; i++) {
            double cases = casesDataset.getSeries(0).getY(i).doubleValue();
            double deaths = deathsDataset.getSeries(0).getY(i).doubleValue();
            if (cases > 0) {
                series.add(cases, deaths);
            }
        }
        
        return ChartFactory.createScatterPlot(
            "Mortality vs Case Rate Correlation",
            "Cases",
            "Deaths",
            new XYSeriesCollection(series),
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
    }
} 