package abdennourboukhris.grp2.findmyfriends.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import abdennourboukhris.grp2.findmyfriends.R;

public class TripsAdapter extends RecyclerView.Adapter<TripsAdapter.TripViewHolder> {

    private final List<Trip> tripList;
    private final OnTripClickListener listener;

    // Interface to handle clicks on a trip item
    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public TripsAdapter(List<Trip> tripList, OnTripClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip, listener);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    // ViewHolder class
    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripName;
        TextView tvTripBy;
        TextView tvTripDate;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tv_trip_name);
            tvTripBy = itemView.findViewById(R.id.tv_trip_by);
            tvTripDate = itemView.findViewById(R.id.tv_trip_date);
        }

        public void bind(final Trip trip, final OnTripClickListener listener) {
            tvTripName.setText(trip.getTripName());
            tvTripBy.setText("by " + trip.getFriendPseudo());
            tvTripDate.setText(trip.getStartTime()); // You could format this date nicely later

            itemView.setOnClickListener(v -> listener.onTripClick(trip));
        }
    }
}