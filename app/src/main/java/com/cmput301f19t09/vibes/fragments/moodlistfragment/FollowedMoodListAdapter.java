package com.cmput301f19t09.vibes.fragments.moodlistfragment;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cmput301f19t09.vibes.models.MoodEvent;
import com.cmput301f19t09.vibes.models.User;
import com.cmput301f19t09.vibes.models.UserManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/*
Subclass of MoodListAdapter, this loads the each of the most recent MoodEvents of the User that the
current user follows
 */
public class FollowedMoodListAdapter extends MoodListAdapter implements Observer
{
    interface FollowedUserListener extends Observer
    {
        @Override
        void update(Observable o, Object arg);
    }

    // Maintain a list of the UIDs of users that this user observes
    private List<String> observed_users;
    private boolean filterChanged;

    public FollowedMoodListAdapter(Context context)
    {
        super(context);
        filterChanged = false;
    }

    /*
    initialize the observed users list and call refresh data
     */
    @Override
    public void initializeData()
    {
       observed_users = new ArrayList<String>();
       refreshData();
    }

    /*
    For each User that the current User is following, make sure that that user has an entry in the
    data set for their most recent MoodEvent
     */
    @Override
    public void refreshData()
    {
        if (observed_users == null)
        {
            return;
        }

        List<String> followed_users = user.getFollowingList();

        boolean listChanged = false;

        for (String followed_user : followed_users)
        {
            if (!observed_users.contains(followed_user))
            {
                listChanged = true;
                /*
                If the user is not followed (by UID), add that user to the observed_users list and
                create an Observer that updates the entry for that user whenever that User is changed
                 */
                User user = UserManager.getUser(followed_user);

                observed_users.add(followed_user);

                /*
                If the User doesn't need to be loaded by UserManager, add the entry immediately
                 */
                if (user.isLoaded())
                {
                    refreshEvent(user);
                }

                UserManager.addUserObserver(followed_user, new Observer() {
                    @Override
                    public void update(Observable o, Object a) {
                        FollowedMoodListAdapter.this.refreshEvent((User) o);
                    }
                });
            }
        }

        if (filterChanged && !listChanged)
        {
            for (String followed_user : followed_users)
            {
                refreshEvent(UserManager.getUser(followed_user));
            }
        }
    }

    /*
    Finds the MoodEvent with User user in the data. If that MoodEvent doesn't exist, add the User's
    most recent MoodEvent, otherwise update the displayed MoodEvent to be the User's most recent MoodEvnet
     */
    public void refreshEvent(User user) {
        Log.d("TEST", "Refreshing event for " + user.getUserName());

        MoodEvent event = user.getMostRecentMoodEvent();
        boolean hideEvent = false;

        if (event == null)
        {
            Log.d("TEST/Following", "User does not have recent event");
            return;
        }

        if (filter != null && event.getState().getEmotion().equals(filter))
        {
            Log.d("TEST/Following", "User has a mood that matches the filter");
        } else if (filter != null)
        {
            Log.d("TEST/Following", "User does not have a mood that matches the filter");
            hideEvent = true;
        }

        Comparator<Object> reverse_chronolgical = new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((MoodEvent) o2).compareTo(o1);
            }
        };

        /*
        Iterate through the list and check if an event associated with user exists, if it does,
        replace that event with the most recent MoodEvent
         */
        boolean replaced = false;
        for (MoodEvent displayedEvent : data)
        {
            Log.d("FollowedMoodListener", "event:"+event.getState().getEmotion() + " , checking with: " + filter);

            if (displayedEvent.getUser().getUid().equals(user.getUid()))
            {
                clear();

                data.remove(displayedEvent);
                if (hideEvent == false)
                {
                    data.add(event);
                }
                data.sort(reverse_chronolgical);

                addAll(data);
                replaced = true;
                Log.d("TEST", "Replaced the event");
                break;
            }
        }

        /*
        If there wasn't an event to replace, add a new entry to the list
         */
        if (!replaced && !hideEvent)
        {
            clear();
            data.add(event);
            data.sort(reverse_chronolgical);
            addAll(data);

            Log.d("TEST", "Event didn't exist, added new event");
        }
    }

    /*
    Called for notifyObservers, if the user parameter is the current user, update the entire list,
    otherwise update the event associated with the User parameter
    @param observable
        A User object
    @param arg
        An optional (ignored) argument
     */
    @Override
    public void update(Observable observable, Object arg)
    {
        User user1 = (User) user;

        if (user1 == this.user)
        {
            refreshData();
        }
        else
        {
            refreshEvent(user1);
        }
    }

    /*
    Remove any observers that observed_users have
     */
    @Override
    public void removeObservers()
    {
        for (String user : observed_users)
        {
            UserManager.removeUserObservers(user);
        }
        super.removeObservers();
    }

    @Override
    public void setFilter(String filter) {
        this.filterChanged = true;
        super.setFilter(filter);
    }
}
