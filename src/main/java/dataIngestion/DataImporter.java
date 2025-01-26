package dataIngestion;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;

public class DataImporter {
    public static List<CSVRecord> importCSV(InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().build())) {
            return csvParser.getRecords();
        }
    }

    public static List<CovidDeathRecord> importJson(String filePath) throws IOException {
        List<CovidDeathRecord> records = new ArrayList<>();
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONArray jsonArray = new JSONArray(content);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String country = obj.optString("State", "Unknown");
            int deaths = obj.optInt("Number of COVID-19 Deaths", 0);
            String date = obj.optString("Data as of", "2020-01-01");
            records.add(new CovidDeathRecord(country, date, deaths));
        }
        return records;
    }

    public static List<CSVRecord> importCSV(String filePath) throws IOException {
        try (FileInputStream fileInput = new FileInputStream(filePath)) {
            return importCSV(fileInput);
        }
    }

    public static class CovidDeathRecord {
        private final String country;
        private final String date;
        private final int deaths;

        public CovidDeathRecord(String country, String date, int deaths) {
            this.country = country;
            this.date = date;
            this.deaths = deaths;
        }

        public String getCountry() {
            return country;
        }

        public String getDate() {
            return date;
        }

        public int getDeaths() {
            return deaths;
        }
    }
}