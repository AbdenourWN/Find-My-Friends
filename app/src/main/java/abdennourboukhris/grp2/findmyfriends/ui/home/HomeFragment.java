package abdennourboukhris.grp2.findmyfriends.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.R;
import abdennourboukhris.grp2.findmyfriends.SessionManager;
import abdennourboukhris.grp2.findmyfriends.TripTrackingService;

public class HomeFragment extends Fragment{

    private static final String TAG = "HomeFragment";

    private Button btnToggleTrip;
    private SessionManager sessionManager;
    private RequestQueue requestQueue;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        // Setup ViewPager
        HomePagerAdapter homePagerAdapter = new HomePagerAdapter(this);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager_home);
        viewPager.setAdapter(homePagerAdapter);

        // Link TabLayout with ViewPager
        TabLayout tabLayout = view.findViewById(R.id.tab_layout_home);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 1 ? "My Trips" : "Friends' Trips")
        ).attach();

        // Setup the Start/Stop Trip Button
        btnToggleTrip = view.findViewById(R.id.btn_toggle_trip);
        updateButtonState();
        btnToggleTrip.setOnClickListener(v -> {
            if (TripTrackingService.isRunning) {
                stopTrackingService();
            } else {
                showStartTripDialog();
            }
        });
    }
    private void showStartTripDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.MyRoundedDialogTheme);
        builder.setTitle("Start a New Trip");
        builder.setMessage("Please enter a name for your trip (e.g., 'Morning Run', 'Road Trip').");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Trip Name");

        // Add padding to the EditText
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Start", (dialog, which) -> {
            String tripName = input.getText().toString().trim();
            if (tripName.isEmpty()) {
                Toast.makeText(getContext(), "Trip name cannot be empty.", Toast.LENGTH_SHORT).show();
            } else {
                createTripOnServer(tripName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createTripOnServer(String tripName) {
        String url = Config.TRIPS_CRUD;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            // Trip created successfully on the server, get the new trip_id
                            int newTripId = jsonResponse.getJSONObject("data").getInt("trip_id");
                            Toast.makeText(getContext(), "Trip started!", Toast.LENGTH_SHORT).show();
                            // Now, start the foreground service with the new trip ID
                            startTrackingService(newTripId);
                            btnToggleTrip.postDelayed(this::updateButtonState, 500);
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Create Trip JSON Error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error starting trip.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Create Trip Volley Error: " + error.toString());
                    Toast.makeText(getContext(), "Network error starting trip.", Toast.LENGTH_SHORT).show();
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "create");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                params.put("trip_name", tripName);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void startTrackingService(int tripId) {
        if (getActivity() == null) return;

        Intent serviceIntent = new Intent(getActivity(), TripTrackingService.class);
        serviceIntent.setAction(TripTrackingService.ACTION_START_TRIP);
        serviceIntent.putExtra(TripTrackingService.EXTRA_TRIP_ID, tripId);
        requireActivity().startService(serviceIntent);
        updateButtonState();
    }

    private void updateButtonState() {
        if (TripTrackingService.isRunning) {
            btnToggleTrip.setText("Stop Current Trip");
            btnToggleTrip.setBackgroundColor(Color.RED);
        } else {
            btnToggleTrip.setText("Start New Trip");
            // Set back to default theme color
            btnToggleTrip.setBackgroundColor(MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnBackground, Color.BLUE));
        }
    }

    private void stopTrackingService() {
        Intent serviceIntent = new Intent(getActivity(), TripTrackingService.class);
        serviceIntent.setAction(TripTrackingService.ACTION_STOP_TRIP);
        getActivity().startService(serviceIntent);

        // Give the service a moment to update its static flag
        btnToggleTrip.postDelayed(this::updateButtonState, 500);
        Toast.makeText(getContext(), "Trip Stoped!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateButtonState();
    }
}