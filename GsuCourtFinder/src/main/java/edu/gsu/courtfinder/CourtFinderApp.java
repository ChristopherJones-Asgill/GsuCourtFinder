package edu.gsu.courtfinder;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// Console version of the court finder.
public class CourtFinderApp {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private static final Path COURTS_FILE = Path.of("data/courts.csv");
    private static final Path CHECKINS_FILE = Path.of("data/checkins.csv");

    public static void main(String[] args) {
        CourtRepository courtRepo = new CourtRepository(COURTS_FILE);
        CheckInRepository checkInRepo = new CheckInRepository(CHECKINS_FILE);
        Scanner scanner = new Scanner(System.in);

        System.out.println("========================================");
        System.out.println(" GSU Court Finder - Statesboro pickup ball");
        System.out.println("========================================");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> listAllCourts(courtRepo);
                case "2" -> findNearbyCourts(courtRepo, scanner);
                case "3" -> checkIn(courtRepo, checkInRepo, scanner);
                case "4" -> showPopularTimes(courtRepo, checkInRepo, scanner);
                case "5" -> {
                    System.out.println("Catch you on the court.");
                    running = false;
                }
                default -> System.out.println("Not a valid option, try again.");
            }
        }
        scanner.close();
    }

    // Displays the main menu.
    private static void printMenu() {
        System.out.println();
        System.out.println("1. List all courts");
        System.out.println("2. Find nearby courts");
        System.out.println("3. Check in at a court (log that you played)");
        System.out.println("4. See when a court is usually busy");
        System.out.println("5. Exit");
        System.out.print("Choose an option: ");
    }

    private static void listAllCourts(CourtRepository courtRepo) {
        System.out.println();
        List<Court> courts = courtRepo.getAllCourts();
        if (courts.isEmpty()) {
            System.out.println("No courts loaded. Check data/courts.csv.");
            return;
        }
        for (Court court : courts) {
            System.out.printf("  [%s] %s%n", court.getId(), court.getName());
        }
    }

    // Gets a location and displays nearby courts.
    private static void findNearbyCourts(CourtRepository courtRepo, Scanner scanner) {
        System.out.println();
        System.out.println("Enter your current location (lat, lon).");
        System.out.println("Tip: you can get this from Google Maps by long-pressing your spot.");
        System.out.println("Or just hit enter to use the GSU Russell Union as a stand-in.");
        System.out.print("Latitude: ");
        String latInput = scanner.nextLine().trim();
        System.out.print("Longitude: ");
        String lonInput = scanner.nextLine().trim();

        double lat;
        double lon;
        try {
            if (latInput.isEmpty() || lonInput.isEmpty()) {
                lat = 32.4237; // Default campus location
                lon = -81.7840;
            } else {
                lat = Double.parseDouble(latInput);
                lon = Double.parseDouble(lonInput);
            }
        } catch (NumberFormatException e) {
            System.out.println("That didn't look like a number, using GSU Russell Union instead.");
            lat = 32.4237;
            lon = -81.7840;
        }

        List<NearbyCourt> nearby = courtRepo.findNearby(lat, lon);
        System.out.println();
        System.out.println("Closest courts to you:");
        for (NearbyCourt nc : nearby) {
            System.out.printf("  [%s] %-28s %.2f miles away%n",
                    nc.court().getId(), nc.court().getName(), nc.distanceMiles());
        }
    }

    // Saves a new court check-in.
    private static void checkIn(CourtRepository courtRepo, CheckInRepository checkInRepo, Scanner scanner) {
        System.out.println();
        listAllCourts(courtRepo);
        System.out.print("Enter the court ID you played at: ");
        String courtId = scanner.nextLine().trim();
        Court court = courtRepo.findById(courtId);
        if (court == null) {
            System.out.println("Don't recognize that court ID.");
            return;
        }

        System.out.println("What day did you play? (MONDAY, TUESDAY, ... or leave blank for today)");
        System.out.print("Day: ");
        String dayInput = scanner.nextLine().trim();
        DayOfWeek day;
        try {
            day = dayInput.isEmpty() ? java.time.LocalDate.now().getDayOfWeek() : DayOfWeek.valueOf(dayInput.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Didn't recognize that day, using today instead.");
            day = java.time.LocalDate.now().getDayOfWeek();
        }

        System.out.print("What time (e.g. 6:30 PM), or blank for now: ");
        String timeInput = scanner.nextLine().trim();
        LocalTime time;
        try {
            time = timeInput.isEmpty() ? LocalTime.now().withSecond(0).withNano(0) : LocalTime.parse(timeInput.toUpperCase(), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            System.out.println("Didn't recognize that time, using right now instead.");
            time = LocalTime.now().withSecond(0).withNano(0);
        }

        checkInRepo.addCheckIn(new CheckIn(court.getId(), day, time));
        System.out.printf("Logged: you played at %s on %s around %s.%n", court.getName(), day, time.format(TIME_FORMAT));
    }

    // Displays popular times for a selected court.
    private static void showPopularTimes(CourtRepository courtRepo, CheckInRepository checkInRepo, Scanner scanner) {
        System.out.println();
        listAllCourts(courtRepo);
        System.out.print("Enter the court ID to check: ");
        String courtId = scanner.nextLine().trim();
        Court court = courtRepo.findById(courtId);
        if (court == null) {
            System.out.println("Don't recognize that court ID.");
            return;
        }

        Map<DayOfWeek, Map<TimeSlot, Integer>> summary = checkInRepo.summarizeByDayAndSlot(court.getId());
        if (summary.isEmpty()) {
            System.out.println("No check-ins logged for " + court.getName() + " yet. Be the first!");
            return;
        }

        System.out.println();
        System.out.println("Check-in history for " + court.getName() + ":");
        for (DayOfWeek day : DayOfWeek.values()) {
            Map<TimeSlot, Integer> slots = summary.get(day);
            if (slots == null) {
                continue;
            }
            System.out.println("  " + day + ":");
            for (TimeSlot slot : TimeSlot.values()) {
                Integer count = slots.get(slot);
                if (count != null) {
                    System.out.printf("    %-22s %d check-in%s%n", slot.getLabel(), count, count == 1 ? "" : "s");
                }
            }
        }

        String peak = checkInRepo.findPeakTime(court.getId());
        System.out.println();
        System.out.println("Busiest time overall: " + peak);
    }
}
