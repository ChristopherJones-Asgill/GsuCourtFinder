package edu.gsu.courtfinder;

import java.time.DayOfWeek;
import java.time.LocalTime;

// Stores one court check-in.
public class CheckIn {

    private final String courtId;
    private final DayOfWeek dayOfWeek;
    private final LocalTime time;

    public CheckIn(String courtId, DayOfWeek dayOfWeek, LocalTime time) {
        this.courtId = courtId;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
    }

    public String getCourtId() {
        return courtId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getTime() {
        return time;
    }

    public TimeSlot getTimeSlot() {
        return TimeSlot.fromTime(time);
    }

    // Converts a check-in to one CSV line.
    public String toCsvLine() {
        return courtId + "," + dayOfWeek + "," + time;
    }

    // Creates a check-in from one CSV line.
    public static CheckIn fromCsvLine(String line) {
        String[] parts = line.split(",");
        String courtId = parts[0].trim();
        DayOfWeek day = DayOfWeek.valueOf(parts[1].trim());
        LocalTime time = LocalTime.parse(parts[2].trim());
        return new CheckIn(courtId, day, time);
    }
}
