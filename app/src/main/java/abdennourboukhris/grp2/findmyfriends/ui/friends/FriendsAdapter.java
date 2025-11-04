package abdennourboukhris.grp2.findmyfriends.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import abdennourboukhris.grp2.findmyfriends.R;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    public enum AdapterType {
        FRIENDS_LIST,
        REQUESTS_LIST
    }

    private final List<Friend> friendList;
    private final AdapterType adapterType;
    private final OnFriendInteractionListener listener;

    // A single, more flexible interface
    public interface OnFriendInteractionListener {
        void onAccept(Friend friend);
        void onDecline(Friend friend);
        void onRequestLocation(Friend friend);
    }

    public FriendsAdapter(List<Friend> friendList, AdapterType type, OnFriendInteractionListener listener) {
        this.friendList = friendList;
        this.adapterType = type;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        holder.tvPseudo.setText(friend.getPseudo());
        holder.tvNumero.setText(friend.getNumero());

        // This is the new logic to show the correct buttons based on adapter type
        if (adapterType == AdapterType.REQUESTS_LIST) {
            holder.requestButtons.setVisibility(View.VISIBLE);
            holder.btnRequestLocation.setVisibility(View.GONE);
            holder.btnAccept.setOnClickListener(v -> listener.onAccept(friend));
            holder.btnDecline.setOnClickListener(v -> listener.onDecline(friend));
        } else { // FRIENDS_LIST
            holder.requestButtons.setVisibility(View.GONE);
            holder.btnRequestLocation.setVisibility(View.VISIBLE);
            holder.btnRequestLocation.setOnClickListener(v -> listener.onRequestLocation(friend));
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvPseudo, tvNumero;
        LinearLayout requestButtons;
        Button btnAccept, btnDecline;
        ImageButton btnRequestLocation;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPseudo = itemView.findViewById(R.id.tv_friend_pseudo);
            tvNumero = itemView.findViewById(R.id.tv_friend_numero);
            requestButtons = itemView.findViewById(R.id.layout_request_buttons);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            btnRequestLocation = itemView.findViewById(R.id.btn_request_location);
        }
    }
}