package com.cmput301f19t09.vibes.models;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.cmput301f19t09.vibes.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class User extends Observable implements Serializable {
    private String uid;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String picturePath;
    private List<String> followingList;
    private List<String> requestedList;

    private boolean loadedData;

    // Objects are not serializable - will crash on switching app if not omitted from serialization
    // Ref https://stackoverflow.com/questions/14582440/how-to-exclude-field-from-class-serialization-in-runtime
    private transient static FirebaseFirestore db;
    private transient static CollectionReference collectionReference ;
    private transient static DocumentReference documentReference;
    private transient static FirebaseStorage storage;
    private transient static StorageReference storageReference;
    private transient Uri profileURL;

    private transient List<MoodEvent> moodEvents;
    private transient List<Map> moods;

    private static boolean connectionStarted;

    // This is the number of elements in the mood map on firebase.
    // I used it to check if a map is complete to show it on the map.
    final private int MAP_MOOD_SIZE = 7;
    final private String TAG = "Sample";

    public interface FirebaseCallback {
        void onCallback(User user);
    }

    public interface UserExistListener {
        void onUserExists();
        void onUserNotExists();
    }

    /**
     *
     * @param userName
     * @param firstName
     * @param lastName
     * @param email
     */
    public User(String userName, String firstName, String lastName, String email) {
        this();
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.picturePath = "image/" + this.userName + ".png";
        this.loadedData = true;

        Map<String, Object> userData = new HashMap<>();
        userData.put("first", userName);
        userData.put("last", lastName);
        userData.put("email", email);
        userData.put("following_list", new ArrayList<>());
        userData.put("moods", new ArrayList<>());
        userData.put("profile_picture", picturePath);
        userData.put("requested_list", new ArrayList<>());

        collectionReference.document(userName).set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Uri imageUri = Uri.parse("android.resource://com.cmput301f19t09.vibes/" + R.drawable.default_profile_picture);
                        storageReference = storage.getReference(picturePath);
                        storageReference.putFile(imageUri).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("INFO", "Failed to store default profile picture");
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("INFO", "Data failed to store in Firestore");
                    }
                });
    }

    /**
     *
     */
    public User(){
        if(!connectionStarted){ // Makes sure these definitions are called only once.
            connectionStarted = true;

            db = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false)
                    .build();
            db.setFirestoreSettings(settings);

            collectionReference = db.collection("users");
            storage = FirebaseStorage.getInstance();
        }
    }

    public User(String uid) {
        this.uid = uid;
        Log.d("TEST", "Creating user from id " + uid);
        if(!connectionStarted){ // Makes sure these definitions are called only once.
            connectionStarted = true;

            db = FirebaseFirestore.getInstance();
            collectionReference = db.collection("users");
            storage = FirebaseStorage.getInstance();
        }

        this.loadedData = false;
    }

    public ListenerRegistration getSnapshotListener() {
        documentReference = collectionReference.document(uid);
        return documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                Log.d("TEST", "User event");
                userName = documentSnapshot.getString("username");
                firstName = documentSnapshot.getString("first");
                lastName = documentSnapshot.getString("last");
                email = documentSnapshot.getString("email");
                picturePath = documentSnapshot.getString("profile_picture");
                followingList = (List<String>) documentSnapshot.get("following_list");
                requestedList = (List<String>) documentSnapshot.get("requested_list");
                moods = (List<Map>) documentSnapshot.get("moods");
                loadedData = true;

                List<Map> moods = (List<Map>) documentSnapshot.get("moods");

                moodEvents = parseToMoodEvent();

                storageReference = storage.getReference(picturePath);
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        profileURL = uri;
                        Log.d("INFO", "Loaded profile picture URL");
                        setChanged();
                        notifyObservers();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("INFO", "Cannot retrieve profile picture download url");
                    }
                });

                Log.d("INFO", "Loaded user information");

                int i = countObservers();
                Log.d("TEST", i + " observers");

            }
        });
    }

    /**
     *
     * @param firebaseCallback
     */
    public void readData(FirebaseCallback firebaseCallback) {
        if(uid == null){
            throw new RuntimeException("[UserClass]: Username isn't defined for readData()");
        }

        Log.d("TEST", "Reading in ALL data");
        // Using SnapshotListener helps reduce load times and obtains from local cache
        // Ref https://firebase.google.com/docs/firestore/query-data/listen
        documentReference = collectionReference.document(uid);

        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                userName = documentSnapshot.getString("username");
                firstName = documentSnapshot.getString("first");
                lastName = documentSnapshot.getString("last");
                email = documentSnapshot.getString("email");
                picturePath = documentSnapshot.getString("profile_picture");
                followingList = (List<String>) documentSnapshot.get("following_list");
                requestedList = (List<String>) documentSnapshot.get("requested_list");
                moods = (List<Map>) documentSnapshot.get("moods");

                List<Map> moods = (List<Map>) documentSnapshot.get("moods");

                moodEvents = parseToMoodEvent();

                storageReference = storage.getReference(picturePath);
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        profileURL = uri;
                        Log.d("INFO", "Loaded profile picture URL");
                        firebaseCallback.onCallback(User.this);
                        loadedData = true;
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("INFO", "Cannot retrieve profile picture download url");
                    }
                });

                Log.d(TAG, "Loaded user information");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("INFO", "Cannot load info");
            }
        });
    }

    public boolean isLoaded()
    {
        return loadedData;
    }

    public String getUid()
    {
        return uid;
    }

    /**
     *
     * @return
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     *
     * @return
     */
    public String getLastName() {
        return lastName;
    }

    /**
     *
     * @return
     */
    public String getUserName() {
        Log.d("TEST", "returning username " + userName);
        return userName;
    }

    /**
     *
     * @return
     */
    public String getPicturePath() {
        return picturePath;
    }

    /**
     *
     * @return
     */
    public Uri getProfileURL() {
        return profileURL;
    }

    /**
     *
     * @return
     */
    public String getEmail() {
        return email;
    }

    /**
     *
     * @return
     */
    public List<String> getFollowingList() {
        return followingList;
    }

    /**
     *
     * @return
     */
    public List<String> getRequestedList() {
        return requestedList;
    }

    /**
     *
     * @param userExistListener
     */
    public void exists(UserExistListener userExistListener) {
        collectionReference.document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if(documentSnapshot != null && documentSnapshot.exists()) {
                                userExistListener.onUserExists();
                            } else {
                                userExistListener.onUserNotExists();
                            }
                        }
                    }
                });
    }

    /**
     *
     * @param firstName
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     *
     * @param lastName
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     *
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     *
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     *
     * @return
     */
    public List<MoodEvent> parseToMoodEvent() {
        List<MoodEvent> events = new ArrayList<MoodEvent>();
        if (moods != null) {
            for (Map moodEvent : moods) {
                String emotion = (String) moodEvent.get("emotion");
                String reason = (String) moodEvent.get("reason");
                Number social = (Number) moodEvent.get("social");
                Long timestamp = (Long) moodEvent.get("timestamp");
                String username = (String) moodEvent.get("username");
                GeoPoint locationGeoPoint = (GeoPoint) moodEvent.get("location");

                if (moodEvent.size() != MAP_MOOD_SIZE) {
                    Log.d("INFO", "Mood isn't complete yet");
                    continue;
                }

                if (timestamp == null) {
                    throw new RuntimeException("[MOOD_ERROR]: Timestamp isn't defined");
                }

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timestamp);

                LocalDateTime time = LocalDateTime.ofEpochSecond(
                        timestamp,
                        0,
                        ZoneOffset.UTC
                );

                Log.d("TEST", "The UTC time is " + time.toString());


                Location location = new Location("");
                location.setLatitude(locationGeoPoint.getLatitude());
                location.setLongitude(locationGeoPoint.getLongitude());

                MoodEvent event = new MoodEvent(time.toLocalDate(),
                        time.toLocalTime(),
                        reason,
                        new EmotionalState(emotion),
                        social.intValue(),
                        location,
                        this);
                events.add(event);
            }
        }
        return events;
    }

    /**
     *
     * @return
     */
    public List<MoodEvent> getMoodEvents() {
        return moodEvents;
    }

    /**
     *
     * @return
     */
    public MoodEvent getMostRecentMoodEvent() {
        MoodEvent moodEvent;
        if (moodEvents != null && moodEvents.size() != 0) {

            moodEvent = moodEvents.get(0);

            for (MoodEvent event : moodEvents)
            {
                if (event.compareTo(moodEvent) <= 0)
                {
                    moodEvent = event;
                }
            }

            //moodEvent = moodEvents.get(moodEvents.size() - 1);
            return moodEvent;
        } else {
            Log.d("INFO", "No mood events");
            return null;
        }
    }

    /**
     *
     * @param moodEvent
     */
    public void addMood(MoodEvent moodEvent) {
        if (moodEvent == null) {
            throw new RuntimeException("Mood not defined");
        } else {
            Map<String, Object> mood = new HashMap<String, Object>();
            LocalDateTime time = LocalDateTime.of(moodEvent.date, moodEvent.time);
            mood.put("emotion", moodEvent.getState().getEmotion());
            mood.put("location", new GeoPoint(53.23, -115.44));
            mood.put("photo", null);
            mood.put("timestamp", time.toEpochSecond(ZoneOffset.UTC));
            mood.put("reason", moodEvent.getDescription());
            mood.put("social", moodEvent.getSocialSituation());
            mood.put("username", moodEvent.getUser().getUserName());

            documentReference = collectionReference.document(uid);
            documentReference.update("moods", FieldValue.arrayUnion(mood)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("INFO", "Moods list updated");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("INFO", "Cannot add mood to list");
                }
            });
        }
    }

    public void addMood() {
        Map<String, Object> mood = new HashMap<String, Object>();
        mood.put("emotion", "SADNESS");
        mood.put("location", new GeoPoint(55.55, -114.44));
        mood.put("photo", null);
        mood.put("reason", "Cause");
        mood.put("social", 1);
        mood.put("timestamp", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        mood.put("username", "testuser");

        documentReference = collectionReference.document(uid);
        documentReference.update("moods", FieldValue.arrayUnion(mood)).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("INFO", "Moods list updated");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("INFO", "Cannot add mood to list");
            }
        });
    }

    /**
     *
     * @param moodEvent
     * @param index
     */
    public void editMood(MoodEvent moodEvent, Integer index) {
        if (index > moods.size() - 1) {
            return;
        } else {
            Map<String, Object> mood = new HashMap<String, Object>();
            LocalDateTime time = LocalDateTime.of(moodEvent.date, moodEvent.time);
            mood.put("emotion", moodEvent.getState().getEmotion());
            mood.put("location", new GeoPoint(53.23, -115.44));
            mood.put("photo", null);
            mood.put("reason", moodEvent.getDescription());
            mood.put("social", moodEvent.getSocialSituation());
            mood.put("timestamp", time.toEpochSecond(ZoneOffset.from(time)));
            mood.put("username", moodEvent.getUser().getUserName());

            moods.set(index.intValue(), mood);
            documentReference = collectionReference.document(userName);
            documentReference.update("moods", moods).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("INFO", "Moods list updated");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("INFO", "Cannot add mood to list");
                }
            });
        }
    }

    /**
     *
     * @param index
     */
    public void deleteMood(Integer index) {
        if (index > moods.size() - 1) {
            return;
        } else {
            moods.remove(index.intValue());
            documentReference = collectionReference.document(uid);
            documentReference.update("moods", moods).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("INFO", "Removed successfully");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("INFO", "Could not remove from array");
                }
            });
        }
    }


    @Override
    public synchronized void addObserver(Observer o)
    {
        super.addObserver(o);
        logObservers("[ADDED " + o.getClass().getSimpleName() + "]");
    }

    @Override
    public synchronized void deleteObserver(Observer o)
    {
        super.deleteObserver(o);
        logObservers("[DELTED " + o.getClass().getSimpleName() + "]");
    }

    @Override
    public void notifyObservers()
    {
        super.notifyObservers();
        logObservers("notifying observers");
    }

    private void logObservers(String msg)
    {
        Log.d("TEST/USERS", " " + getUserName() + " has " + countObservers() + " observers");
        Log.d("USERS/" + getUserName(), "Has " + countObservers() + " observers " + msg);
    }
}
