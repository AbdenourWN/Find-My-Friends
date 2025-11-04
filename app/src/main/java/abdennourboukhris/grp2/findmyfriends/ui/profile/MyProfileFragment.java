package abdennourboukhris.grp2.findmyfriends.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.LoginActivity;
import abdennourboukhris.grp2.findmyfriends.R;
import abdennourboukhris.grp2.findmyfriends.SessionManager;

public class MyProfileFragment extends Fragment {

    private static final String TAG = "MyProfileFragment";

    private TextInputEditText etPseudo;
    private TextInputEditText etNumero;
    private Button btnSaveChanges;
    private Button btnLogout;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize helpers and views
        sessionManager = new SessionManager(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        etPseudo = view.findViewById(R.id.et_pseudo);
        etNumero = view.findViewById(R.id.et_numero);
        btnSaveChanges = view.findViewById(R.id.btn_save_changes);
        btnLogout = view.findViewById(R.id.btn_logout);
        progressBar = view.findViewById(R.id.progress_bar_profile);

        // Populate the fields with data from the session
        populateUserData();

        // Set listeners
        btnSaveChanges.setOnClickListener(v -> handleSaveChanges());
        btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void populateUserData() {
        etPseudo.setText(sessionManager.getUserPseudo());
        etNumero.setText(sessionManager.getUserNumero());
    }

    private void handleSaveChanges() {
        String newPseudo = etPseudo.getText().toString().trim();
        if (newPseudo.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        String url = Config.USERS_CRUD;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            // Update the pseudo in the local session
                            sessionManager.createLoginSession(
                                    sessionManager.getUserId(),
                                    newPseudo,
                                    sessionManager.getUserNumero()
                            );
                            Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Update Pseudo JSON Error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing server response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    setLoading(false);
                    Log.e(TAG, "Update Pseudo Volley Error: " + error.toString());
                    Toast.makeText(getContext(), "Network error.", Toast.LENGTH_SHORT).show();
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "update");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                params.put("pseudo", newPseudo);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void handleLogout() {
        sessionManager.logoutUser();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveChanges.setEnabled(!isLoading);
        btnLogout.setEnabled(!isLoading);
    }
}