package abdennourboukhris.grp2.findmyfriends.ui.map;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.Position;
import abdennourboukhris.grp2.findmyfriends.R;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RequestQueue requestQueue;
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
        // It's safer to initialize the queue here, tied to the fragment's lifecycle
        requestQueue = Volley.newRequestQueue(requireContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        fetchAllLocations();
    }

    private void fetchAllLocations() {
        if (mMap == null) return;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, Config.URL_GetAll_Location, null,
                response -> {
                    List<Position> positionList = new ArrayList<>();
                    try {
                        if (response.getInt("success") == 1) {
                            JSONArray positionsArray = response.getJSONArray("positions");
                            for (int i = 0; i < positionsArray.length(); i++) {
                                JSONObject posObject = positionsArray.getJSONObject(i);
                                positionList.add(new Position(
                                        posObject.getDouble("latitude"),
                                        posObject.getDouble("longitude"),
                                        // --- THIS IS THE CRITICAL FIX ---
                                        // Read the phone number as a String to prevent the crash
                                        posObject.getString("numero"),
                                        posObject.getString("pseudo")
                                ));
                            }
                            displayAllMarkersAndFocus(positionList);
                        } else {
                            Log.e("MapFragment", "Server responded with success=0");
                            Toast.makeText(getContext(), "Server error: Could not fetch locations.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("MapFragment", "JSON Parsing error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing server data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("MapFragment", "Volley error: " + error.toString());
                    Toast.makeText(getContext(), "Could not connect to server", Toast.LENGTH_SHORT).show();
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void displayAllMarkersAndFocus(List<Position> positionList) {
        if (mMap == null) return; // Add a safety check
        mMap.clear();

        if (positionList.isEmpty()) {
            Toast.makeText(getContext(), "No saved locations found.", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (Position pos : positionList) {
            LatLng latLng = new LatLng(pos.getLatitude(), pos.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title(pos.getPseudo()).snippet("Tel: " + pos.getNumero()));
            boundsBuilder.include(latLng);
        }

        if (arguments != null && arguments.containsKey("focus_latitude")) {
            float lat = arguments.getFloat("focus_latitude");
            float lon = arguments.getFloat("focus_longitude");
            LatLng focusPoint = new LatLng(lat, lon);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(focusPoint, 15f), 1500, null);
            arguments = null; // Important: Consume the arguments
        } else {
            LatLngBounds bounds = boundsBuilder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }
}