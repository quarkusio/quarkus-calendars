package io.quarkus.calendars.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import io.quarkus.calendars.config.GoogleCalendarConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Service for interacting with the Google Calendar API using a service account.
 */
@ApplicationScoped
public class GoogleCalendarService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_EVENTS);

    @Inject
    GoogleCalendarConfig config;

    private Calendar calendarService;

    public Calendar getCalendarService() throws GeneralSecurityException, IOException {
        if (calendarService == null) {
            calendarService = createCalendarService();
        }
        return calendarService;
    }

    private Calendar createCalendarService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        String keyPath = config.serviceAccountKey();
        Path credentialsPath = Paths.get(keyPath);

        if (!Files.exists(credentialsPath)) {
            throw new IllegalStateException("Service account key file not found: " + keyPath);
        }

        GoogleCredentials credentials;
        try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath.toFile())) {
            credentials = GoogleCredentials.fromStream(serviceAccountStream)
                    .createScoped(SCOPES);
        }

        return new Calendar.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(config.applicationName())
                .build();
    }

    /**
     * Test the connection to the Google Calendar API by fetching a small number of events
     * from both calendars.
     *
     * @return true if the connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            Calendar service = getCalendarService();
            String releasesCalendarId = getReleasesCalendarId();
            String callCalendarId = getCallsCalendarId();

            if (releasesCalendarId.isBlank()) {
                Log.warn("Releases calendar ID is not configured");
                return false;
            }

            if (callCalendarId.isBlank()) {
                Log.warn("Calls calendar ID is not configured");
                return false;
            }

            Events events1 = service.events()
                    .list(releasesCalendarId)
                    .setMaxResults(1)
                    .execute();

            Events events2 = service.events()
                    .list(callCalendarId)
                    .setMaxResults(1)
                    .execute();

            Log.infof("Successfully connected to calendar. Found %d events in the release calendar, and %d in the call calendar", events1.getItems().size(), events2.getItems().size());
            return true;
        } catch (Exception e) {
            Log.errorf(e, "Failed to connect to Google Calendar API");
            return false;
        }
    }

    /**
     * List events from the specified calendar.
     *
     * @param calendarId the ID of the calendar to fetch events from
     * @param maxResults the maximum number of events to return
     * @return the list of events
     * @throws GeneralSecurityException thrown if there is a security issue
     * @throws IOException              thrown if there is an I/O issue
     */
    public List<Event> listEvents(String calendarId, int maxResults) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();

        Events events = service.events()
                .list(calendarId)
                .setMaxResults(maxResults)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        return events.getItems();
    }

    /**
     * Create a new event in the specified calendar.
     *
     * @param calendarId the ID of the calendar to create the event in
     * @param event      the event to create
     * @throws GeneralSecurityException thrown if there is a security issue
     * @throws IOException              thrown if there is an I/O issue
     */
    public void createEvent(String calendarId, Event event) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();

        service.events()
                .insert(calendarId, event)
                .execute();
    }

    /**
     * Update an existing event in the specified calendar.
     *
     * @param calendarId the ID of the calendar containing the event
     * @param eventId    the ID of the event to update
     * @param event      the updated event data
     * @throws GeneralSecurityException thrown if there is a security issue
     * @throws IOException              thrown if there is an I/O issue
     */
    public void updateEvent(String calendarId, String eventId, Event event) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();

        service.events()
                .update(calendarId, eventId, event)
                .execute();
    }

    /**
     * Delete an event from the specified calendar.
     *
     * @param calendarId the ID of the calendar containing the event
     * @param eventId    the ID of the event to delete
     * @throws GeneralSecurityException thrown if there is a security issue
     * @throws IOException              thrown if there is an I/O issue
     */
    public void deleteEvent(String calendarId, String eventId) throws GeneralSecurityException, IOException {
        Calendar service = getCalendarService();

        service.events()
                .delete(calendarId, eventId)
                .execute();
    }

    /**
     * Get the configured releases calendar ID.
     *
     * @return the releases calendar ID, or an empty string if not configured
     */
    public String getReleasesCalendarId() {
        return config.calendars().releases().id().orElse("");
    }

    /**
     * Get the configured calls calendar ID.
     *
     * @return the calls calendar ID, or an empty string if not configured
     */
    public String getCallsCalendarId() {
        return config.calendars().calls().id().orElse("");
    }
}
