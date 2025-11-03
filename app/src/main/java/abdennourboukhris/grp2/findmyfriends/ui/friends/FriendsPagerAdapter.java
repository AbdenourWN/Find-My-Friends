package abdennourboukhris.grp2.findmyfriends.ui.friends;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class FriendsPagerAdapter extends FragmentStateAdapter {

    public FriendsPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a new fragment instance for the given position.
        if (position == 1) {
            // This is the "Requests" tab
            return new RequestsFragment();
        }
        // This is the "My Friends" tab (position 0)
        return new MyFriendsFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}