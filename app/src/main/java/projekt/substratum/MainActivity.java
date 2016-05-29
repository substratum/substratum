package projekt.substratum;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public RecyclerView list;
    private String[] headerNamesArray, headerPreviewsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        // TODO: Create it so it uses a recyclerView to parse substratum-based themes


        // Test

        CardView testCard = (CardView) findViewById(R.id.theme_card);
        testCard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        Intent myIntent = new Intent(MainActivity.this, ThemeInformation.class);
                        //myIntent.putExtra("key", value); //Optional parameters
                        myIntent.putExtra("theme_name", "Domination by Dave");
                        myIntent.putExtra("theme_pid", "com.annihilation.domination");
                        MainActivity.this.startActivity(myIntent);
                    }
                }).start();
            }
        });


    }

}
