package edu.gsu.courtfinder;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

// JavaFX version of the GSU Court Finder.
public class CourtFinderFxApp extends Application {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private static final Path COURTS_FILE = Path.of("data/courts.csv");
    private static final Path CHECKINS_FILE = Path.of("data/checkins.csv");

    private final CourtRepository courtRepo = new CourtRepository(COURTS_FILE);
    private final CheckInRepository checkInRepo = new CheckInRepository(CHECKINS_FILE);

    @Override
    public void start(Stage stage) {
        VBox header = buildHeader();

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Courts", buildCourtsTab()),
                new Tab("Find Nearby", buildNearbyTab()),
                new Tab("Check In", buildCheckInTab()),
                new Tab("Popular Times", buildPopularTimesTab())
        );
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox root = new VBox(header, tabs);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 940, 700);
        scene.getStylesheets().add(
                getClass().getResource("/edu/gsu/courtfinder/court-finder.css").toExternalForm()
        );

        stage.setTitle("GSU Court Finder");
        stage.setMinWidth(820);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    // Builds the title and navigation area.
    private VBox buildHeader() {
        Label title = new Label("GSU Court Finder");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Find courts • Check in • Discover popular times");
        subtitle.getStyleClass().add("app-subtitle");

        VBox text = new VBox(3, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label basketball = new Label("🏀");
        basketball.getStyleClass().add("basketball-icon");

        HBox row = new HBox(16, text, spacer, basketball);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(row);
        header.getStyleClass().add("app-header");
        return header;
    }
    // Builds the page that lists all courts.
    private VBox buildCourtsTab() {
        Label title = pageTitle("Courts");
        Label subtitle = pageSubtitle("Browse courts around Georgia Southern and the Statesboro area.");

        VBox rows = new VBox(0);
        List<Court> courts = courtRepo.getAllCourts();
        for (int i = 0; i < courts.size(); i++) {
            rows.getChildren().add(buildCourtRow(courts.get(i), i < courts.size() - 1));
        }

        Label count = new Label(courts.size() + " courts available");
        count.getStyleClass().add("section-caption");

        VBox card = new VBox(8, count, rows);
        card.getStyleClass().add("card");

        VBox page = new VBox(8, title, subtitle, card);
        page.getStyleClass().add("page");
        return page;
    }

    private VBox buildCourtRow(Court court, boolean showDivider) {
        Label name = new Label(court.getName());
        name.getStyleClass().add("court-name");

        Label type = new Label("Basketball court");
        type.getStyleClass().add("court-meta");

        VBox text = new VBox(3, name, type);

        HBox row = new HBox(text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("court-row");

        VBox wrapper = new VBox(row);
        if (showDivider) {
            Region divider = new Region();
            divider.getStyleClass().add("row-divider");
            wrapper.getChildren().add(divider);
        }
        return wrapper;
    }
    // Builds the nearby court search page.
    private VBox buildNearbyTab() {
        Label title = pageTitle("Find Nearby");
        Label subtitle = pageSubtitle("Enter coordinates, or leave both blank to use Russell Union.");

        TextField latField = new TextField();
        latField.setPromptText("32.4237");

        TextField lonField = new TextField();
        lonField.setPromptText("-81.7840");

        Button searchButton = new Button("Find Courts");
        searchButton.getStyleClass().add("primary-button");

        HBox fields = new HBox(12,
                fieldGroup("Latitude", latField),
                fieldGroup("Longitude", lonField)
        );
        HBox.setHgrow(fields.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(fields.getChildren().get(1), Priority.ALWAYS);

        Label hint = new Label("Leave both fields empty to use the default campus location.");
        hint.getStyleClass().add("hint");

        VBox searchCard = new VBox(12, fields, searchButton, hint);
        searchCard.getStyleClass().add("card");

        Label resultsHeading = new Label("Results");
        resultsHeading.getStyleClass().add("section-title");

        VBox resultsBox = new VBox(0);
        resultsBox.getChildren().add(emptyState("Search for a location to see nearby courts."));

        VBox resultsCard = new VBox(10, resultsHeading, resultsBox);
        resultsCard.getStyleClass().add("card");

        latField.setOnAction(e -> searchButton.fire());
        lonField.setOnAction(e -> searchButton.fire());

        searchButton.setOnAction(e -> {
            double lat;
            double lon;
            try {
                if (latField.getText().isBlank() && lonField.getText().isBlank()) {
                    lat = 32.4237;
                    lon = -81.7840;
                } else if (latField.getText().isBlank() || lonField.getText().isBlank()) {
                    showAlert("Enter both latitude and longitude, or leave both blank.");
                    return;
                } else {
                    lat = Double.parseDouble(latField.getText().trim());
                    lon = Double.parseDouble(lonField.getText().trim());
                    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                        showAlert("Latitude must be between -90 and 90, and longitude between -180 and 180.");
                        return;
                    }
                }
            } catch (NumberFormatException ex) {
                showAlert("Enter valid numbers for latitude and longitude.");
                return;
            }

            List<NearbyCourt> nearby = courtRepo.findNearby(lat, lon);
            resultsBox.getChildren().clear();

            for (int i = 0; i < nearby.size(); i++) {
                NearbyCourt item = nearby.get(i);
                Court court = item.court();

                Label name = new Label(court.getName());
                name.getStyleClass().add("court-name");

                Label detail = new Label("Basketball court");
                detail.getStyleClass().add("court-meta");

                VBox text = new VBox(2, name, detail);

                Label distance = new Label(String.format("%.2f mi", item.distanceMiles()));
                distance.getStyleClass().add("distance-pill");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(12, text, spacer, distance);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("court-row");

                VBox wrapper = new VBox(row);
                if (i < nearby.size() - 1) {
                    Region divider = new Region();
                    divider.getStyleClass().add("row-divider");
                    wrapper.getChildren().add(divider);
                }
                resultsBox.getChildren().add(wrapper);
            }
        });

        VBox page = new VBox(8, title, subtitle, searchCard, resultsCard);
        page.getStyleClass().add("page");
        return page;
    }
    // Builds the check-in form.
    private VBox buildCheckInTab() {
        Label title = pageTitle("Check In");
        Label subtitle = pageSubtitle("Save a visit so the app can build popular-time data.");

        ComboBox<Court> courtBox = new ComboBox<>(FXCollections.observableArrayList(courtRepo.getAllCourts()));
        courtBox.setPromptText("Choose a court");

        ComboBox<DayOfWeek> dayBox = new ComboBox<>(FXCollections.observableArrayList(DayOfWeek.values()));
        dayBox.setValue(LocalDate.now().getDayOfWeek());

        TextField timeField = new TextField();
        timeField.setPromptText("6:30 PM");
        timeField.setText(LocalTime.now().withSecond(0).withNano(0).format(TIME_FORMAT));

        Button checkInButton = new Button("Save Check-In");
        checkInButton.getStyleClass().add("primary-button");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        VBox form = new VBox(14,
                fieldGroup("Court", courtBox),
                fieldGroup("Day", dayBox),
                fieldGroup("Time", timeField),
                checkInButton,
                statusLabel
        );
        form.setMaxWidth(520);

        VBox card = new VBox(form);
        card.getStyleClass().add("card");

        checkInButton.setOnAction(e -> {
            Court court = courtBox.getValue();
            DayOfWeek day = dayBox.getValue();

            if (court == null || day == null) {
                showAlert("Pick a court and a day first.");
                return;
            }

            LocalTime time;
            try {
                time = LocalTime.parse(timeField.getText().trim().toUpperCase(), TIME_FORMAT);
            } catch (DateTimeParseException ex) {
                showAlert("Time should look like 6:30 PM.");
                return;
            }

            checkInRepo.addCheckIn(new CheckIn(court.getId(), day, time));
            statusLabel.setText("Saved: " + court.getName() + " • " + formatDay(day) + " at " + time.format(TIME_FORMAT));
            statusLabel.getStyleClass().setAll("label", "success-box");
            statusLabel.setManaged(true);
            statusLabel.setVisible(true);
        });


        VBox page = new VBox(8, title, subtitle, card);
        page.getStyleClass().add("page");
        return page;
    }
    // Builds the popular times page.
    private VBox buildPopularTimesTab() {
        Label title = pageTitle("Popular Times");
        Label subtitle = pageSubtitle("View check-in activity for a selected court.");

        ComboBox<Court> courtBox = new ComboBox<>(FXCollections.observableArrayList(courtRepo.getAllCourts()));
        courtBox.setPromptText("Choose a court");
        HBox.setHgrow(courtBox, Priority.ALWAYS);

        Button viewButton = new Button("Show Times");
        viewButton.getStyleClass().add("primary-button");

        HBox selector = new HBox(12, courtBox, viewButton);
        selector.setAlignment(Pos.CENTER_LEFT);

        VBox selectorCard = new VBox(8, selector);
        selectorCard.getStyleClass().add("card");

        VBox resultsBox = new VBox(10);
        resultsBox.getChildren().add(emptyState("Choose a court to see its popular times."));

        VBox resultsCard = new VBox(resultsBox);
        resultsCard.getStyleClass().add("card");

        viewButton.setOnAction(e -> {
            Court court = courtBox.getValue();
            if (court == null) {
                showAlert("Pick a court first.");
                return;
            }

            Map<DayOfWeek, Map<TimeSlot, Integer>> summary =
                    checkInRepo.summarizeByDayAndSlot(court.getId());

            resultsBox.getChildren().clear();

            Label courtName = new Label(court.getName());
            courtName.getStyleClass().add("results-court-name");
            resultsBox.getChildren().add(courtName);

            if (summary.isEmpty()) {
                resultsBox.getChildren().add(emptyState("No check-ins have been logged for this court yet."));
                return;
            }

            for (DayOfWeek day : DayOfWeek.values()) {
                Map<TimeSlot, Integer> slots = summary.get(day);
                if (slots == null || slots.isEmpty()) {
                    continue;
                }

                Label dayLabel = new Label(formatDay(day));
                dayLabel.getStyleClass().add("day-label");

                VBox slotRows = new VBox(5);
                for (TimeSlot slot : TimeSlot.values()) {
                    Integer count = slots.get(slot);
                    if (count == null) {
                        continue;
                    }

                    Label slotName = new Label(slot.getLabel());
                    slotName.getStyleClass().add("slot-name");

                    Label countLabel = new Label(count + (count == 1 ? " check-in" : " check-ins"));
                    countLabel.getStyleClass().add("slot-count");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    HBox slotRow = new HBox(10, slotName, spacer, countLabel);
                    slotRow.getStyleClass().add("slot-row");
                    slotRows.getChildren().add(slotRow);
                }

                VBox dayGroup = new VBox(6, dayLabel, slotRows);
                dayGroup.getStyleClass().add("day-group");
                resultsBox.getChildren().add(dayGroup);
            }

            Label peak = new Label("Busiest overall: " + checkInRepo.findPeakTime(court.getId()));
            peak.getStyleClass().add("peak-box");
            resultsBox.getChildren().add(peak);
        });

        VBox page = new VBox(8, title, subtitle, selectorCard, resultsCard);
        page.getStyleClass().add("page");
        return page;
    }

    private Label pageTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        return label;
    }

    private Label pageSubtitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-subtitle");
        return label;
    }

    private VBox fieldGroup(String labelText, Control control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        VBox group = new VBox(6, label, control);
        group.setMaxWidth(Double.MAX_VALUE);
        return group;
    }

    private Label emptyState(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        label.setWrapText(true);
        return label;
    }

    private String formatDay(DayOfWeek day) {
        String lower = day.name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText("Check your entry");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
