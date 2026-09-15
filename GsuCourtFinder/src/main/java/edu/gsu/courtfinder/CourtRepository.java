package edu.gsu.courtfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Loads courts and handles court searches.
public class CourtRepository {

    private final Path csvPath;
    private final List<Court> courts = new ArrayList<>();

    public CourtRepository(Path csvPath) {
        this.csvPath = csvPath;
        load();
    }

    // Loads the courts from the CSV file.
    private void load() {
        try {
            if (!Files.exists(csvPath)) {
                return;
            }
            List<String> lines = Files.readAllLines(csvPath);
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                courts.add(Court.fromCsvLine(line));
            }
        } catch (IOException e) {
            System.out.println("Couldn't load courts.csv: " + e.getMessage());
        }
    }

    public List<Court> getAllCourts() {
        return courts;
    }

    public Court findById(String id) {
        return courts.stream()
                .filter(c -> c.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    // Finds courts and sorts them by distance.
    public List<NearbyCourt> findNearby(double userLat, double userLon) {
        List<NearbyCourt> results = new ArrayList<>();
        for (Court court : courts) {
            double dist = GeoUtils.distanceMiles(userLat, userLon, court.getLatitude(), court.getLongitude());
            results.add(new NearbyCourt(court, dist));
        }
        results.sort(Comparator.comparingDouble(NearbyCourt::distanceMiles));
        return results;
    }
}
