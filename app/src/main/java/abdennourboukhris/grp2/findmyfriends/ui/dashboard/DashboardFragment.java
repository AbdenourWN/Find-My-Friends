package abdennourboukhris.grp2.findmyfriends.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import abdennourboukhris.grp2.findmyfriends.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST_CODE = 101;
    private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        binding.btnSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String destinationNumber = binding.edNumberFd.getText().toString();
                String smsMessage = "findFriends: Send me your location";
                if (destinationNumber.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> permissionsToRequest = new ArrayList<>();
                permissionsToRequest.add(Manifest.permission.SEND_SMS);
                permissionsToRequest.add(Manifest.permission.RECEIVE_SMS);
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);

                // Add Notification permission only if on Android 13 (TIRAMISU) or higher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
                }

                List<String> permissionsNotGranted = new ArrayList<>();
                for (String permission : permissionsToRequest) {
                    if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                        permissionsNotGranted.add(permission);
                    }
                }

                if (permissionsNotGranted.isEmpty()) {
                    // All permissions are already granted
                    sendSmsRequest(destinationNumber);
                } else {
                    // Request the permissions that are not granted
                    requestPermissions(permissionsNotGranted.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
                }
            }
        });

        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // Check if all permissions were granted
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // User granted all permissions, now we can try to send the SMS again
                String destinationNumber = binding.edNumberFd.getText().toString().trim();
                sendSmsRequest(destinationNumber);
            } else {
                // User denied one or more permissions. Show a message explaining why the feature is disabled.
                Toast.makeText(getContext(), "Permissions are required to send location requests.", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void sendSmsRequest(String destinationNumber) {
        String smsMessage = "findFriends: Send me your location";
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(destinationNumber, null, smsMessage, null, null);
            Toast.makeText(getContext(), "Location request sent!", Toast.LENGTH_SHORT).show();
            binding.edNumberFd.setText("");
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send SMS.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}