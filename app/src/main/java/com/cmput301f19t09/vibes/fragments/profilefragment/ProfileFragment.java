package com.cmput301f19t09.vibes.fragments.profilefragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cmput301f19t09.vibes.R;
import com.cmput301f19t09.vibes.fragments.mooddetailsfragment.MoodDetailsFragment;
import com.cmput301f19t09.vibes.fragments.moodlistfragment.MoodListFragment;
import com.cmput301f19t09.vibes.models.User;
import com.cmput301f19t09.vibes.models.UserManager;

import java.util.Observable;
import java.util.Observer;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/**
 * ProfileFragment contains information about the the current user logged in or information of
 * other users being viewed based on whether you're following the other user or not. Calls a child
 * fragment to view user's own mood history if on own user profile but calls a child fragment to
 * view other user's most recent mood event if following
 *
 * Following other users not implemented yet
 */
public class ProfileFragment extends Fragment implements Observer {
    private TextView fullNameTextView;
    private TextView userNameTextView;
    private ImageView profilePictureImageView;
    private Button followButton;
    private User otherUser;
    private User user;

    /**
     * Creates a new instance of the profile fragment for the current user
     * @return ProfileFragment of the current user
     */
    public static ProfileFragment newInstance() {
        ProfileFragment profileFragment = new ProfileFragment();
        return profileFragment;
    }

    /**
     * Creates a new instance of the profile fragment for the user being viewed
     * @param otherUserUID The UID of the other user you want to view
     * @return ProfileFragment of the user you want to view
     */
    public static ProfileFragment newInstance(String otherUserUID) {
        ProfileFragment profileFragment = new ProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putString("otherUserUID", otherUserUID);
        profileFragment.setArguments(bundle);
        return profileFragment;
    }

    /**
     * Creates the view of the ProfileFragment and loading specific fields with values based on
     * who's profile is being viewed
     * @param inflater Makes the view of the fragment from the XML layout file
     * @param container Parent container to store the fragment in
     * @param savedInstanceState Saved instance state of the MainActivity
     * @return The created ProfileFragment view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_profile, container, false);

        // Get specific views
        fullNameTextView = view.findViewById(R.id.fullname_textview);
        userNameTextView = view.findViewById(R.id.username_textview);
        profilePictureImageView = view.findViewById(R.id.profile_picture);
        followButton = view.findViewById(R.id.follow_button);

        // Verifies if other user's UID is passed in through new instance
        String otherUserUID = null;
        if (getArguments() != null) {
            otherUserUID = getArguments().getString("otherUserUID");
        }

        // Creates a user and observer for the user being viewed
        otherUser = null;
        if (otherUserUID != null) {
            otherUser = UserManager.getUser(otherUserUID);
            UserManager.addUserObserver(otherUserUID, this);
        }

        // Gets the current user from UserManager
        user = UserManager.getCurrentUser();

        // TODO: Implementing in last checkpoint
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "REQUESTED", Toast.LENGTH_LONG).show();
            }
        });

        // Verifies if user exists
        if (user == null) {
            throw new RuntimeException("[ERROR]: USER IS NOT DEFINED");
        }

        // Show's your own profile if other user wasn't passed in, set's the info, and gets the child
        // fragment MoodListFragment of the mood list of the current user
        if (otherUser == null) {
            followButton.setVisibility(View.INVISIBLE);
            UserManager.addUserObserver(user.getUid(), this);
            setInfo(user);
            MoodListFragment moodListFragment = MoodListFragment.newInstance(MoodListFragment.OWN_MOODS_LOCKED);
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.user_mood_list, moodListFragment).commit();

        } else {
            // Checks if the user is following the other user and show their latest mood event by
            // calling the child fragment MoodDetailsFragment
            if (user.getFollowingList().contains(otherUser.getUid())) {
                followButton.setVisibility(View.INVISIBLE);

                if (otherUser.isLoaded()) {
                    Log.d("TEST", "Other user loaded");
                    setInfo(otherUser);
                    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                    transaction.replace(R.id.user_mood_list, MoodDetailsFragment.newInstance(otherUser.getMostRecentMoodEvent()));
                    transaction.commit();
                } else {
                    Log.d("TEST", "Other user not loaded");
                }

                otherUser.addObserver(new Observer() {
                    @Override
                    public void update(Observable o, Object arg) {
                        User u = (User) o;
                        ProfileFragment.this.setInfo(u);
                        FragmentTransaction transaction = ProfileFragment.this.getChildFragmentManager().beginTransaction();
                        transaction.replace(R.id.user_mood_list, MoodDetailsFragment.newInstance(u.getMostRecentMoodEvent()));
                        transaction.commit();
                    }
                });
                setInfo(otherUser);
            } else {
                // Not following the viewed user shows nothing
                followButton.setVisibility(View.VISIBLE);
                setInfo(otherUser);
            }
        }
        return view;
    }

    /**
     * Updates the fragment with new values if there was an update from the database.
     * @param user The user object being observed for changes
     * @param object Any object being handed in (will always be null)
     */
    @Override
    public void update(Observable user, Object object) {
        setInfo((User) user);
    }

    /**
     * Updates the fields with user information
     * @param user The object to get the values from
     */
    public void setInfo(User user) {
        if (user.isLoaded()) {
            fullNameTextView.setText(user.getFirstName() + " " + user.getLastName());
            userNameTextView.setText(user.getUserName());
            Glide.with(this).load(user.getProfileURL()).into(profilePictureImageView);
            profilePictureImageView.setClipToOutline(true);
        }
    }

    /**
     * Removes observers when switch fragments
     */
    @Override
    public void onPause() {
        super.onPause();
        UserManager.removeUserObserver(UserManager.getCurrentUserUID(), this);
        if (otherUser != null) {
            UserManager.removeUserObservers(otherUser.getUid());
        }
    }
}