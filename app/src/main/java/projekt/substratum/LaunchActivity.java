package projekt.substratum;

import android.content.Context;
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

public class LaunchActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        //if (!prefs.getBoolean("first_run", false)) {
        if (false == true) {
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
                .MATERIAL_TUTORIAL_ARG_TUTORIAL_ITEMS, getTutorialItems(this));
        startActivityForResult(mainAct, REQUEST_CODE);
    }

    private ArrayList<TutorialItem> getTutorialItems(Context context) {
        TutorialItem tutorialItem1 = new TutorialItem(R.string.slide_1_african_story_books, R
                .string.slide_1_african_story_books,
                R.color.slide_1, R.drawable.tut_page_1_front, R.drawable.tut_page_1_background);

        TutorialItem tutorialItem2 = new TutorialItem(R.string.slide_2_volunteer_professionals, R
                .string.slide_2_volunteer_professionals_subtitle,
                R.color.slide_2, R.drawable.tut_page_2_front, R.drawable.tut_page_2_background);

        TutorialItem tutorialItem3 = new TutorialItem(context.getString(R.string
                .slide_3_download_and_go), null,
                R.color.slide_3, R.drawable.tut_page_3_foreground);

        TutorialItem tutorialItem4 = new TutorialItem(R.string.slide_4_different_languages, R
                .string.slide_4_different_languages_subtitle,
                R.color.slide_4, R.drawable.tut_page_4_foreground, R.drawable
                .tut_page_4_background);

        ArrayList<TutorialItem> tutorialItems = new ArrayList<>();
        tutorialItems.add(tutorialItem1);
        tutorialItems.add(tutorialItem2);
        tutorialItems.add(tutorialItem3);
        tutorialItems.add(tutorialItem4);

        return tutorialItems;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {

            // Add all predefined booleans here for the app to function
            prefs.edit().putBoolean("first_run", true).apply();
            prefs.edit().putBoolean("show_installed_packages", true).apply();

            // Finally start the MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            this.finish();
        } else {
            this.finish();
        }
    }
}