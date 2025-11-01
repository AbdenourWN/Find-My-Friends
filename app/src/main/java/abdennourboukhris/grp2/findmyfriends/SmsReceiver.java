package abdennourboukhris.grp2.findmyfriends;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavDeepLinkBuilder;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    public static final String CHANNEL_ID = "FindMyFriendsChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    String senderNumber = smsMessage.getDisplayOriginatingAddress();
                    String messageBody = smsMessage.getMessageBody();

                    if (messageBody.startsWith("findFriends: Send me your location")) {
                        Log.d(TAG, "Received location request from: " + senderNumber);
                        //fetchAndSendLocationWithFused(context, senderNumber);

                        fetchAndSendLocationWithManager(context, senderNumber);

                    } else if (messageBody.startsWith("findFriends_location:")) {
                        Log.d(TAG, "Received location data from: " + senderNumber);
                        saveAndShowNotification(context, senderNumber, messageBody);
                    }
                }
            }
        }
    }

    /**
     * The original method to get location using LocationManager.
     * This is less efficient than FusedLocationProvider but works as requested.
     */
    private void fetchAndSendLocationWithManager(Context context, String destinationNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot get location, permission not granted.");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that will receive the location update
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // We got a location, now send it
                sendLocationSms(destinationNumber, location.getLatitude(), location.getLongitude());
                // IMPORTANT: Stop listening for updates to save battery
                locationManager.removeUpdates(this);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                // If the provider is disabled, we should also stop listening
                locationManager.removeUpdates(this);
            }
        };

        // Request a single, fresh location update from the GPS provider
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void fetchAndSendLocationWithFused(Context context, String destinationNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot get location, permission not granted.");
            return;
        }

        FusedLocationProviderClient mClient = LocationServices.getFusedLocationProviderClient(context);

        mClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                sendLocationSms(destinationNumber, location.getLatitude(), location.getLongitude());
            } else {
                Log.e(TAG, "Location is null, requesting fresh update...");
                LocationRequest locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setNumUpdates(1);

                mClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        Location freshLocation = locationResult.getLastLocation();
                        if (freshLocation != null) {
                            sendLocationSms(destinationNumber, freshLocation.getLatitude(), freshLocation.getLongitude());
                        }
                    }
                }, Looper.getMainLooper());
            }
        });
    }

    private void sendLocationSms(String destination, double lat, double lon) {
        String locationMessage = "findFriends_location:" + lat + "," + lon;
        try {
            SmsManager.getDefault().sendTextMessage(destination, null, locationMessage, null, null);
            Log.d(TAG, "Sent location back to " + destination);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send location SMS", e);
        }
    }

    private void saveAndShowNotification(Context context, String sender, String message) {
        createNotificationChannel(context);
        float latitude;
        float longitude;
        try {
            String coords = message.split(":")[1];
            String[] latLng = coords.split(",");
            latitude = Float.parseFloat(latLng[0]);
            longitude = Float.parseFloat(latLng[1]);
        } catch (Exception e) {
            Log.e(TAG, "Could not parse location from SMS", e);
            return;
        }

        saveLocationToDatabase(context, String.valueOf(latitude), String.valueOf(longitude), sender);

        Bundle args = new Bundle();
        args.putFloat("focus_latitude", latitude);
        args.putFloat("focus_longitude", longitude);

        PendingIntent pendingIntent = new NavDeepLinkBuilder(context)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.navigation_map)
                .setArguments(args)
                .createPendingIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(org.osmdroid.library.R.drawable.ic_menu_compass)
                .setContentTitle("New Location Saved")
                .setContentText("Location from " + sender + " has been added.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void saveLocationToDatabase(Context context, String latitude, String longitude, String numero) {
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.URL_POST_Location,
                response -> Log.d(TAG, "Successfully saved location: " + response),
                error -> Log.e(TAG, "Failed to save location: " + error.toString())
        ) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("latitude", latitude);
                params.put("longitude", longitude);
                params.put("numero", numero);
                params.put("pseudo", numero);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FindMyFriends Channel";
            String description = "Channel for friend location notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}