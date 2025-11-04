package abdennourboukhris.grp2.findmyfriends.ui.home;

public class Trip {
    private int tripId;
    private String tripName;
    private String friendPseudo;
    private String startTime;

    public Trip(int tripId, String tripName, String friendPseudo, String startTime) {
        this.tripId = tripId;
        this.tripName = tripName;
        this.friendPseudo = friendPseudo;
        this.startTime = startTime;
    }

    // Getters
    public int getTripId() {
        return tripId;
    }

    public String getTripName() {
        return tripName;
    }

    public String getFriendPseudo() {
        return friendPseudo;
    }

    public String getStartTime() {
        return startTime;
    }
}