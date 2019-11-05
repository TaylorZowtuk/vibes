package com.cmput301f19t09.vibes.fragments.moodlistfragment;

import android.content.Context;
import android.util.Log;

import com.cmput301f19t09.vibes.models.User;

import java.util.ArrayList;

public class FollowedMoodListAdapter extends MoodListAdapter
{
    public FollowedMoodListAdapter(Context context, User user)
    {
        super(context, user);
    }

    @Override
    protected void initializeData()
    {
        data = new ArrayList<MoodListItem>();
        for (String followed_username : user.getFollowingList())
        {
            User followed_user = new User(followed_username);

            user.exists(new User.UserExistListener()
            {
                @Override
                public void onUserExists()
                {
                    addUser(followed_user);
                }

                @Override
                public void onUserNotExists()
                {

                }
            });
        }
    }

    private void addUser(User user)
    {
        data.add(new MoodListItem(user, null));

        user.readData(new User.FirebaseCallback()
        {
            @Override
            public void onCallback(User user)
            {
                setMoodEvent(user);
            }
        });
    }

    private void setMoodEvent(User user)
    {
        for (MoodListItem item : data)
        {
            if (item.user == user)
            {
                Log.d("BBBBBBBBBBBBBBBBBBBBBBBB", "Added mood evetn");
                item.event = user.getMostRecentMoodEvent();
                Log.d("CCCCCCCCCCCCCCCCCCCCCCCC", item.event.getDescription());
                break;
            }
        }

        notifyDataSetChanged();
    }
}
