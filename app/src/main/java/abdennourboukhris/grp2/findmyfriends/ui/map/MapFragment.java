package abdennourboukhris.grp2.findmyfriends.ui.map;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import abdennourboukhris.grp2.findmyfriends.R;
import abdennourboukhris.grp2.findmyfriends.databinding.FragmentMapBinding;

public class MapFragment extends Fragment {

    private MapView map = null;
    private FragmentMapBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment using View Binding
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- osmdroid Configuration ---
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireActivity().getPackageName());

        // --- Get the MapView from the Binding ---
        map = binding.mapView;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // --- Get Arguments Passed from Navigation ---
        float latitude = 0.0f;
        float longitude = 0.0f;
        String sender = "Unknown";
        if (getArguments() != null) {
            latitude = getArguments().getFloat("latitude");
            longitude = getArguments().getFloat("longitude");
            sender = getArguments().getString("sender");
        }

        // --- Center Map and Add Marker ---
        GeoPoint friendLocation = new GeoPoint(latitude, longitude);
        map.getController().setZoom(18.0);
        map.getController().setCenter(friendLocation);

        Marker locationMarker = new Marker(map);
        locationMarker.setPosition(friendLocation);
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        locationMarker.setTitle("Friend's Location");
        locationMarker.setSnippet("Received from: " + sender);
        map.getOverlays().add(locationMarker);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the binding reference
        binding = null;
    }
}