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
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavDeepLinkBuilder;


public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    public static final String CHANNEL_ID = "FindMyFriendsChannel";

    // onReceive and the notification logic remains the same...
    @Override
    public void onReceive(Context context, Intent intent) {
        // ... (This method is the same as before)
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu : pdus) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                String senderNumber = smsMessage.getDisplayOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();

                if (messageBody.startsWith("findFriends: Send me your location")) {
                    Log.d(TAG, "Received location request from: " + senderNumber);
                    fetchAndSendLocation(context, senderNumber);
                } else if (messageBody.startsWith("findFriends_location:")) {
                    Log.d(TAG, "Received location data from: " + senderNumber);
                    showLocationNotification(context, senderNumber, messageBody);
                }
            }
        }
    }

    // --- REWRITTEN with native LocationManager ---
    private void fetchAndSendLocation(Context context, String destinationNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot get location, permission not granted.");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that will receive the location updates
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

                // Important: Stop listening for updates to save battery
                locationManager.removeUpdates(this);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                // Called if the user disables GPS
                locationManager.removeUpdates(this);
            }
        };

        // Request a single, high-accuracy location update.
        // The listener above will be called when a location is found.
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    // showLocationNotification and createNotificationChannel are the same as before
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
                .setComponentName(MainActivity.class) // The host activity
                .setGraph(R.navigation.mobile_navigation) // The navigation graph
                .setDestination(R.id.navigation_map) // The destination fragment
                .setArguments(args) // The arguments to pass
                .createPendingIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dashboard_black_24dp) // Change icon if needed
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
}