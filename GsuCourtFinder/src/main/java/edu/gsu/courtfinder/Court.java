package edu.gsu.courtfinder;

// Stores information about one basketball court.
public class Court {

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;

    public Court(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    // Converts a court to one CSV line.
    public String toCsvLine() {
        return id + "," + name + "," + latitude + "," + longitude;
    }

    // Creates a court from one CSV line.
    public static Court fromCsvLine(String line) {
        String[] parts = line.split(",");
        String id = parts[0].trim();
        String name = parts[1].trim();
        double lat = Double.parseDouble(parts[2].trim());
        double lon = Double.parseDouble(parts[3].trim());
        return new Court(id, name, lat, lon);
    }

    @Override
    public String toString() {
        return name;
    }
}
