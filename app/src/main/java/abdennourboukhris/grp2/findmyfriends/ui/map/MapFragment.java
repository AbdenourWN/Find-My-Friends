package abdennourboukhris.grp2.findmyfriends.ui.map;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.R;
import abdennourboukhris.grp2.findmyfriends.SessionManager;
import abdennourboukhris.grp2.findmyfriends.ui.friends.Friend;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";

    private GoogleMap mMap;
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    private ProgressBar progressBar;
    private FloatingActionButton fabFilter;

    private List<Friend> friendsList = new ArrayList<>();
    private List<Friend> selectedFriends = new ArrayList<>();
    private String recencyFilter = "recent";
    private Calendar startDate = null;
    private Calendar endDate = null;

    private Bundle arguments;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.arguments = getArguments();
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());
        sessionManager = new SessionManager(requireContext());

        progressBar = view.findViewById(R.id.progress_bar_map);
        fabFilter = view.findViewById(R.id.fab_filter);
        fabFilter.setOnClickListener(v -> showFilterDialog());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Only fetch friends for filtering if we are not viewing a specific trip
        if (arguments == null || !arguments.containsKey("trip_id_to_view")) {
            fetchFriendListForFilter();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // --- NEW ROUTING LOGIC ---
        if (arguments != null && arguments.containsKey("trip_id_to_view")) {
            // A trip was selected, so we are in "Trip View" mode.
            int tripId = arguments.getInt("trip_id_to_view");
            fabFilter.setVisibility(View.GONE); // Hide filter button in trip view mode
            fetchTripPath(tripId);
        } else {
            // No trip was selected, so we are in "Live Location" mode.
            fabFilter.setVisibility(View.VISIBLE);
            fetchFilteredLocations();
        }
    }

    /**
     * NEW METHOD: Fetches all location points for a specific trip and draws a Polyline.
     */
    private void fetchTripPath(int tripId) {
        if (mMap == null) return;
        setLoading(true);
        mMap.clear();

        String url = Config.LOCATION_POINTS_CRUD;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            if (data.length() > 0) {
                                drawTripOnMap(data);
                            } else {
                                Toast.makeText(getContext(), "This trip has no location points.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Fetch Trip Path JSON Error: " + e.getMessage());
                    }
                },
                error -> {
                    setLoading(false);
                    Log.e(TAG, "Fetch Trip Path Volley Error: " + error.toString());
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "read_for_trip");
                params.put("trip_id", String.valueOf(tripId));
                return params;
            }
        };
        requestQueue.add(request);
    }

    /**
     * NEW METHOD: Takes an array of points and draws markers and a line on the map.
     */
    private void drawTripOnMap(JSONArray points) throws JSONException {
        PolylineOptions polylineOptions = new PolylineOptions().color(Color.BLUE).width(10);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        // Add Start Marker
        JSONObject startPoint = points.getJSONObject(0);
        LatLng startLatLng = new LatLng(startPoint.getDouble("latitude"), startPoint.getDouble("longitude"));
        mMap.addMarker(new MarkerOptions().position(startLatLng).title("Trip Start").snippet(startPoint.getString("created_at")));

        for (int i = 0; i < points.length(); i++) {
            JSONObject point = points.getJSONObject(i);
            LatLng latLng = new LatLng(point.getDouble("latitude"), point.getDouble("longitude"));
            polylineOptions.add(latLng);
            boundsBuilder.include(latLng);
        }

        // Add End Marker (if more than one point exists)
        if (points.length() > 1) {
            JSONObject endPoint = points.getJSONObject(points.length() - 1);
            LatLng endLatLng = new LatLng(endPoint.getDouble("latitude"), endPoint.getDouble("longitude"));
            mMap.addMarker(new MarkerOptions().position(endLatLng).title("Trip End").snippet(endPoint.getString("created_at")));
        }

        // Add the polyline to the map
        mMap.addPolyline(polylineOptions);

        // Zoom the camera to fit the entire trip path
        LatLngBounds bounds = boundsBuilder.build();
        int padding = 200; // padding in pixels
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    // ... ALL OTHER METHODS for live location filtering (fetchFilteredLocations, addMarkersAndFocus, showFilterDialog, etc.) remain exactly the same as before.

    // [Paste the rest of your MapFragment code here, starting from fetchFilteredLocations()]
    // ...
    private void fetchFilteredLocations() {
        if (mMap == null) return;
        setLoading(true);
        mMap.clear(); // Clear old markers before fetching new ones

        String url = Config.GET_FILTERED_LOCATIONS;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            if (data.length() == 0) {
                                Toast.makeText(getContext(), "No locations found for the selected filters.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            addMarkersAndFocus(data);
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Fetch Locations JSON Error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing location data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    setLoading(false);
                    Log.e(TAG, "Fetch Locations Volley Error: " + error.toString());
                    Toast.makeText(getContext(), "Network error while fetching locations.", Toast.LENGTH_SHORT).show();
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                params.put("recency_filter", recencyFilter);

                if (!selectedFriends.isEmpty()) {
                    String friendIds = selectedFriends.stream()
                            .map(friend -> String.valueOf(friend.getUserId()))
                            .collect(Collectors.joining(","));
                    params.put("friend_ids", friendIds);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                if (startDate != null) {
                    params.put("start_date", sdf.format(startDate.getTime()));
                }
                if (endDate != null) {
                    params.put("end_date", sdf.format(endDate.getTime()));
                }

                Log.d(TAG, "Requesting locations with params: " + params);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void addMarkersAndFocus(JSONArray data) throws JSONException {
        if (mMap == null || data.length() == 0) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (int i = 0; i < data.length(); i++) {
            JSONObject loc = data.getJSONObject(i);
            LatLng position = new LatLng(loc.getDouble("latitude"), loc.getDouble("longitude"));
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(loc.getString("pseudo"))
                    .snippet("At: " + loc.getString("created_at")));
            boundsBuilder.include(position);
        }

        if (arguments != null && arguments.containsKey("focus_latitude")) {
            float lat = arguments.getFloat("focus_latitude");
            float lon = arguments.getFloat("focus_longitude");
            LatLng focusPoint = new LatLng(lat, lon);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(focusPoint, 15f), 1500, null);
            arguments = null;
        } else {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 150;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.MyRoundedDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_map_filters, null);
        builder.setView(dialogView);

        TextView tvSelectedFriends = dialogView.findViewById(R.id.tv_selected_friends);
        RadioGroup rgRecency = dialogView.findViewById(R.id.rg_recency);
        View layoutDateFilters = dialogView.findViewById(R.id.layout_date_filters);
        Button btnStartDate = dialogView.findViewById(R.id.btn_start_date);
        Button btnEndDate = dialogView.findViewById(R.id.btn_end_date);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_filters);
        Button btnApply = dialogView.findViewById(R.id.btn_apply_filters);

        updateSelectedFriendsTextView(tvSelectedFriends);
        rgRecency.check(recencyFilter.equals("recent") ? R.id.rb_recent : R.id.rb_all);
        layoutDateFilters.setVisibility(recencyFilter.equals("all") ? View.VISIBLE : View.GONE);
        updateDateButtonTextView(btnStartDate, startDate, "Start Date");
        updateDateButtonTextView(btnEndDate, endDate, "End Date");

        tvSelectedFriends.setOnClickListener(v -> showFriendSelectionDialog(tvSelectedFriends));
        rgRecency.setOnCheckedChangeListener((group, checkedId) -> {
            layoutDateFilters.setVisibility(checkedId == R.id.rb_all ? View.VISIBLE : View.GONE);
        });
        btnStartDate.setOnClickListener(v -> showDatePickerDialog(true, btnStartDate));
        btnEndDate.setOnClickListener(v -> showDatePickerDialog(false, btnEndDate));

        final AlertDialog dialog = builder.create();

        btnClear.setOnClickListener(v -> {
            selectedFriends.clear();
            recencyFilter = "recent";
            startDate = null;
            endDate = null;
            fetchFilteredLocations();
            dialog.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            recencyFilter = rgRecency.getCheckedRadioButtonId() == R.id.rb_recent ? "recent" : "all";
            fetchFilteredLocations();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void fetchFriendListForFilter() {
        String url = Config.FRIENDSHIPS_CRUD;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            friendsList.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject obj = data.getJSONObject(i);
                                friendsList.add(new Friend(-1, obj.getInt("user_id"), obj.getString("pseudo"), obj.getString("numero")));
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Fetch Friends JSON Error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Fetch Friends Volley Error: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "read_friends");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void showFriendSelectionDialog(TextView tvSelectedFriends) {
        if (friendsList.isEmpty()) {
            Toast.makeText(getContext(), "You have no friends to filter by.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] friendNames = friendsList.stream().map(Friend::getPseudo).toArray(String[]::new);
        boolean[] checkedItems = new boolean[friendsList.size()];
        List<Friend> tempSelectedFriends = new ArrayList<>(selectedFriends);

        for (int i = 0; i < friendsList.size(); i++) {
            Friend currentFriend = friendsList.get(i);
            checkedItems[i] = tempSelectedFriends.stream().anyMatch(f -> f.getUserId() == currentFriend.getUserId());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Friends")
                .setMultiChoiceItems(friendNames, checkedItems, (dialog, which, isChecked) -> {
                    Friend friend = friendsList.get(which);
                    if (isChecked) {
                        tempSelectedFriends.add(friend);
                    } else {
                        tempSelectedFriends.removeIf(f -> f.getUserId() == friend.getUserId());
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    selectedFriends = new ArrayList<>(tempSelectedFriends);
                    updateSelectedFriendsTextView(tvSelectedFriends);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showDatePickerDialog(boolean isStartDate, Button dateButton) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            if (isStartDate) {
                selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                selectedDate.set(Calendar.MINUTE, 0);
                selectedDate.set(Calendar.SECOND, 0);
                startDate = selectedDate;
            } else {
                selectedDate.set(Calendar.HOUR_OF_DAY, 23);
                selectedDate.set(Calendar.MINUTE, 59);
                selectedDate.set(Calendar.SECOND, 59);
                endDate = selectedDate;
            }
            updateDateButtonTextView(dateButton, selectedDate, "");
        };

        new DatePickerDialog(requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateSelectedFriendsTextView(TextView textView) {
        if (selectedFriends.isEmpty()) {
            textView.setText("All Friends");
        } else {
            String selectedNames = selectedFriends.stream().map(Friend::getPseudo).collect(Collectors.joining(", "));
            textView.setText(selectedNames);
        }
    }

    private void updateDateButtonTextView(Button button, Calendar calendar, String defaultText) {
        if (calendar != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            button.setText(sdf.format(calendar.getTime()));
        } else {
            button.setText(defaultText);
        }
    }
}