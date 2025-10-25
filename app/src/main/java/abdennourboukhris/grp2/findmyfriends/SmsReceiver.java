package abdennourboukhris.grp2.findmyfriends;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import abdennourboukhris.grp2.findmyfriends.ui.GoogleMap.GoogleMaps;


public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    public static final String CHANNEL_ID = "FindMyFriendsChannel";

    private static final String CHANNEL_NAME = "Position Notifications";
    private static final int NOTIFICATION_ID = 1001;


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    String senderNumber = smsMessage.getDisplayOriginatingAddress();
                    String messageBody = smsMessage.getMessageBody();

                    if (messageBody.startsWith("findFriends: Send me your location")) {
                        Log.d(TAG, "Received location request from: " + senderNumber);

                        fetchAndSendLocationWithFused(context, senderNumber);
                    } else if (messageBody.startsWith("findFriends_location:")) {
                        Log.d(TAG, "Received location data from: " + senderNumber);
                        //showLocationNotification(context, senderNumber, messageBody);
                        showPositionNotificationGoogle(context, senderNumber, messageBody);
                    }
                }
            }
        }
    }

    private void fetchAndSendLocation(Context context, String destinationNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot get location, permission not granted.");
            return;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // We have a location! Now we can send it.
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String locationMessage = "findFriends_location:" + latitude + "," + longitude;

                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(destinationNumber, null, locationMessage, null, null);
                    Log.d(TAG, "Sent location back to " + destinationNumber);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send location SMS", e);
                }

                locationManager.removeUpdates(this);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                locationManager.removeUpdates(this);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void showLocationNotification(Context context, String sender, String message) {
        createNotificationChannel(context);

        Bundle args = new Bundle();
        try {
            String coords = message.split(":")[1];
            String[] latLng = coords.split(",");

            args.putFloat("latitude", Float.parseFloat(latLng[0]));
            args.putFloat("longitude", Float.parseFloat(latLng[1]));
            args.putString("sender", sender);

        } catch (Exception e) {
            Log.e(TAG, "Could not parse location from SMS", e);
            return;
        }


        PendingIntent pendingIntent = new NavDeepLinkBuilder(context)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.navigation_map)
                .setArguments(args)
                .createPendingIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dashboard_black_24dp)
                .setContentTitle("Location Received")
                .setContentText("You can now view " + sender + "'s location")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // 6. Actually show it
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
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

    private void fetchAndSendLocationWithFused(Context context, String destinationNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot get location, permission not granted.");
            return;
        }

        FusedLocationProviderClient mClient = LocationServices.getFusedLocationProviderClient(context);

        mClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String locationMessage = "findFriends_location:" + latitude + "," + longitude;

                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(destinationNumber, null, locationMessage, null, null);
                    Log.d(TAG, "Sent location back to " + destinationNumber);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send location SMS", e);
                }

            } else {
                Log.e(TAG, "Location is null, requesting fresh update...");

                LocationRequest locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(2000)
                        .setFastestInterval(1000)
                        .setNumUpdates(1);

                mClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult != null) {
                            Location freshLocation = locationResult.getLastLocation();
                            double latitude = freshLocation.getLatitude();
                            double longitude = freshLocation.getLongitude();
                            String locationMessage = "findFriends_location:" + latitude + "," + longitude;

                            try {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(destinationNumber, null, locationMessage, null, null);
                                Log.d(TAG, "Sent fresh location back to " + destinationNumber);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to send fresh location SMS", e);
                            }

                            mClient.removeLocationUpdates(this);
                        }
                    }
                }, Looper.getMainLooper());
            }
        });
    }

    public void showPositionNotificationGoogle(Context context, String sender, String message) {

        createNotificationChannel(context);

        String coords = message.split(":")[1];
        String[] latLng = coords.split(",");


        Intent mapIntent = new Intent(context, GoogleMaps.class);

        mapIntent.putExtra("latitude", Double.parseDouble(latLng[0]));
        mapIntent.putExtra("longitude", Double.parseDouble(latLng[1]));
        mapIntent.putExtra("sender", sender);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("Location Received")
                .setContentText("You can now view " + sender + "'s location")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Afficher la notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }


}