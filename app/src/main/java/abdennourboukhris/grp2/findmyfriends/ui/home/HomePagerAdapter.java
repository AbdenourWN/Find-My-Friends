package abdennourboukhris.grp2.findmyfriends.ui.home;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class HomePagerAdapter extends FragmentStateAdapter {

    public HomePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new MyTripsFragment();
        }
        return new FriendsTripsFragment();
    }

    @Override
    public int getItemCount() {
        return 2; // We have two tabs
    }
}