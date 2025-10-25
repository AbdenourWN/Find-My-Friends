package abdennourboukhris.grp2.findmyfriends.ui.GoogleMap;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import abdennourboukhris.grp2.findmyfriends.R;
import abdennourboukhris.grp2.findmyfriends.databinding.ActivityGoogleMapsBinding;

public class GoogleMaps extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityGoogleMapsBinding binding;
    private double latitude;
    private double longitude;
    private String sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGoogleMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // recuperer les donnees de l intent depuis la notification
        Intent intent = getIntent();
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
        sender = intent.getStringExtra("sender");


        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Coordonnées invalides", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        // Créer l'objet LatLng avec les coordonnées reçues
        LatLng userPosition = new LatLng(latitude, longitude);


        String markerTitle = "Position de " + (sender != null ? sender : "utilisateur");

        mMap.addMarker(new MarkerOptions()
                .position(userPosition)
                .title(markerTitle)
                .snippet("Lat: " + latitude + ", Lng: " + longitude));

        // Déplacer et zoomer la caméra sur la position
        // Zoom level: 1 = World, 5 = Landmass/continent, 10 = City, 15 = Streets, 20 = Buildings
        float zoomLevel = 15.0f; // Bon niveau pour voir les rues
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, zoomLevel));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, zoomLevel), 1000, null);

        Toast.makeText(this, "Position de " + sender + " affichée", Toast.LENGTH_SHORT).show();
    }

}