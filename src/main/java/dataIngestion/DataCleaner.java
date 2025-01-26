package dataIngestion;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVRecord;

public class DataCleaner {
    public static List<CSVRecord> cleanCSVData(List<CSVRecord> rawData) {
        List<CSVRecord> cleanedData = new ArrayList<>();
        for (CSVRecord record : rawData) {
            if (record != null && record.size() > 0 && record.size() > 1) {
                cleanedData.add(record);
            }
        }
        return cleanedData;
    }

    public static List<DataImporter.CovidDeathRecord> cleanJsonData(List<DataImporter.CovidDeathRecord> rawData) {
        List<DataImporter.CovidDeathRecord> cleanedData = new ArrayList<>();
        for (DataImporter.CovidDeathRecord record : rawData) {
            if (record.getDeaths() > 0) {
                cleanedData.add(record);
            }
        }
        return cleanedData;
    }
}
