package com.cmput301f19t09.vibes.fragments.mooddetailsfragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.cmput301f19t09.vibes.MainActivity;
import com.cmput301f19t09.vibes.R;
import com.cmput301f19t09.vibes.fragments.profilefragment.ProfileFragment;
import com.cmput301f19t09.vibes.models.MoodEvent;
import com.cmput301f19t09.vibes.models.User;
import com.cmput301f19t09.vibes.fragments.editfragment.EditFragment;
import com.cmput301f19t09.vibes.models.UserManager;

import java.time.Duration;
import java.time.LocalDateTime;

/*
This fragment opens a Dialog that shows the event. If the event belongs to the current user, then
it is editable and the user can delete it from the database. Clicking on the user's profile picture
will open the profile (ProfileFragmetn) of that user
 */
public class MoodDetailsDialogFragment extends DialogFragment
{
    private MoodEvent event;
    private User user;
    private User eventUser;
    private boolean editable;


    /*
    Create a new fragment for the event, set the editable flag to editable
    @param event
        The MoodEvent to show the details of
    @param editable
        Whether the MoodEvent should be editable
     */
    public static MoodDetailsDialogFragment newInstance(MoodEvent event, boolean editable)
    {
        Bundle bundle = new Bundle();
        MoodDetailsDialogFragment fragment = new MoodDetailsDialogFragment();

        bundle.putSerializable("event", event);
        bundle.putBoolean("editable", editable);
        fragment.setArguments(bundle);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        Bundle arguments = getArguments();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.mood_details, null);

        event = (MoodEvent) arguments.getSerializable("event");
        user = UserManager.getCurrentUser(); // The current user of the app
        eventUser = event.getUser();         // The user who posted the event
        editable = arguments.getBoolean("editable");

        ImageView userImage, emotionImage, reasonImage;
        TextView userUsername, userFullName, moodTime, moodReason;
        Button editButton, confirmButton;
        ImageButton deleteButton;

        userImage = view.findViewById(R.id.user_image);
        emotionImage =  view.findViewById(R.id.emotion_image);
        reasonImage = view.findViewById(R.id.reason_image);
        userUsername =  view.findViewById(R.id.username);
        userFullName = view.findViewById(R.id.full_name);
        moodTime = view.findViewById(R.id.mood_time);
        moodReason = view.findViewById(R.id.reason);
        deleteButton = view.findViewById(R.id.delete_button);
        editButton = view.findViewById(R.id.edit_button);
        confirmButton = view.findViewById(R.id.confirm_button);

        // Set profile picture and crop to a circle
        Glide.with(getContext()).load(eventUser.getProfileURL()).into(userImage);
        userImage.setClipToOutline(true);

        // Set emotion picture and colour
        emotionImage.setImageResource(event.getState().getImageFile());
        emotionImage.setColorFilter(event.getState().getColour());
        emotionImage.setClipToOutline(true);

        deleteButton.setImageResource(R.drawable.ic_delete_white_24dp);

        // Set the username and user's name fields
        userUsername.setText(eventUser.getUserName());
        userFullName.setText(eventUser.getFirstName() + " " + eventUser.getLastName());

        // Calculate the time since the event was posted
        Duration timeSincePost = Duration.between(event.getLocalDateTime(), LocalDateTime.now());

        String timeString = "~";

        /*
        Set the field that displays time (e.g. ~5s)
        TODO: Make this better
         */
        if (timeSincePost.getSeconds() < 60)
        {
            timeString += timeSincePost.getSeconds() + " s";
        }
        else if (timeSincePost.toMinutes() < 60)
        {
            timeString += timeSincePost.toMinutes() + " m";
        }
        else if (timeSincePost.toHours() < 24)
        {
            timeString += timeSincePost.toHours() + " h";
        }
        else if (timeSincePost.toDays() < 365)
        {
            timeString += timeSincePost.toDays() + " d";
        }
        else
        {
            timeString += ( timeSincePost.toDays() / 365 ) + " y";
        }

        moodTime.setText(timeString);
        moodReason.setText(event.getDescription());

        if (editable)
        {
            editButton.setVisibility(View.VISIBLE);
        }
        else
        {
            // If the MoodEvent is not editable, disable the editButton, this also disables the delte
            // button
            editButton.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final Dialog dialog = builder.setView(view)
                .create();

        deleteButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Calculate the index of this event within the user's list of events
                int mood_index = eventUser.getMoodEvents().indexOf(event);

                // If the event doesn't exist in the user, return (i.e. the event has been deleted since
                // the fragment was opened
                if (mood_index == -1)
                {
                    return;
                }

                eventUser.deleteMood(mood_index);

                dismiss();
            }
        });

        editButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        int mood_index = eventUser.getMoodEvents().indexOf(event);

                        // If the mood is not within the user, return
                        if (mood_index == -1)
                        {
                            return;
                        }

                        // Open an edit fragment for the mood
                        ((MainActivity) getActivity()).setMainFragment(EditFragment.newInstance(event, mood_index));
                    }
                });

                dialog.dismiss();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.dismiss();
            }
        });

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    /*
                    When the dialog has closed, open a ProfileFragment for the Event's user,
                     */
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface d) {
                        ProfileFragment profileFragment;

                        if (user == eventUser) {
                            // Open own profile
                            profileFragment = ProfileFragment.newInstance();
                        } else {
                            // Open other user's profile
                            profileFragment = ProfileFragment.newInstance(eventUser.getUid());
                        }
                        ((MainActivity) MoodDetailsDialogFragment.this.getActivity()).setMainFragment(profileFragment);
                    }
                });

                dialog.dismiss();
            }
        });

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.circle);

        return dialog;
    }
}