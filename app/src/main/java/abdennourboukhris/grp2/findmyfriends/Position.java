package abdennourboukhris.grp2.findmyfriends;

public class Position {
    int IdPosition;
    double Latitude;
    double Longitude;
    String numero;
    String Pseudo;

    public Position(int idPosition, double latitude, double longitude, String numero, String pseudo) {
        IdPosition = idPosition;
        Latitude = latitude;
        Longitude = longitude;
        this.numero = numero;
        Pseudo = pseudo;
    }

    public Position(double latitude, double longitude, String numero, String pseudo) {
        Latitude = latitude;
        Longitude = longitude;
        this.numero = numero;
        Pseudo = pseudo;
    }

    public int getIdPosition() {
        return IdPosition;
    }

    public void setIdPosition(int idPosition) {
        IdPosition = idPosition;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getPseudo() {
        return Pseudo;
    }

    public void setPseudo(String pseudo) {
        Pseudo = pseudo;
    }

    @Override
    public String toString() {
        return
                "IdPosition=" + IdPosition +
                ", Latitude=" + Latitude +
                ", Longitude=" + Longitude +
                ", numero=" + numero +
                ", Pseudo='" + Pseudo + '\'';
    }
}
