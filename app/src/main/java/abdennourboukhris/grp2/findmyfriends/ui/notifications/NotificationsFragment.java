package abdennourboukhris.grp2.findmyfriends.ui.notifications;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.JSONParser;
import abdennourboukhris.grp2.findmyfriends.Position;
import abdennourboukhris.grp2.findmyfriends.databinding.FragmentNotificationsBinding;
import abdennourboukhris.grp2.findmyfriends.ui.home.HomeFragment;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddLocation addLocation = new AddLocation();
                addLocation.execute();
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    class AddLocation extends AsyncTask{

        AlertDialog alert;

        @Override
        protected void onPreExecute() {
            //UIThread -> accees à l'interface
            AlertDialog.Builder builder = new AlertDialog.Builder(NotificationsFragment.this.getContext());
            builder.setTitle("Adding Location...");
            builder.setMessage("Wait Please...");

            alert = builder.create();
            alert.show();
            super.onPreExecute();
        }


        @Override
        protected Object doInBackground(Object[] objects) {
            //second thread -> pas d'accees l'interface
            JSONParser parser = new JSONParser();

            String pseudoStr = binding.etPseudo.getText().toString().trim();
            String numeroStr = binding.etNumero.getText().toString().trim();
            String latStr = binding.etLatitude.getText().toString().trim();
            String lonStr = binding.etLongitude.getText().toString().trim();

            if (pseudoStr.isEmpty() || numeroStr.isEmpty() || latStr.isEmpty() || lonStr.isEmpty()) {
                Log.e("response:", "invalid inputs");

                // You might want to return a specific error JSON or null here
                return null;
            }

            HashMap<String, String> params = new HashMap<>();
            params.put("pseudo", pseudoStr);
            params.put("numero", numeroStr);
            params.put("longitude", lonStr);
            params.put("latitude", latStr);
            Log.e("response:",params.toString());


            JSONObject response = parser.makeHttpRequest(
                    Config.URL_POST_Location,
                    "POST",
                    params
            );

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            alert.dismiss();

        }

    }
}