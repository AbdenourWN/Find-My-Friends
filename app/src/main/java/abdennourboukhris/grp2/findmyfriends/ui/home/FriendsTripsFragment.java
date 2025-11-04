package abdennourboukhris.grp2.findmyfriends.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class FriendsTripsFragment extends Fragment implements TripsAdapter.OnTripClickListener {

    private static final String TAG = "FriendsTripsFragment";
    private List<Trip> tripList = new ArrayList<>();
    private TripsAdapter tripsAdapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SessionManager sessionManager;
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends_trips, container, false);

        sessionManager = new SessionManager(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        progressBar = view.findViewById(R.id.progress_bar_friends_trips);
        tvEmpty = view.findViewById(R.id.tv_empty_friends_trips);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_friends_trips);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tripsAdapter = new TripsAdapter(tripList, this);
        recyclerView.setAdapter(tripsAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchFriendsTrips();
    }

    private void fetchFriendsTrips() {
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
                                tripList.add(new Trip(
                                        obj.getInt("trip_id"),
                                        obj.getString("trip_name"),
                                        obj.getString("friend_pseudo"),
                                        obj.getString("start_time")
                                ));
                            }
                            tripsAdapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(tripList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage());
                    }
                },
                error -> {
                    setLoading(false);
                    Log.e(TAG, "Volley Error: " + error.toString());
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "read_friends_trips");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                return params;
            }
        };
        requestQueue.add(request);
    }

    @Override
    public void onTripClick(Trip trip) {
        Bundle args = new Bundle();
        args.putInt("trip_id_to_view", trip.getTripId());
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.action_homeFragment_to_tripDetail, args);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        tvEmpty.setVisibility(View.GONE);
    }
}