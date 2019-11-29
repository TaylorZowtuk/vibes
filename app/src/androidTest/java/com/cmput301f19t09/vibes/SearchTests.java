package com.cmput301f19t09.vibes;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.victoralbertos.device_animation_test_rule.DeviceAnimationTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SearchTests {
    // automate disabling device animations which is required by espresso
    @ClassRule
    public static DeviceAnimationTestRule
            deviceAnimationTestRule = new DeviceAnimationTestRule();

    @Rule
    public ActivityTestRule<LoginActivity> rule =
            new ActivityTestRule<>(LoginActivity.class, true, true);

    /**
     * Login to the default test account.
     */
    @Before
    public void setUp() throws InterruptedException {
        try {
            Login.setUp();
        }
        catch (InterruptedException e) {
            Log.d("Test Exception", e.toString());
        }

        Thread.sleep(1000);
        onView(withId(R.id.main_search_button)).perform(click());
        onView(withId(R.id.search_fragment)).check(matches(isDisplayed()));
    }

    /**
     * Test whether all UI elements are on screen in the search fragment.
     */
    @Test
    public void assertElements() {
        onView(withId(R.id.search_edittext)).check(matches(isDisplayed()));
        onView(withId(R.id.main_search_button)).check(matches(isDisplayed()));
    }

//    @Test
    public void searchName() {
        // search for test users are they should be stable in the 
        onView(withId(R.id.edit_reason_view)).perform(typeText("?"));

    }
}
