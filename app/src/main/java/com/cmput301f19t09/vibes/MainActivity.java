package com.cmput301f19t09.vibes;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cmput301f19t09.vibes.fragments.followingfragment.FollowingFragment;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.cmput301f19t09.vibes.fragments.editfragment.EditFragment;
import com.cmput301f19t09.vibes.fragments.mapfragment.MapFragment;
import com.cmput301f19t09.vibes.fragments.moodlistfragment.MoodListFragment;
import com.cmput301f19t09.vibes.fragments.profilefragment.ProfileFragment;
import com.cmput301f19t09.vibes.fragments.searchfragment.SearchFragment;
import com.cmput301f19t09.vibes.models.MoodEvent;
import com.cmput301f19t09.vibes.models.User;
import com.cmput301f19t09.vibes.models.UserManager;
import com.google.firebase.auth.FirebaseAuth;

/**
 * MainActivity is the main activity that shows up in the app right now.
 */
public class MainActivity extends FragmentActivity {

    private enum ButtonMode {LIST, MAP}

    private ButtonMode currentButtonMode;
    private @IdRes
    int fragment_root;
    private User user;
    Context mcontext;

    //TODO: DEBUG, REMOVE THIS
    private final String logTag = "TEST/MainActivity";
    
    private MapFragment.Filter mapFilter = MapFragment.Filter.SHOW_MINE; // The filter of the map.
    private String listFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mcontext = this;
        // Set the button in the bottom left to open the map fragment

        user = UserManager.getCurrentUser();

        // Defines onClickListeners for the components defined above in the class.
        initialize();

        setListFragment(MoodListFragment.OWN_MOODS);

        /*
            Add a backstack listener to change what the "view" (list/map) button displays
            when a new fragment is opened
         */
        final FragmentManager manager = getSupportFragmentManager();
        manager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                List<Fragment> fragments = manager.getFragments();
                Fragment currentFragment = fragments.get(fragments.size() - 1);

                if (currentFragment == null || currentFragment.getClass().equals(MoodListFragment.class)) {
                    currentButtonMode = ButtonMode.MAP;
                } else {
                    currentButtonMode = ButtonMode.LIST;
                }

                updateViewButton();
            }
        });
    }

    /*
     *  Initializes the views in the main activity layout
     */
    private void initialize() {

        fragment_root = R.id.main_fragment_root;
        currentButtonMode = ButtonMode.MAP;

        View addButton, searchButton, followingButton, viewButton, logoutButton;
        ImageView profileButton;
        addButton = findViewById(R.id.main_add_button);
        profileButton = findViewById(R.id.main_profile_button);
        followingButton = findViewById(R.id.main_follow_list_button);
        logoutButton = findViewById(R.id.logoutButton);
        searchButton = findViewById(R.id.main_search_button);
        viewButton = findViewById(R.id.main_view_button);
        logoutButton.setVisibility(View.INVISIBLE);
        Glide.with(this).load(user.getProfileURL()).into(profileButton);
        profileButton.setClipToOutline(true);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //setMainFragment(EditFragment.newInstance());
                logoutButton.setVisibility(View.INVISIBLE);
                setEditFragment();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //setMainFragment(SearchFragment.newInstance());
                logoutButton.setVisibility(View.INVISIBLE);
                setSearchFragment();
            }
        });

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //setMainFragment(ProfileFragment.newInstance());
                logoutButton.setVisibility(View.VISIBLE);
                setProfileFragment();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                // Log out action
//                                UserManager.unre
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(mcontext);
                builder.setMessage("Do you want to logout?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

            }
        });

        followingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //setMainFragment(FollowingFragment.newInstance(UserManager.getCurrentUser()));
                logoutButton.setVisibility(View.INVISIBLE);
                setFollowingFragment();
            }
        });

        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                Set the button to represent which fragment will be opened the NEXT TIME the button
                is pressed (i.e. the current fragment)
                 */
                logoutButton.setVisibility(View.INVISIBLE);

                switch (currentButtonMode) {
                    case LIST:
                        //setMainFragment(MoodListFragment.newInstance(MoodListFragment.OWN_MOODS));
                        setListFragment(MoodListFragment.OWN_MOODS);
                        currentButtonMode = ButtonMode.MAP;
                        break;
                    default:
                        //setMainFragment(MapFragment.newInstance(getApplicationContext()));
                        setMapFragment();
                        currentButtonMode = ButtonMode.LIST;
                        break;
                }
            }
        });

        user.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                Glide.with(MainActivity.this).load(user.getProfileURL()).into(profileButton);
            }
        });

        updateViewButton(); // Updates the view button only.
    }


    /*
        Creates a FragmentTransaction which replaces the current fragment with the specified one
        @param fragment
            the fragment to place in the main view
     */
    public void setMainFragment(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();

        int entryCount = manager.getBackStackEntryCount();

        if (entryCount != 0 &&
                tag != null &&
                tag.equals(manager.getBackStackEntryAt(entryCount - 1).getName()))
        {
            return;
        }

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(fragment_root, fragment, tag);
        transaction.addToBackStack(tag);
        transaction.commit();
    }

    private Fragment getFragmentInstance(String tag)
    {
        FragmentManager manager = getSupportFragmentManager();
        Fragment instance = manager.findFragmentByTag(tag);

        return instance;
    }

    public void setMainFragment(Fragment f) {}

    public void setListFragment(int arg)
    {
        String tag = MoodListFragment.class.getSimpleName();
        Fragment instance = getFragmentInstance(tag);

        if (instance == null)
        {
            instance = MoodListFragment.newInstance(MoodListFragment.OWN_MOODS);
        }

        setMainFragment(instance, tag);
    }

    public void setMapFragment()
    {
        String tag = MapFragment.class.getSimpleName();
        Fragment instance = getFragmentInstance(tag);

        if (instance == null)
        {
            instance = MapFragment.newInstance(getApplicationContext());
        }

        setMainFragment(instance, tag);
    }

    public void setProfileFragment()
    {
        setProfileFragment(null);
    }

    public void setProfileFragment(String uid)
    {
        String tag = ProfileFragment.class.getSimpleName();

        if (uid != null)
        {
            tag += uid;
        }
        else
        {
            tag += UserManager.getCurrentUserUID();
        }

        Fragment instance = getFragmentInstance(tag);

        if (instance == null)
        {
            if (uid == null)
            {
                instance = ProfileFragment.newInstance();
            }
            else
            {
                instance = ProfileFragment.newInstance(uid);
            }
        }

        setMainFragment(instance, tag);
    }

    public void setEditFragment()
    {
        setEditFragment(null, -1);
    }

    public void setEditFragment(MoodEvent event, int index)
    {
        Fragment instance = null;

        if (event == null) {
            instance = EditFragment.newInstance();
        } else {
            instance = EditFragment.newInstance(event, index);
        }

        setMainFragment(instance, null);
    }

    public void setFollowingFragment()
    {
        String tag = FollowingFragment.class.getSimpleName();
        Fragment instance = getFragmentInstance(tag);

        if (instance == null)
        {
            instance = FollowingFragment.newInstance();
        }

        setMainFragment(instance, tag);
    }

    public void setSearchFragment()
    {
        String tag = SearchFragment.class.getSimpleName();
        Fragment instance = SearchFragment.newInstance();
        setMainFragment(instance, tag);
    }

    /*
        Creates a FragmentTransaction to open a DialogFragment in the main view
        @param fragment
            the fragment to open
     */
    public void openDialogFragment(DialogFragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        fragment.show(transaction, null);
    }

    /*
    Update the image of the list/map button to reflect the type of fragment it will open if pressed
    @param fragmentType
        The type of fragment that the button will open if pressed
    */
    private void updateViewButton() {
        ImageButton viewButton = findViewById(R.id.main_view_button);
        @DrawableRes int image;

        switch (currentButtonMode) {
            case LIST:
                image = R.drawable.ic_list_white_36dp;
                break;
            case MAP:
            default:
                image = R.drawable.ic_map_white_36dp;
        }

        viewButton.setImageResource(image);
    }

    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();

        if (manager.getBackStackEntryCount() > 1)
        {
            manager.popBackStackImmediate();
        }

        while (manager.getBackStackEntryCount() > 1 &&
                manager.getBackStackEntryAt(manager.getBackStackEntryCount() - 1).getName() == null)
        {
            manager.popBackStackImmediate();
        }
    }
}

