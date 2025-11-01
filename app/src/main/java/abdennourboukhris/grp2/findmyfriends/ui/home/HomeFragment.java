package abdennourboukhris.grp2.findmyfriends.ui.home;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.JSONParser;
import abdennourboukhris.grp2.findmyfriends.Position;
import abdennourboukhris.grp2.findmyfriends.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    public ArrayList<Position> positions = new ArrayList<>();

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Download download = new Download();
                download.execute();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    class Download extends AsyncTask {
        AlertDialog alert;

        @Override
        protected void onPreExecute() {
            //UIThread -> accees à l'interface
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeFragment.this.getContext());
            builder.setTitle("Downloading...");
            builder.setMessage("Wait Please...");

            alert = builder.create();
            alert.show();
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            //second thread -> pas d'accees l'interface
            JSONParser parser = new JSONParser();
            JSONObject response = parser.makeHttpRequest(
                    Config.URL_GetAll_Location,
                    "GET",
                    null
            );
            Log.e("response", response.toString());

            try {
                int success = response.getInt("success");
                if (success == 1) {
                    JSONArray result = response.getJSONArray("positions");
                    positions.clear();
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject ligne = result.getJSONObject(i);
                        int Idposition = ligne.getInt("idposition");
                        double Latitude = ligne.getDouble("latitude");
                        double Longitude = ligne.getDouble("longitude");
                        String numero = ligne.getString("numero");
                        String Pseudo = ligne.getString("pseudo");
                        Position position = new Position(Idposition, Latitude, Longitude, numero, Pseudo);
                        positions.add(position);
                    }

                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(Object o) {
            //UIThread -> accees à l'interface
            super.onPostExecute(o);
            if (!positions.isEmpty()) {
                ArrayAdapter<Position> adapter = new ArrayAdapter<>(HomeFragment.this.getContext(), android.R.layout.simple_list_item_1, positions);
                binding.lvHome.setAdapter(adapter);
            }
            alert.dismiss();


        }
    }
}
