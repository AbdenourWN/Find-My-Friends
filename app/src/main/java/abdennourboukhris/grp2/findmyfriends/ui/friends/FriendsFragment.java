package abdennourboukhris.grp2.findmyfriends.ui.friends;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.R;
import abdennourboukhris.grp2.findmyfriends.SessionManager;

public class FriendsFragment extends Fragment {

    private static final String TAG = "FriendsFragment";
    private RequestQueue requestQueue;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());
        sessionManager = new SessionManager(requireContext());

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);
        FloatingActionButton fabAddFriend = view.findViewById(R.id.fab_add_friend);

        FriendsPagerAdapter pagerAdapter = new FriendsPagerAdapter(getChildFragmentManager(), getLifecycle());
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(position == 1 ? "Requests" : "My Friends")
        ).attach();

        fabAddFriend.setOnClickListener(v -> showAddFriendDialog());
    }

    private void showAddFriendDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.MyRoundedDialogTheme);

        builder.setTitle("Add Friend");
        builder.setMessage("Enter the phone number of the user you want to add.");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint("Phone Number");

        // Apply the same padding trick as in LoginActivity
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

        builder.setPositiveButton("Send Request", (dialog, which) -> {
            String numero = input.getText().toString().trim();
            if (!numero.isEmpty()) {
                findUserAndSendRequest(numero);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void findUserAndSendRequest(String numero) {
        String url = Config.USERS_CRUD;
        StringRequest findUserRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if ("success".equals(jsonResponse.getString("status")) && !jsonResponse.isNull("data")) {
                            JSONObject userData = jsonResponse.getJSONObject("data");
                            int friendId = userData.getInt("user_id");
                            sendFriendRequest(friendId);
                        } else {
                            Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Find User JSON Error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Find User Volley Error: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "find_by_numero");
                params.put("numero", numero);
                params.put("current_user_id", String.valueOf(sessionManager.getUserId()));
                return params;
            }
        };
        requestQueue.add(findUserRequest);
    }

    private void sendFriendRequest(int friendId) {
        String url = Config.FRIENDSHIPS_CRUD;
        StringRequest sendRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        Log.e(TAG, "Send Request JSON Error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Send Request Volley Error: " + error.toString())) {
            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "create");
                params.put("user_one_id", String.valueOf(sessionManager.getUserId()));
                params.put("user_two_id", String.valueOf(friendId));
                return params;
            }
        };
        requestQueue.add(sendRequest);
    }
}