package projekt.substratum.easteregg;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

import projekt.substratum.R;

public class LLandActivity extends Activity {

    public static ImageView deathText1;
    public static ImageView deathText2;
    public static ImageView deathText3;
    public static ImageView deathBlood;
    public static View newHighView;
    public static int yTranslation;
    public static View dummy;
    public static Typeface font;
    protected Dialog splashDialog;
    int touchBubbles[] = {R.drawable.bubble_1, R.drawable.bubble_2,
            R.drawable.bubble_3, R.drawable.bubble_4, R.drawable.bubble_5};
    int timeBubbles[] = {R.drawable.bubble_6, R.drawable.bubble_7,
            R.drawable.bubble_8, R.drawable.bubble_9, R.drawable.bubble_10,
            R.drawable.bubble_11, R.drawable.bubble_12};
    Random mRandom = new Random();
    private ImageView splashBox;
    private ImageView speechBubble;
    private ImageView titleOne;
    private ImageView titleTwo;
    private ImageView titleThree;
    private ImageView splashBubbaSmall;
    private ImageView splashBubbaBig;
    private ImageView wink;
    private View dismissDialogView;
    private boolean splashAnimating = false;
    private boolean bubbleVisible = false;
    private boolean touchBubbleVisible = false;
    private boolean winkVisible = false;
    private boolean rainbowVisible = false;
    private Handler mHandler;
    private Handler wHandler;
    private Runnable updateBubble;
    private Runnable updateWink;
    private TextView bestScore;
    private TextView newHigh1;
    private TextView newHigh2;
    private TextView newHigh3;
    private int cockpunch = 0;
    private View splashBG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.bubbaland);

        font = Typeface.createFromAsset(getAssets(), "fonts/Cnsid.ttf");

        showSplash();

        LLand world = (LLand) findViewById(R.id.world);
        world.setScoreField((TextView) findViewById(R.id.score));
        world.setSplash(findViewById(R.id.welcome));
        Log.v(LLand.TAG, "focus: " + world.requestFocus());

        deathBlood = (ImageView) findViewById(R.id.blood);
        deathText1 = (ImageView) findViewById(R.id.deadtext1);
        deathText2 = (ImageView) findViewById(R.id.deadtext2);
        deathText3 = (ImageView) findViewById(R.id.deadtext3);
        newHighView = (View) findViewById(R.id.new_high_view);
        newHigh1 = (TextView) findViewById(R.id.new_high_score_1);
        newHigh2 = (TextView) findViewById(R.id.new_high_score_2);
        newHigh3 = (TextView) findViewById(R.id.new_high_score_3);
        dummy = findViewById(R.id.dummyview);

        dummy.setVisibility(View.GONE);

        newHigh1.setTypeface(font);
        newHigh1.setText("NEW");
        newHigh2.setTypeface(font);
        newHigh2.setText("HIGH");
        newHigh3.setTypeface(font);
        newHigh3.setText("SCORE!");
    }

    protected void removeSplash() {
        if (splashDialog != null) {
            splashDialog.dismiss();
            splashDialog = null;
            mHandler.removeCallbacks(updateBubble);
        }
    }

    protected void showSplash() {
        splashDialog = new Dialog(LLandActivity.this, R.style.splash) {
        };
        splashDialog.setContentView(R.layout.splashscreen);
        splashDialog.setCancelable(false);
        splashDialog.show();
        splashAnimating = true;

        splashBG = (View) splashDialog.findViewById(R.id.splashpic);
        titleOne = (ImageView) splashDialog.findViewById(R.id.title1);
        titleTwo = (ImageView) splashDialog.findViewById(R.id.title2);
        titleThree = (ImageView) splashDialog.findViewById(R.id.title3);
        splashBubbaSmall = (ImageView) splashDialog
                .findViewById(R.id.splashbubbasmall);
        splashBubbaBig = (ImageView) splashDialog
                .findViewById(R.id.splashbubbabig);
        splashBox = (ImageView) splashDialog.findViewById(R.id.splashbox);
        speechBubble = (ImageView) splashDialog
                .findViewById(R.id.speech_bubble);
        wink = (ImageView) splashDialog.findViewById(R.id.wink);
        dismissDialogView = splashDialog.findViewById(R.id.full_dialog_view);
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(this);
        int highScore = pref.getInt("kofferland_high_score", 0);
        bestScore = (TextView) splashDialog.findViewById(R.id.best_score);
        bestScore.setTypeface(font);
        bestScore.setText("Best: " + highScore);
        if (highScore == 0) {
            bestScore.setVisibility(View.GONE);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final int screenHeight = metrics.heightPixels;
        final double a = 1.2;
        final double b = 3.84;
        final int y1 = (int) (screenHeight * a);
        final int y2 = (int) (screenHeight / b);
        yTranslation = y1;

        dismissDialogView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!splashAnimating) {
                    removeSplash();
                }
            }
        });

        mHandler = new Handler();
        updateBubble = new Runnable() {
            @Override
            public void run() {
                if (!bubbleVisible) {
                    int i = mRandom.nextInt(timeBubbles.length);
                    speechBubble.setImageResource(timeBubbles[i]);
                    speechBubble.setVisibility(View.VISIBLE);
                    bubbleVisible = true;
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            speechBubble.setVisibility(View.GONE);
                            bubbleVisible = false;
                        }
                    }, 2500);
                }
                mHandler.postDelayed(this, 10000);
            }
        };
        mHandler.postDelayed(updateBubble, 10000);

        splashBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!touchBubbleVisible) {
                    int i = mRandom.nextInt(touchBubbles.length);
                    speechBubble.setImageResource(touchBubbles[i]);
                    speechBubble.setVisibility(View.VISIBLE);
                    bubbleVisible = true;
                    touchBubbleVisible = true;
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            speechBubble.setVisibility(View.GONE);
                            bubbleVisible = false;
                            touchBubbleVisible = false;
                        }
                    }, 2500);
                    cockpunch++;
                    if (cockpunch == 5) {
                        if (!rainbowVisible) {
                            splashBG.setBackgroundResource(R.drawable.rainbow);
                            rainbowVisible = true;
                            AnimationDrawable rainbowAnim = (AnimationDrawable) splashBG
                                    .getBackground();
                            splashBG.setVisibility(View.VISIBLE);
                            rainbowAnim.start();

                            wHandler = new Handler();
                            updateWink = new Runnable() {
                                @Override
                                public void run() {
                                    if (!winkVisible) {
                                        wink.setVisibility(View.VISIBLE);
                                        winkVisible = true;
                                        Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                wink.setVisibility(View.GONE);
                                                winkVisible = false;
                                            }
                                        }, 300);
                                    }
                                    wHandler.postDelayed(this, 5000);
                                }
                            };
                            wHandler.postDelayed(updateWink, 5000);
                        }
                    }
                }
            }
        });

        final Animation scale1 = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        scale1.setDuration(500);
        scale1.setStartOffset(250);
        scale1.setFillAfter(true);
        final Animation scale2 = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        scale2.setDuration(500);
        scale2.setFillAfter(true);
        final Animation scale3 = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        scale3.setDuration(500);
        scale3.setFillAfter(true);
        final Animation translateUp = new TranslateAnimation(0, 0, y1, 0);
        translateUp.setDuration(1000);
        final Animation moveOffScreen = new TranslateAnimation(0, 0, y1, 0);
        translateUp.setDuration(1);
        final Animation scaleBubbaBig = new ScaleAnimation(0.4f, 1f, 0.4f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        scaleBubbaBig.setDuration(1000);
        final AnimationSet animSetBig = new AnimationSet(true);
        animSetBig.setDuration(1000);
        animSetBig.setFillAfter(true);
        animSetBig.setInterpolator(new DecelerateInterpolator());
        animSetBig.addAnimation(translateUp);
        animSetBig.addAnimation(scaleBubbaBig);
        final Animation translateDown = new TranslateAnimation(0, 0,
                -screenHeight, y2);
        translateUp.setDuration(1000);
        final Animation scaleBubbaSmall = new ScaleAnimation(0.3f, 1f, 0.3f,
                1f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleBubbaBig.setDuration(1000);
        final AnimationSet animSetSmall = new AnimationSet(true);
        animSetSmall.setFillAfter(true);
        animSetSmall.setDuration(1000);
        animSetSmall.setInterpolator(new DecelerateInterpolator());
        animSetSmall.addAnimation(translateDown);
        animSetSmall.addAnimation(scaleBubbaSmall);
        scale1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                titleTwo.setVisibility(View.VISIBLE);
                titleTwo.startAnimation(scale2);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        scale2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                titleThree.setVisibility(View.VISIBLE);
                titleThree.startAnimation(scale3);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        scale3.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                splashBubbaSmall.setVisibility(View.VISIBLE);
                splashBubbaSmall.startAnimation(animSetSmall);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        scaleBubbaSmall.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                splashBubbaSmall.setVisibility(View.GONE);
                splashBubbaBig.startAnimation(moveOffScreen);
                splashBubbaBig.setVisibility(View.VISIBLE);
                splashBubbaBig.startAnimation(animSetBig);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        scaleBubbaBig.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                splashAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        titleOne.setVisibility(View.VISIBLE);
        titleOne.startAnimation(scale1);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(updateBubble);
    }
}
