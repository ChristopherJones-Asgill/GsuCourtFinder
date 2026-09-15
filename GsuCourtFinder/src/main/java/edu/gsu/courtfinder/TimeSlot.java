package edu.gsu.courtfinder;

import java.time.LocalTime;

// Groups check-in times into parts of the day.
public enum TimeSlot {
    MORNING("Morning (6am-12pm)"),
    AFTERNOON("Afternoon (12pm-5pm)"),
    EVENING("Evening (5pm-9pm)"),
    NIGHT("Night (9pm-6am)");

    private final String label;

    TimeSlot(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TimeSlot fromTime(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 6 && hour < 12) {
            return MORNING;
        } else if (hour >= 12 && hour < 17) {
            return AFTERNOON;
        } else if (hour >= 17 && hour < 21) {
            return EVENING;
        } else {
            return NIGHT;
        }
    }
}
