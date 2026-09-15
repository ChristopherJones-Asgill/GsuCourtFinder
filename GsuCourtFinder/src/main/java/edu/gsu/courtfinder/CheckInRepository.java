package edu.gsu.courtfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Loads, saves, and summarizes check-ins.
public class CheckInRepository {

    private final Path csvPath;
    private final List<CheckIn> checkIns = new ArrayList<>();

    public CheckInRepository(Path csvPath) {
        this.csvPath = csvPath;
        load();
    }

    // Loads saved check-ins from the CSV file.
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
                checkIns.add(CheckIn.fromCsvLine(line));
            }
        } catch (IOException e) {
            System.out.println("Couldn't load checkins.csv: " + e.getMessage());
        }
    }

    // Adds a check-in and saves it to the file.
    public void addCheckIn(CheckIn checkIn) {
        checkIns.add(checkIn);
        try {
            Files.writeString(csvPath, checkIn.toCsvLine() + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Couldn't save check-in: " + e.getMessage());
        }
    }

    public List<CheckIn> getCheckInsForCourt(String courtId) {
        return checkIns.stream()
                .filter(c -> c.getCourtId().equalsIgnoreCase(courtId))
                .toList();
    }

    // Counts check-ins by day and time slot.
    public Map<DayOfWeek, Map<TimeSlot, Integer>> summarizeByDayAndSlot(String courtId) {
        Map<DayOfWeek, Map<TimeSlot, Integer>> summary = new EnumMap<>(DayOfWeek.class);
        for (CheckIn checkIn : getCheckInsForCourt(courtId)) {
            Map<TimeSlot, Integer> slotCounts = summary.computeIfAbsent(
                    checkIn.getDayOfWeek(), d -> new EnumMap<>(TimeSlot.class));
            slotCounts.merge(checkIn.getTimeSlot(), 1, Integer::sum);
        }
        return summary;
    }

    // Finds the busiest day and time slot.
    public String findPeakTime(String courtId) {
        DayOfWeek bestDay = null;
        TimeSlot bestSlot = null;
        int bestCount = 0;

        for (Map.Entry<DayOfWeek, Map<TimeSlot, Integer>> dayEntry : summarizeByDayAndSlot(courtId).entrySet()) {
            for (Map.Entry<TimeSlot, Integer> slotEntry : dayEntry.getValue().entrySet()) {
                if (slotEntry.getValue() > bestCount) {
                    bestCount = slotEntry.getValue();
                    bestDay = dayEntry.getKey();
                    bestSlot = slotEntry.getKey();
                }
            }
        }

        if (bestDay == null) {
            return null;
        }
        return bestDay + " " + bestSlot.getLabel() + " (" + bestCount + " check-ins)";
    }
}
