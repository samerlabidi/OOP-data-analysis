package analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"controller", "analysis", "visualization", "dataIngestion", "model"})
public class CovidAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(CovidAnalysisApplication.class, args);
    }
} 