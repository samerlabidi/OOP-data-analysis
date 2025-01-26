package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CovidDeath {
    @JsonProperty("Data as of")
    private String dataAsOf;
    
    @JsonProperty("Start Week")
    private String startWeek;
    
    @JsonProperty("End Week")
    private String endWeek;
    
    @JsonProperty("State")
    private String state;
    
    @JsonProperty("Age Group")
    private String ageGroup;
    
    @JsonProperty("Condition Group")
    private String conditionGroup;
    
    @JsonProperty("Number of COVID-19 Deaths")
    private String numberOfDeaths;

    public String getDataAsOf() { return dataAsOf; }
    public String getStartWeek() { return startWeek; }
    public String getEndWeek() { return endWeek; }
    public String getState() { return state; }
    public String getAgeGroup() { return ageGroup; }
    public String getConditionGroup() { return conditionGroup; }
    public String getNumberOfDeaths() { return numberOfDeaths; }

    public void setDataAsOf(String dataAsOf) { this.dataAsOf = dataAsOf; }
    public void setStartWeek(String startWeek) { this.startWeek = startWeek; }
    public void setEndWeek(String endWeek) { this.endWeek = endWeek; }
    public void setState(String state) { this.state = state; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    public void setConditionGroup(String conditionGroup) { this.conditionGroup = conditionGroup; }
    public void setNumberOfDeaths(String numberOfDeaths) { this.numberOfDeaths = numberOfDeaths; }
} 