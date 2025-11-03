package abdennourboukhris.grp2.findmyfriends;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class OtpActivity extends AppCompatActivity {

    private String numero;
    private EditText etOtp;
    private Button btnVerify;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        numero = getIntent().getStringExtra("numero");
        if (numero == null) {
            Toast.makeText(this, "Error: Phone number not provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);
        progressBar = findViewById(R.id.progressBarOtp);
        TextView tvInstruction = findViewById(R.id.tvOtpInstruction);
        tvInstruction.setText("We have sent a 6-digit code to\n" + numero);

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() == 6) {
                verifyOtp(otp);
            } else {
                Toast.makeText(this, "Please enter the 6-digit code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp(String otp) {
        setLoading(true);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = Config.VERIFY_OTP;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            JSONObject userData = jsonResponse.getJSONObject("data");
                            sessionManager.createLoginSession(
                                    userData.getInt("user_id"),
                                    userData.getString("pseudo"),
                                    userData.getString("numero")
                            );

                            // Verification successful! Navigate to MainActivity.
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, jsonResponse.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing server response.", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    setLoading(false);
                    Toast.makeText(this, "Network Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        ) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("numero", numero);
                params.put("otp_code", otp);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!isLoading);
    }
}