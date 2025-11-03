package abdennourboukhris.grp2.findmyfriends.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private final OnRequestInteractionListener listener;

    // Interface for handling button clicks in the RequestsFragment
    public interface OnRequestInteractionListener {
        void onAccept(Friend friend);
        void onDecline(Friend friend);
    }

    // Constructor for the Friends List (doesn't need a listener)
    public FriendsAdapter(List<Friend> friendList, AdapterType type) {
        this.friendList = friendList;
        this.adapterType = type;
        this.listener = null;
    }

    // Constructor for the Requests List (requires a listener)
    public FriendsAdapter(List<Friend> friendList, AdapterType type, OnRequestInteractionListener listener) {
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

        // This is the core logic: show buttons only for the REQUESTS_LIST
        if (adapterType == AdapterType.REQUESTS_LIST && listener != null) {
            holder.requestButtons.setVisibility(View.VISIBLE);
            holder.btnAccept.setOnClickListener(v -> listener.onAccept(friend));
            holder.btnDecline.setOnClickListener(v -> listener.onDecline(friend));
        } else {
            holder.requestButtons.setVisibility(View.GONE);
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

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPseudo = itemView.findViewById(R.id.tv_friend_pseudo);
            tvNumero = itemView.findViewById(R.id.tv_friend_numero);
            requestButtons = itemView.findViewById(R.id.layout_request_buttons);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }
    }
}