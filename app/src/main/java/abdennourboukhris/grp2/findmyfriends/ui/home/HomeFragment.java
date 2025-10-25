package abdennourboukhris.grp2.findmyfriends.ui.home;

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

import org.json.JSONObject;

import abdennourboukhris.grp2.findmyfriends.Config;
import abdennourboukhris.grp2.findmyfriends.JSONParser;
import abdennourboukhris.grp2.findmyfriends.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

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

        @Override
        protected void onPreExecute() {
            //UIThread -> accees à l'interface
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
        return response;
        }

        @Override
        protected void onPostExecute(Object o) {
            //UIThread -> accees à l'interface
            super.onPostExecute(o);
        }
    }
}
