package com.cmput301f19t09.vibes.models;

import android.location.Location;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents an event relating to a mood. Extends Event to inherit date, time,
 * and description. For a MoodEvent date and time are not optional while description
 * is. MoodEvent also has a non-optional EmotionalState and optional socialSituation and
 * Location. A MoodEvent is associated with a particular User.
 * <p>
 * Implements serializable to be passable in intents. Implements Comparable to allow
 * MoodList to be sorted in reverse chronological order of LocalDateTime of the MoodEvents.
 *
 * @TODO: Stricter restrictions on non-optional attributes.
 */
public class MoodEvent extends Event implements Serializable, Comparable, ClusterItem {
    // date not optional
    // time not optional
    // description optional
    private EmotionalState state; // not optional
    private int socialSituation; // optional
    private transient Uri photo; // optional
    private transient Location location; // optional
    private User user; // the user that the mood is associated with

    /**
     * Creates a new MoodEvent object by accepting a date, time, description, state, socialSituation,
     * location, and user.
     *
     * @param date            Date of when the event occurred; should have the format ISO_LOCAL_DATE (yyyy-MM-dd).
     * @param time            Time of when the event occurred; should have the format HH:mm.
     * @param description     Describes the circumstances surrounding an event.
     * @param state           The EmotionalState of a MoodEvent which contains an emotion, emoticon, and associated color.
     * @param socialSituation The context surrounding an event; can be one of alone, with one other person, with two
     *                        to several people, or with a crowd.
     * @param photo           Used in part to describe the circumstances of an event. The path to a photo.
     * @param location        Where a MoodEvent occurred; the location should contain latitude and longitude coordinates.
     * @param user            The user that the created the MoodEvent.
     */
    public MoodEvent(LocalDate date, LocalTime time, String description, EmotionalState state,
                     int socialSituation, Uri photo, Location location, User user) {
        super(date, time, description);
        this.state = state;
        this.socialSituation = socialSituation;
        this.photo = photo;
        this.location = location;
        this.user = user;
    }

    /**
     * Getter for the EmotionalState. An EmotionalState is used to collect an emotion with
     * its associated emoticon and color.
     *
     * @return The EmotionalState associated with a MoodEvent.
     */
    public EmotionalState getState() {
        return state;
    }

    /**
     * Sets the state attribute. EmotionalState used to indicate the emotion that was felt
     * during a MoodEvent. A MoodEvent can only be associated with one EmotionalState.
     *
     * @param state The EmotionalState associated with a MoodEvent.
     */
    public void setState(EmotionalState state) {
        this.state = state;
    }

    /**
     * Getter for the socialSituation attribute. A socialSituation is used to provide information about
     * the number of individuals present during a mood event. A predefined set of values;
     * can be one of alone, with one other person, with two to several people, or with a crowd.
     *
     * @return The socialSituation associated with a MoodEvent; return value of -1 indicates that
     * SocialSituation has not been specified.
     * @TODO: Enforce the set of possible choices.
     */
    public int getSocialSituation() {
        return socialSituation;
    }

    /**
     * Sets the socialSituation attribute. A socialSituation is used to provide information about
     * the number of individuals present during a mood event. A predefined set of values;
     * can be one of alone, with one other person, with two to several people, or with a crowd.
     *
     * @param socialSituation The socialSituation associated with a MoodEvent.
     * @TODO: Enforce the set of possible choices.
     */
    public void setSocialSituation(int socialSituation) {
        this.socialSituation = socialSituation;
    }

    /**
     * Returns the photo for a mood event. Used in part to describe a mood event along with
     * description.
     */
    public Uri getPhoto() {
        return this.photo;
    }

    /**
     * Sets the photo for a mood event. Used in part to describe a mood event along with
     * description.
     *
     * @param photo The photo path to attach to a mood event.
     */
    public void setPhoto(Uri photo) {
        this.photo = photo;
    }

    /**
     * Getter for the location attribute. The location is where a MoodEvent was created. It contains
     * the latitude and longitude coordinates. Location is used to map where a MoodEvent occurred.
     *
     * @return The location associated with a MoodEvent; return value of null indicates that
     * location has not been specified; could be due to app permissions issues.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Setter for the location attribute. The location is where a MoodEvent was created. It contains
     * the latitude and longitude coordinates. Location is typically set by a location services API.
     * The location passed in should represent where a MoodEvent occurred and therefore should be
     * acquired when creating a new MoodEvent only.
     *
     * @param location The location associated with a MoodEvent.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Converts the Location object into a printable string. Used for displaying the latitude
     * and longitude coordinates associated with a Location object.
     *
     * @return The String representation of: "<location_lat> <location_long>"
     */
    public String getLocationString() {
        return Location.convert(
                location.getLatitude(), Location.FORMAT_DEGREES)
                + " "
                + Location.convert(location.getLongitude(), Location.FORMAT_DEGREES
        );
    }

    /**
     * Getter for the user attribute. The user attribute represents who created this MoodEvent. Used
     * for storing in the database and for displaying on that users moodList.
     *
     * @return The user who created this MoodEvent.
     */
    public User getUser() {
        return user;
    }

    /**
     * Setter for the user attribute. The user attribute represents who created this MoodEvent. Only
     * the user who is currently in session and creating a new MoodEvent should be associated with
     * a the newly created MoodEvent.
     *
     * @param user The user associated with a MoodEvent.
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Custom comparator to allow MoodEvent objects to be compared.
     *
     * @return A positive integer, if the current MoodEvent is greater than the passed in MoodEvent o.
     * The integer 0, if both objects are considered to be the same.
     * A negative integer, if the current MoodEvent is less than the passed in MoodEvent o.
     */
    @Override
    public int compareTo(Object o) {
        return this.getLocalDateTime().compareTo(((MoodEvent) o).getLocalDateTime());
    }

    /**
     * Used in the maps fragment to get the location of the mood event in the
     * LatLng form
     *
     * @return Returns the LatLng of the location the moodevent has.
     */
    public LatLng getLanLng() {
        return new LatLng(this.location.getLatitude(), this.location.getLongitude());
    }

    /**
     * Used in the maps fragment to get the position of the mood event.
     * This function is called by the CustomClusterRenderer
     *
     * @return
     */
    @Override
    public LatLng getPosition() {
        return this.getLanLng();
    }

    /**
     * Used in the maps fragment to get the title of the mood event.
     * Curently it is set to being the emotion.
     * This function is called by the CustomClusterRenderer.
     *
     * @return returns the emotion of the mood event
     */
    @Override
    public String getTitle() {
        if (!this.user.getUid().equals(UserManager.getCurrentUserUID())) {
            return this.user.getUserName();
        } else {
            return null;
        }
    }

    /**
     * Used in the maps fragment to get the snippet of the mood event.
     * Curently it is set to being the description.
     * This function is called by the CustomClusterRenderer.
     *
     * @return Returns the description of the mood event.
     */
    @Override
    public String getSnippet() {
        return this.getState().getEmotion();
    }
}
