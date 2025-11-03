package abdennourboukhris.grp2.findmyfriends;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText etPhoneNumber;
    private Button btnContinue;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;

    private static final int PREFILL_PERMISSION_CODE = 101;
    private static final int LOGIN_PERMISSIONS_CODE = 102;
    private String enteredPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);
        requestQueue = Volley.newRequestQueue(this);

        // Immediately request the optional permission for pre-filling.
        requestPrefillPermission();

        btnContinue.setOnClickListener(v -> {
            String numero = etPhoneNumber.getText().toString().trim();
            if (numero.isEmpty() || numero.length() < 5) {
                Toast.makeText(this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
            } else {
                enteredPhoneNumber = numero;
                checkAndRequestLoginPermissions();
            }
        });
    }

    private void requestPrefillPermission() {
        Log.d(TAG, "Checking for READ_PHONE_STATE permission...");
        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_NUMBERS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PREFILL_PERMISSION_CODE);
        } else {
            prefillPhoneNumber();
        }
    }

    private void checkAndRequestLoginPermissions() {
        Log.d(TAG, "Checking for SEND_SMS permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission not found. Requesting it.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, LOGIN_PERMISSIONS_CODE);
        } else {
            Log.d(TAG, "Permission already granted. Calling checkIfUserExists().");
            checkIfUserExists(enteredPhoneNumber);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PREFILL_PERMISSION_CODE) {
            Log.d(TAG, "Received result for PREFILL_PERMISSION_CODE.");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "READ_PHONE_STATE permission GRANTED. Calling prefillPhoneNumber().");
                prefillPhoneNumber();
            } else {
                Log.d(TAG, "READ_PHONE_STATE permission DENIED.");
                // User denied permission, we do nothing.
            }
        } else if (requestCode == LOGIN_PERMISSIONS_CODE) {
            Log.d(TAG, "Received result for LOGIN_PERMISSIONS_CODE.");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "SEND_SMS permission GRANTED. Calling checkIfUserExists().");
                checkIfUserExists(enteredPhoneNumber);
            } else {
                Log.d(TAG, "SEND_SMS permission DENIED.");
                Toast.makeText(this, "SMS permission is required to verify your number.", Toast.LENGTH_LONG).show();
            }
        }
    }
    @SuppressLint("HardwareIds")
    private void prefillPhoneNumber() {
        Log.d(TAG, "Attempting to prefill phone number...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
            try {
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String phoneNumber = tm.getLine1Number();

                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    phoneNumber = "+15551234567";
                }

                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    etPhoneNumber.setText(phoneNumber);
                } else {
                    Toast.makeText(this, "Could not detect phone number. Please enter manually.", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Log.e("LoginActivity", "Permission issue while getting phone number", e);
            }
        }
    }
    private void checkIfUserExists(String numero) {
        setLoading(true);
        String url = Config.CHECK_USER_EXISTS;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean exists = jsonResponse.getJSONObject("data").getBoolean("exists");
                        if (exists) {
                            sendOtpRequest(numero, null);
                        } else {
                            promptForPseudo(numero);
                        }
                    } catch (JSONException e) {
                        setLoading(false);
                        Toast.makeText(this, "Error: Could not parse server response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    setLoading(false);
                    Toast.makeText(this, "Network Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("numero", numero);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void promptForPseudo(String numero) {
        setLoading(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this,  R.style.MyRoundedDialogTheme);
        builder.setTitle("Create Account");
        builder.setMessage("This phone number isn't registered. Please enter your name to continue.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Your Name");

        // Create a parent container to hold the EditText and apply margins
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        // This adds left and right padding inside the dialog
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);

        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Register", (dialog, which) -> {
            String pseudo = input.getText().toString().trim();
            if (pseudo.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            } else {
                sendOtpRequest(numero, pseudo);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendOtpRequest(String numero, String pseudo) {
        setLoading(true);
        String url = Config.SEND_OTP;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            String otpCode = jsonResponse.getJSONObject("data").getString("debug_otp");
                            sendSmsWithOtp(numero, otpCode);
                            Intent intent = new Intent(this, OtpActivity.class);
                            intent.putExtra("numero", numero);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing OTP response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    setLoading(false);
                    Toast.makeText(this, "Network Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("numero", numero);
                if (pseudo != null) {
                    params.put("pseudo", pseudo);
                }
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void sendSmsWithOtp(String phoneNumber, String otpCode) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "Your FindMyFriends verification code is: " + otpCode;
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Verification SMS sent.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("LoginActivity", "SMS failed to send.", e);
            Toast.makeText(this, "SMS failed. DEBUG OTP: " + otpCode, Toast.LENGTH_LONG).show();
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnContinue.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnContinue.setEnabled(true);
        }
    }
}