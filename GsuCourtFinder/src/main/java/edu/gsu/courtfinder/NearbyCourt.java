package edu.gsu.courtfinder;

// Pairs a court with its distance from the user.
public record NearbyCourt(Court court, double distanceMiles) {
}
