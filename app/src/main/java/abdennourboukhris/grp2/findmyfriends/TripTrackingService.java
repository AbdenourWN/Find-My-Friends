package abdennourboukhris.grp2.findmyfriends;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TripTrackingService extends Service {

    private static final String TAG = "TripTrackingService";
    public static final String ACTION_START_TRIP = "ACTION_START_TRIP";
    public static final String ACTION_STOP_TRIP = "ACTION_STOP_TRIP";
    public static final String EXTRA_TRIP_ID = "EXTRA_TRIP_ID";

    private static final String NOTIFICATION_CHANNEL_ID = "TripTrackingChannel";
    private static final int NOTIFICATION_ID = 12345;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    private int currentTripId = -1;

    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);
        sessionManager = new SessionManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_TRIP.equals(action)) {
                currentTripId = intent.getIntExtra(EXTRA_TRIP_ID, -1);
                if (currentTripId != -1) {
                    Log.d(TAG, "Starting trip with ID: " + currentTripId);
                    isRunning = true;
                    startLocationTracking();
                }
            } else if (ACTION_STOP_TRIP.equals(action)) {
                Log.d(TAG, "Stopping trip.");
                stopSelf();
            }
        }

        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void startLocationTracking() {
        // 1. Create the notification channel (required for Android 8+)
        createNotificationChannel();

        // 2. Build the persistent notification for the foreground service
        Notification notification = buildNotification();

        // 3. Start the service in the foreground
        startForeground(NOTIFICATION_ID, notification);

        // 4. Configure location request parameters
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateDistanceMeters(50) // 50 meters minimum distance
                .build();

        // 5. Define the callback for when location updates are received
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "New location: " + location.getLatitude() + ", " + location.getLongitude());
                    sendLocationToServer(location);
                }
            }
        };


        // 6. Start listening for location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private Notification buildNotification() {
        // Intent for what happens when the user taps the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Intent for the "Stop" button in the notification
        Intent stopIntent = new Intent(this, TripTrackingService.class);
        stopIntent.setAction(ACTION_STOP_TRIP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Trip Recording in Progress")
                .setContentText("Your location is being tracked for the current trip.")
                .setSmallIcon(R.drawable.ic_map_vector) // Use a suitable icon
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_stop, "Stop Trip", stopPendingIntent) // Add the stop action
                .setOngoing(true) // Make it non-dismissible
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Trip Tracking Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void sendLocationToServer(Location location) {
        if (currentTripId == -1) return;

        String url = Config.LOCATION_POINTS_CRUD;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            Log.d(TAG, "Successfully saved location point.");
                        } else {
                            Log.e(TAG, "Server failed to save point: " + jsonResponse.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing server response: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Volley error sending location: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "create");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                params.put("trip_id", String.valueOf(currentTripId));
                params.put("latitude", String.valueOf(location.getLatitude()));
                params.put("longitude", String.valueOf(location.getLongitude()));
                return params;
            }
        };
        requestQueue.add(request);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (fusedLocationClient != null && locationCallback != null) {
            Log.d(TAG, "Stopping location updates.");
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (currentTripId != -1) {
            endTripOnServer();
        }
    }

    private void endTripOnServer() {
        String url = Config.TRIPS_CRUD;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            Log.d(TAG, "Successfully marked trip as ended on server.");
                        } else {
                            Log.e(TAG, "Failed to end trip on server: " + jsonResponse.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "End Trip JSON Error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "End Trip Volley Error: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "end_trip");
                params.put("trip_id", String.valueOf(currentTripId));
                return params;
            }
        };
        requestQueue.add(request);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}