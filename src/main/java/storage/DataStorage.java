package storage;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.commons.csv.CSVRecord;

import dataIngestion.DataImporter;

public class DataStorage {
    private final Connection connection;

    public DataStorage(String dbUrl, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(dbUrl, user, password);
    }

    public void insertData(List<CSVRecord> cleanedData) throws SQLException {
        String insertSQL = "INSERT INTO covid_statistics (country, total_cases, total_deaths) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            for (CSVRecord record : cleanedData) {
                stmt.setString(1, record.get("Country/Region"));
                stmt.setInt(2, parseIntWithDefault(record.get("TotalCases"), 0));
                stmt.setInt(3, parseIntWithDefault(record.get("TotalDeaths"), 0));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public void insertGroupedData(List<CSVRecord> cleanedData) throws SQLException {
        String insertSQL = "INSERT INTO covid_grouped (country, date, confirmed) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            for (CSVRecord record : cleanedData) {
                stmt.setString(1, record.get("Country/Region"));
                stmt.setDate(2, Date.valueOf(record.get("Date")));
                stmt.setInt(3, parseIntWithDefault(record.get("Confirmed"), 0));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public void insertJsonData(List<DataImporter.CovidDeathRecord> cleanedJsonData) throws SQLException {
        String insertSQL = "INSERT INTO covid_deaths (country, date, deaths) VALUES (?, ?, ?)";
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");  // Matches JSON format
    
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            for (DataImporter.CovidDeathRecord record : cleanedJsonData) {
                stmt.setString(1, record.getCountry());
    
                try {
                    // Parse and convert the date
                    LocalDate localDate = LocalDate.parse(record.getDate(), inputFormatter);
                    Date sqlDate = Date.valueOf(localDate);
                    stmt.setDate(2, sqlDate);
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid date format for record: " + record.getDate());
                    stmt.setDate(2, null);  // Set to null if date parsing fails
                }
    
                stmt.setInt(3, record.getDeaths());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private int parseIntWithDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
