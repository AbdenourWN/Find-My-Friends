package abdennourboukhris.grp2.findmyfriends.ui.friends;

public class Friend {
    private int friendshipId;
    private int userId;
    private String pseudo;
    private String numero;

    public Friend(int friendshipId, int userId, String pseudo, String numero) {
        this.friendshipId = friendshipId;
        this.userId = userId;
        this.pseudo = pseudo;
        this.numero = numero;
    }

    // Getters
    public int getFriendshipId() { return friendshipId; }
    public int getUserId() { return userId; }
    public String getPseudo() { return pseudo; }
    public String getNumero() { return numero; }
}