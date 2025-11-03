package abdennourboukhris.grp2.findmyfriends;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "FindMyFriendsSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_PSEUDO = "user_pseudo";
    private static final String KEY_USER_NUMERO = "user_numero";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context _context;

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(int userId, String pseudo, String numero) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_PSEUDO, pseudo);
        editor.putString(KEY_USER_NUMERO, numero);
        editor.commit();
    }

    public boolean isLoggedIn() {
        // If user_id is -1 (default), user is not logged in
        return getUserId() != -1;
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getUserPseudo() {
        return pref.getString(KEY_USER_PSEUDO, null);
    }

    public String getUserNumero() {
        return pref.getString(KEY_USER_NUMERO, null);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
        // You might want to redirect to a login activity here if you had one
    }
}