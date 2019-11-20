package com.cmput301f19t09.vibes.fragments.searchfragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cmput301f19t09.vibes.R;
import com.cmput301f19t09.vibes.models.User;
import com.cmput301f19t09.vibes.models.UserManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SearchListAdapter extends ArrayAdapter<User> implements Observer {
    protected List<User> data;
    protected User user;
    private Context context;

    public SearchListAdapter(Context context) {
        super(context, 0);
        this.context = context;
        this.user = UserManager.getCurrentUser();
        this.data = new ArrayList<User>();

        initialize();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View item = convertView;

        if (item == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            item = inflater.inflate(R.layout.search_list_item, null);

        }

        User user = data.get(position);

        if (user == null) {
            return item;
        }

        ImageView userProfile = item.findViewById(R.id.search_profile_picture);
        TextView userFullName = item.findViewById(R.id.search_full_name);
        TextView userUserName = item.findViewById(R.id.search_username);
        Glide.with(getContext()).load(user.getProfileURL()).into(userProfile);
        userProfile.setClipToOutline(true);

        userFullName.setText(user.getFirstName() + " " + user.getLastName());
        userUserName.setText(user.getUserName());
        return item;
    }

    public void refreshData() {
        clear();
        data = new ArrayList<User>();

        List<User> users = null;
        if (users == null) {
            return;
        }

        for (User user : users) {
            data.add(user);
        }

        data.sort(new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                return user1.getFirstName().compareTo(user2.getFirstName());
            }
        });

        addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public void update(Observable observable, Object o) {
        refreshData();
    }

    private void initialize() {
        UserManager.addUserObserver(user.getUid(), this);
        refreshData();
    }

    public void destroy() {
        UserManager.removeUserObserver(user.getUid(), this);
    }
}
