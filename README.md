# COVID-19 Data Analysis System

## Project Overview
A comprehensive system for analyzing and visualizing COVID-19 data. The application processes both CSV and JSON data formats, providing interactive visualizations and statistical analysis of COVID-19 cases, deaths, and related metrics.

## Features
- Data Import & Processing
  - CSV file processing for case/death data
  - JSON processing for detailed mortality statistics
  - Data validation and cleaning

- Data Analysis
  - Time series analysis
  - Growth rate calculations
  - Age distribution analysis
  - Mortality correlation studies

- Visualization
  - Interactive charts using Chart.js
  - Multiple visualization types:
    - Line charts for time series
    - Bar charts for distributions
    - Scatter plots for correlations
  - Responsive design

- Data Storage
  - PostgreSQL database integration
  - Efficient batch processing
  - Data integrity validation

## Technology Stack
- Backend: Java Spring Boot
- Frontend: HTML5, CSS3, JavaScript
- Database: PostgreSQL
- Libraries:
  - Chart.js for visualization
  - Apache Commons CSV for data processing
  - JDBC for database operations

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   ├── controller/
│   │   │   └── CovidAnalysisController.java
│   │   ├── storage/
│   │   │   └── DataStorage.java
│   │   └── dataIngestion/
│   │       └── DataImporter.java
│   └── resources/
│       └── templates/
│           ├── analysis.html
│           ├── comprehensive-analysis.html
│           ├── csv-analysis.html
│           └── json-analysis.html
```
## Installation & Setup
1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/covid19-data-analysis.git
   ```

2. Configure PostgreSQL database
   ```sql
   CREATE DATABASE covid_analysis;
   ```

3. Update application.properties with your database credentials

4. Build the project
   ```bash
   mvn clean install
   ```

5. Run the application
   ```bash
   mvn spring-boot:run
   ```

## Usage
1. Access the application at `http://localhost:8080`
2. Upload COVID-19 data files (CSV or JSON format)
3. View generated visualizations and analysis
4. Export or save results as needed

## Object-Oriented Design
The project implements core OOP principles:
- **Encapsulation**: Data and methods are encapsulated within appropriate classes
- **Inheritance**: Utilizes Spring framework inheritance
- **Polymorphism**: Different chart types and data processing methods
- **Abstraction**: High-level interfaces for data operations


