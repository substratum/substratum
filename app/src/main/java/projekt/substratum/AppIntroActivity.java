package projekt.substratum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

import za.co.riggaroo.materialhelptutorial.TutorialItem;
import za.co.riggaroo.materialhelptutorial.tutorial.MaterialTutorialActivity;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class AppIntroActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        if (prefs.getBoolean("first_run", true)) {
            loadAppIntro();
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            this.finish();
        }
    }

    public void loadAppIntro() {
        Intent mainAct = new Intent(this, MaterialTutorialActivity.class);
        mainAct.putParcelableArrayListExtra(MaterialTutorialActivity
                .MATERIAL_TUTORIAL_ARG_TUTORIAL_ITEMS, getTutorialItems());
        startActivityForResult(mainAct, REQUEST_CODE);
    }

    private ArrayList<TutorialItem> getTutorialItems() {
        TutorialItem tutorialItem1 = new TutorialItem(R.string.slide_1_title, R
                .string.slide_1_text,
                R.color.slide_1, R.drawable.appintro_slide_1, R.drawable.appintro_slide_1_anim);

        TutorialItem tutorialItem2 = new TutorialItem(R.string.slide_2_title, R
                .string.slide_2_text,
                R.color.slide_2, R.drawable.appintro_slide_2, R.drawable.appintro_slide_2);

        TutorialItem tutorialItem3 = new TutorialItem(R.string.slide_3_title, R
                .string.slide_3_text,
                R.color.slide_3, R.drawable.appintro_slide_3, R.drawable.appintro_slide_3);

        TutorialItem tutorialItem4 = new TutorialItem(R.string.slide_4_title, R
                .string.slide_4_text,
                R.color.slide_4, R.drawable.appintro_slide_4, R.drawable.appintro_slide_4);

        TutorialItem tutorialItem5 = new TutorialItem(R.string.slide_5_title, R
                .string.slide_5_text,
                R.color.slide_5, R.drawable.appintro_slide_5, R.drawable.appintro_slide_5);

        TutorialItem tutorialItem6 = new TutorialItem(R.string.slide_6_title, R
                .string.slide_6_text,
                R.color.slide_6, R.drawable.appintro_slide_6, R.drawable.appintro_slide_6);

        ArrayList<TutorialItem> tutorialItems = new ArrayList<>();
        tutorialItems.add(tutorialItem1);
        tutorialItems.add(tutorialItem2);
        tutorialItems.add(tutorialItem3);
        tutorialItems.add(tutorialItem4);
        tutorialItems.add(tutorialItem5);
        tutorialItems.add(tutorialItem6);

        return tutorialItems;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {

            // Add all predefined booleans here for the app to function
            prefs.edit().putBoolean("enable_swapping_overlays", false).apply();
            prefs.edit().putBoolean("first_run", false).apply();

            // Finally start the MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            this.finish();
        } else {
            this.finish();
        }
    }
}