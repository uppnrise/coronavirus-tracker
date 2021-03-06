package com.upp.coronavirustracker.services;

import com.upp.coronavirustracker.models.LocationStats;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class CoronaVirusDataService {

    @Value("${c-tracker.virusDataUrl}")
    private String VIRUS_DATA_URL;

    private List<LocationStats> allStats = new ArrayList<>();

    public List<LocationStats> getAllStats() {
        return allStats;
    }

    @PostConstruct
    @Scheduled(cron = "${c-tracker.cron}")
    public void getVirusData() {

        List<LocationStats> newStats = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIRUS_DATA_URL))
                .build();

        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            StringReader csvBodyReader = new StringReader(httpResponse.body());
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);
            for (CSVRecord record : records) {
                LocationStats locationStat = new LocationStats();

                locationStat.setState(record.get("Province/State"));
                locationStat.setCountry(record.get("Country/Region"));

                int latestCases = Integer.parseInt(record.get(record.size() - 1));
                int prevDayCases = Integer.parseInt(record.get(record.size() - 2));

                locationStat.setLatestTotalCases(latestCases);
                locationStat.setDiffFromPrevDay(latestCases - prevDayCases);

                newStats.add(locationStat);
            }

            this.allStats = newStats;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


    }


}
