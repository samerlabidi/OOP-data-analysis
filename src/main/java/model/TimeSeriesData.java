package model;

import java.time.LocalDate;

public class TimeSeriesData {
    private final LocalDate date;
    private final double value;

    public TimeSeriesData(LocalDate date, double value) {
        this.date = date;
        this.value = value;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public double getValue() { return value; }
} 