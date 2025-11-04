package abdennourboukhris.grp2.findmyfriends.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import abdennourboukhris.grp2.findmyfriends.R;

public class MyTripsAdapter extends RecyclerView.Adapter<MyTripsAdapter.MyTripViewHolder> {

    private final List<Trip> tripList;
    private final OnTripInteractionListener listener;

    public interface OnTripInteractionListener {
        void onTripClicked(Trip trip);
        void onDeleteClicked(Trip trip);
    }

    public MyTripsAdapter(List<Trip> tripList, OnTripInteractionListener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyTripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_trip, parent, false);
        return new MyTripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyTripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip, listener);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class MyTripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripName, tvTripDate;
        ImageButton btnDelete;

        public MyTripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tv_my_trip_name);
            tvTripDate = itemView.findViewById(R.id.tv_my_trip_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_trip);
        }

        public void bind(final Trip trip, final OnTripInteractionListener listener) {
            tvTripName.setText(trip.getTripName());
            tvTripDate.setText(trip.getStartTime());

            itemView.setOnClickListener(v -> listener.onTripClicked(trip));
            btnDelete.setOnClickListener(v -> listener.onDeleteClicked(trip));
        }
    }
}