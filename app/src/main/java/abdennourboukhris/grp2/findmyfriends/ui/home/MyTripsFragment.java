package abdennourboukhris.grp2.findmyfriends.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.R;
import abdennourboukhris.grp2.findmyfriends.SessionManager;

public class MyTripsFragment extends Fragment implements MyTripsAdapter.OnTripInteractionListener {

    private static final String TAG = "MyTripsFragment";

    private RecyclerView recyclerView;
    private MyTripsAdapter adapter;
    private List<Trip> tripList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private SessionManager sessionManager;
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_trips, container, false);

        sessionManager = new SessionManager(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        recyclerView = view.findViewById(R.id.recycler_view_my_trips);
        progressBar = view.findViewById(R.id.progress_bar_my_trips);
        tvEmpty = view.findViewById(R.id.tv_empty_my_trips);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyTripsAdapter(tripList, this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchMyTrips();
    }

    private void fetchMyTrips() {
        setLoading(true);
        String url = Config.TRIPS_CRUD;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            tripList.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject obj = data.getJSONObject(i);
                                // The 'friendPseudo' field isn't needed here, so we can pass null or an empty string
                                tripList.add(new Trip(
                                        obj.getInt("trip_id"),
                                        obj.getString("trip_name"),
                                        "",
                                        obj.getString("start_time")
                                ));
                            }
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(tripList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Fetch Trips JSON Error: " + e.getMessage());
                    }
                },
                error -> {
                    setLoading(false);
                    Log.e(TAG, "Fetch Trips Volley Error: " + error.toString());
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "read_for_user");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                return params;
            }
        };
        requestQueue.add(request);
    }

    @Override
    public void onTripClicked(Trip trip) {
        Bundle args = new Bundle();
        args.putInt("trip_id_to_view", trip.getTripId());

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.action_homeFragment_to_tripDetail, args);
    }

    @Override
    public void onDeleteClicked(Trip trip) {
        new AlertDialog.Builder(requireContext(), R.style.MyRoundedDialogTheme)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to permanently delete '" + trip.getTripName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTripFromServer(trip))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTripFromServer(Trip trip) {
        String url = Config.TRIPS_CRUD;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            Toast.makeText(getContext(), "Trip deleted.", Toast.LENGTH_SHORT).show();
                            int position = tripList.indexOf(trip);
                            if (position != -1) {
                                tripList.remove(position);
                                adapter.notifyItemRemoved(position);
                                tvEmpty.setVisibility(tripList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Delete Trip JSON Error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Delete Trip Volley Error: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "delete");
                params.put("trip_id", String.valueOf(trip.getTripId()));
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}