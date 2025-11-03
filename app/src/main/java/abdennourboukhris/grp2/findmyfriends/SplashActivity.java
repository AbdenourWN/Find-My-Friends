package abdennourboukhris.grp2.findmyfriends;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Keep the splash screen visible while we decide where to go.
        // We will remove this listener in the destination activity.
        splashScreen.setKeepOnScreenCondition(() -> true);

        // --- The Routing Logic ---
        SessionManager sessionManager = new SessionManager(this);
        Intent intent;

        if (sessionManager.isLoggedIn()) {
            // User is logged in, go to the main app.
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // User is not logged in, go to the login screen.
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        // Finish SplashActivity so the user can't navigate back to it.
        finish();
    }
}