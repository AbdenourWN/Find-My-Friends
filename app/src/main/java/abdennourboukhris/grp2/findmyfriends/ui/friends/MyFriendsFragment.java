package abdennourboukhris.grp2.findmyfriends.ui.friends;

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
import androidx.fragment.app.Fragment;
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

public class MyFriendsFragment extends Fragment {

    private static final String TAG = "MyFriendsFragment";

    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private List<Friend> friendsList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_friends, container, false);

        // Initialize helpers and views
        requestQueue = Volley.newRequestQueue(requireContext());
        sessionManager = new SessionManager(requireContext());
        friendsList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.recycler_view_my_friends);
        progressBar = view.findViewById(R.id.progress_bar_my_friends);
        tvEmpty = view.findViewById(R.id.tv_empty_my_friends);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendsAdapter(friendsList, FriendsAdapter.AdapterType.FRIENDS_LIST);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Fetch friends every time the fragment is shown to keep the list up-to-date
        fetchFriends();
    }

    private void fetchFriends() {
        setLoading(true);
        String url = Config.FRIENDSHIPS_CRUD;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            friendsList.clear(); // Clear the old list before adding new data
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject friendObject = data.getJSONObject(i);
                                friendsList.add(new Friend(
                                        -1, // friendship_id is not needed for this list
                                        friendObject.getInt("user_id"),
                                        friendObject.getString("pseudo"),
                                        friendObject.getString("numero")
                                ));
                            }
                            adapter.notifyDataSetChanged();
                            checkIfEmpty();
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    setLoading(false);
                    Log.e(TAG, "Volley error: " + error.toString());
                    Toast.makeText(getContext(), "Network error.", Toast.LENGTH_SHORT).show();
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "read_friends");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void checkIfEmpty() {
        tvEmpty.setVisibility(friendsList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}