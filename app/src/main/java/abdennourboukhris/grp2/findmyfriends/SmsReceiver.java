package abdennourboukhris.grp2.findmyfriends;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavDeepLinkBuilder;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    public static final String CHANNEL_ID = "FindMyFriendsChannel";
    private RequestQueue requestQueue;

    private interface UserFoundCallback {
        void onUserFound(int userId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            String senderNumber = smsMessage.getDisplayOriginatingAddress();
            String messageBody = smsMessage.getMessageBody();

            if (messageBody.startsWith("findFriends: Send me your location")) {
                Log.d(TAG, "Received location request from: " + senderNumber);
                handleLocationRequest(context, senderNumber);
            } else if (messageBody.startsWith("findFriends_location:")) {
                Log.d(TAG, "Received location data from: " + senderNumber);
                handleReceivedLocation(context, senderNumber, messageBody);
            }
        }
    }

    private void handleLocationRequest(Context context, String requesterNumber) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot identify device owner: READ_PHONE_STATE/NUMBERS permission not granted.");
            return;
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceOwnerNumber;
        try {
            deviceOwnerNumber = telephonyManager.getLine1Number();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException getting device number.", e);
            return;
        }

        if (deviceOwnerNumber == null || deviceOwnerNumber.isEmpty()) {
            Log.e(TAG, "Could not retrieve this device's phone number from the SIM. Cannot process request.");
            return;
        }

        Log.d(TAG, "This device's number is: " + deviceOwnerNumber + ". Finding its owner.");

        findUserByNumero(deviceOwnerNumber, deviceOwnerId -> {
            if (deviceOwnerId != -1) {

                Log.d(TAG, "Device owner found with ID: " + deviceOwnerId + ". Getting location.");
                getLocationAndSave(context, deviceOwnerId, requesterNumber);
            } else {
                Log.e(TAG, "No user is registered with this device's phone number. Ignoring request.");
            }
        });

    }

    private void findUserByNumero(String numero, UserFoundCallback callback) {
        String url = Config.USERS_CRUD;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status")) && !jsonResponse.isNull("data")) {
                            int userId = jsonResponse.getJSONObject("data").getInt("user_id");
                            callback.onUserFound(userId);
                        } else {
                            callback.onUserFound(-1); // User not found
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error in findUserByNumero: " + e.getMessage());
                        callback.onUserFound(-1);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error in findUserByNumero: " + error.toString());
                    callback.onUserFound(-1);
                })
        {
            @NonNull @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "find_by_numero");
                params.put("numero", numero);
                params.put("current_user_id", "-1"); // Not filtering against self
                return params;
            }
        };
        requestQueue.add(request);
    }

    /**
     * This method now uses LocationManager to get a single location update.
     */
    private void getLocationAndSave(Context context, int userId, String requesterNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted. Cannot process request.");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.d(TAG, "LocationManager found location. Saving to server.");
                saveLocationPointToServer(context, userId,requesterNumber, location.getLatitude(), location.getLongitude());

                // IMPORTANT: Remove the listener to save battery.
                locationManager.removeUpdates(this);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.w(TAG, "Location provider disabled. Removing listener.");
                locationManager.removeUpdates(this);
            }
        };

        // Request a single, high-accuracy update.
        Log.d(TAG, "Requesting location update from LocationManager...");
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
    }


    private void handleReceivedLocation(Context context, String senderNumber, String message) {
        float latitude;
        float longitude;
        try {
            String coords = message.split(":")[1];
            String[] latLng = coords.split(",");
            latitude = Float.parseFloat(latLng[0]);
            longitude = Float.parseFloat(latLng[1]);
        } catch (Exception e) {
            Log.e(TAG, "Could not parse location from received SMS", e);
            return;
        }
        findUserByNumeroAndSave(context, senderNumber, latitude, longitude);
    }

    private void findUserByNumeroAndSave(Context context, String numero, double lat, double lon) {
        String url = Config.USERS_CRUD;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status")) && !jsonResponse.isNull("data")) {
                            int senderId = jsonResponse.getJSONObject("data").getInt("user_id");
                            //saveLocationPointToServer(context, senderId, lat, lon);
                            showLocationReceivedNotification(context, numero, lat, lon);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error finding user by numero: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Volley error finding user: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "find_by_numero");
                params.put("numero", numero);
                params.put("current_user_id", "-1");
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void saveLocationPointToServer(Context context, int userId,String requesterNumber, double lat, double lon) {
        String url = Config.LOCATION_POINTS_CRUD;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            Log.d(TAG, "Successfully saved location point for user " + userId);
                                SmsManager smsManager = SmsManager.getDefault();
                                String message = "findFriends_location: "+lat+" , "+lon;
                                //smsManager.sendTextMessage(requesterNumber, null, message, null, null);
                                smsManager.sendTextMessage("+15551234567", null, message, null, null);

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
                params.put("user_id", String.valueOf(userId));
                params.put("trip_id", "");
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                return params;
            }
        };
        requestQueue.add(request);
    }

    @SuppressLint("MissingPermission")
    private void showLocationReceivedNotification(Context context, String sender, double lat, double lon) {
        createNotificationChannel(context);
        Bundle args = new Bundle();
        args.putFloat("focus_latitude", (float) lat);
        args.putFloat("focus_longitude", (float) lon);

        PendingIntent pendingIntent = new NavDeepLinkBuilder(context)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.navigation_map)
                .setArguments(args)
                .createPendingIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_map_vector)
                .setContentTitle("New Location Received")
                .setContentText("A new location from " + sender + " is available on the map.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Location Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}