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

public class RequestsFragment extends Fragment implements FriendsAdapter.OnRequestInteractionListener {

    private static final String TAG = "RequestsFragment";

    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private List<Friend> requestsList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        requestQueue = Volley.newRequestQueue(requireContext());
        sessionManager = new SessionManager(requireContext());
        requestsList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.recycler_view_requests);
        progressBar = view.findViewById(R.id.progress_bar_requests);
        tvEmpty = view.findViewById(R.id.tv_empty_requests);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendsAdapter(requestsList, FriendsAdapter.AdapterType.REQUESTS_LIST, this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchRequests();
    }

    private void fetchRequests() {
        setLoading(true);
        String url = Config.FRIENDSHIPS_CRUD;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            requestsList.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject reqObject = data.getJSONObject(i);
                                requestsList.add(new Friend(
                                        reqObject.getInt("friendship_id"),
                                        reqObject.getInt("user_id"),
                                        reqObject.getString("pseudo"),
                                        reqObject.getString("numero")
                                ));
                            }
                            adapter.notifyDataSetChanged();
                            checkIfEmpty();
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    setLoading(false);
                    Log.e(TAG, "Volley error: " + error.toString());
                }) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "read_pending");
                params.put("user_id", String.valueOf(sessionManager.getUserId()));
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    @Override
    public void onAccept(Friend friend) {
        updateRequestStatus(friend, "accepted");
    }

    @Override
    public void onDecline(Friend friend) {
        updateRequestStatus(friend, "delete");
    }

    private void updateRequestStatus(Friend friend, String newStatusOrAction) {
        String url = Config.FRIENDSHIPS_CRUD;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status"))) {
                            // Remove the item from the list for a smooth UI update
                            int position = requestsList.indexOf(friend);
                            if (position != -1) {
                                requestsList.remove(position);
                                adapter.notifyItemRemoved(position);
                                checkIfEmpty();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing error on update: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Volley error on update: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (newStatusOrAction.equals("delete")) {
                    params.put("action", "delete");
                } else {
                    params.put("action", "update_status");
                    params.put("status", newStatusOrAction);
                }
                params.put("friendship_id", String.valueOf(friend.getFriendshipId()));
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
        tvEmpty.setVisibility(requestsList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}