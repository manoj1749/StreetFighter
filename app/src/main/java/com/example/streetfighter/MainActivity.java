package com.example.streetfighter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import pl.droidsonroids.gif.GifTextView;

public class MainActivity extends AppCompatActivity {
    //region Declarations beyond onCreate
    SharedPreferences sp;
    RelativeLayout layout_joystick;
    ObjectAnimator animatefireball1,npc1movedown;
    ObjectAnimator animateavatary,animateavatarx,animateavatary2,animateavatarx2;
    GifTextView avatar, animationlayer1
            ,animationlayer5, npc1;
    pl.droidsonroids.gif.GifImageView animationlayer2;
    JoyStickClass js;
    private int npc1HitCount = 0; // Track the number of hits
    // Declare variables to track collisions and duration
    private int collisionCount = 0;
    private boolean isContactOngoing = false; // Track if contact is ongoing
    private long contactStartTime = 0L; // Track when contact started
    private Handler contactHandler = new Handler();
    private boolean isGameOver = false; // Tracks if the game is over
    private int score = 0; // Tracks the player's score


    //endregion
    @SuppressLint("ClickableViewAccessibility")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        scoreTextView = findViewById(R.id.scoreTextView);

        // Update the score when it changes
        updateScoreUI();
        //region Declarations
        sp = getSharedPreferences("MyUserPrefs", Context.MODE_PRIVATE);
        avatar = (pl.droidsonroids.gif.GifTextView) findViewById(R.id.avatar);
        npc1 = (pl.droidsonroids.gif.GifTextView) findViewById(R.id.npc1);

        animationlayer1 = (pl.droidsonroids.gif.GifTextView) findViewById(R.id.animationlayer1);
        animationlayer2 = (pl.droidsonroids.gif.GifImageView) findViewById(R.id.animationlayer2);
        animationlayer5 = (pl.droidsonroids.gif.GifTextView) findViewById(R.id.animationlayer5);
        layout_joystick = (RelativeLayout)findViewById(R.id.layout_joystick);
        ImageView skill1 = (ImageView) findViewById(R.id.skill1);
        ImageView skill2 = (ImageView) findViewById(R.id.skill2);
        ImageView skill3 = (ImageView) findViewById(R.id.skill3);
        ImageView avatarshadow = (ImageView) findViewById(R.id.avatarshadow);
        //endregion
        //region Default outset settings
        js = new JoyStickClass(getApplicationContext(), layout_joystick, R.drawable.image_button);
        js.setStickSize(150, 150);
        js.setLayoutSize(500, 500);
        js.setLayoutAlpha(150);
        js.setStickAlpha(100);
        js.setOffset(90);
        js.setMinimumDistance(50);

        // Get screen dimensions
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // Safe distance from avatar
        float safeDistanceFromAvatar = 300f; // Adjust this as needed

        // Generate random spawn point for NPC1
        float[] npc1SpawnPoint = getRandomSpawnPoint(avatar, safeDistanceFromAvatar, screenWidth, screenHeight);

        npc1.setX(npc1SpawnPoint[0]);
        npc1.setY(npc1SpawnPoint[1]);

        SharedPreferences.Editor editor = sp.edit();
        editor.putString("upleft", "0");
        editor.apply();
        editor.putString("upright", "0");
        editor.apply();
        editor.putString("downleft", "0");
        editor.apply();
        editor.putString("downright", "0");
        editor.apply();
        editor.putString("up", "0");
        editor.apply();
        editor.putString("downmoving", "0");
        editor.apply();
        editor.putString("right", "0");
        editor.apply();
        editor.putString("left", "0");
        editor.apply();
        editor.putString("fireballstopatone", "0");
        editor.apply();
        editor.putString("lightningstopatone", "0");
        editor.apply();
        editor.putString("npc1beinghit", "0");
        editor.apply();
        editor.putString("hit", "0");
        editor.apply();
        editor.putString("walkingdisabled", "0");
        editor.apply();
        down();
        npc1down();
        npc1MoveTowardsAvatar(npc1);

        startCollisionCheck();
        //endregion
        //region Skill 1,2,3 OnClickListeners
        skill1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lightaura();
            }
        });
        skill2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("walkingdisabled", "1");
                editor.apply();
                fireball();
                //walking has to be disabled when skill is cast
                //for a short moment because animated values need time to be updated
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editor.putString("walkingdisabled", "0");
                        editor.apply();
                    }
                }, 500);
            }
        });
        skill3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isGameOver) {
                    resetGame(); // Restart the game if it's over
                }
            }
        });
        //endregion
        //region Infinite Loop Checker for every 10ms
        //infinite loop checker for every 10ms to check the joystick's direction
        //this is done through the use of sharedpreferences (saved data when we putString() at ontouchlistener)
        //we use ObjectAnimator to move the avatar according to joystick's direction
        final Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                //personal settings (avatar movement)
                float slowspeed = 4; //increase this value to make the speed slower
                long animationduration = 50; //increase this lengthens the animation time resulting in longer reaction time
                //
                //getString retrieves the saved information (in this case, its a string)
                //"0" is the default return when string is null (i.e. empty).
                String walkingdisabled = sp.getString("walkingdisabled","0");
                if (walkingdisabled.equals("0")) {
                    String joystickdirection = sp.getString("joystickdirection", "0");
                    if (joystickdirection.equals("left")) {
                        left();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "0");
                        editor.apply();
                        editor.putString("upright", "0");
                        editor.apply();
                        editor.putString("downleft", "0");
                        editor.apply();
                        editor.putString("downright", "0");
                        editor.apply();
                        editor.putString("up", "0");
                        editor.apply();
                        editor.putString("downmoving", "0");
                        editor.apply();
                        editor.putString("right", "0");
                        editor.apply();
                        editor.putString("left", "1");
                        editor.apply();
                        animateavatarx = animateavatarx.ofFloat(avatar, "x", avatar.getX(), avatar.getX() - avatar.getWidth() / slowspeed);
                        animateavatarx.setDuration(animationduration);
                        animateavatarx.setInterpolator(new LinearInterpolator());
                        animateavatarx.start();
                        //layer
                        animateavatarx2 = animateavatarx2.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX() - avatar.getWidth() / slowspeed);
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        animateavatary2 = animateavatary2.ofFloat(avatarshadow, "y", avatar.getY(), avatar.getY());
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();

                        //listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        //

                    }
                    if (joystickdirection.equals("right")) {
                        right();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "0");
                        editor.apply();
                        editor.putString("upright", "0");
                        editor.apply();
                        editor.putString("downleft", "0");
                        editor.apply();
                        editor.putString("downright", "0");
                        editor.apply();
                        editor.putString("up", "0");
                        editor.apply();
                        editor.putString("downmoving", "0");
                        editor.apply();
                        editor.putString("left", "0");
                        editor.apply();
                        editor.putString("right", "1");
                        editor.apply();
                        animateavatarx = animateavatarx.ofFloat(avatar, "x", avatar.getX(), avatar.getX() + avatar.getWidth() / slowspeed);
                        animateavatarx.setDuration(animationduration);
                        animateavatarx.setInterpolator(new LinearInterpolator());
                        animateavatarx.start();
                        //layer
                        animateavatarx2 = animateavatarx2.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX() + avatar.getWidth() / slowspeed);
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        animateavatary2 = animateavatary2.ofFloat(avatarshadow, "y", avatar.getY(), avatar.getY());
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();

                        //region listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        animateavatary2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageYPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageYPosition", imageYPosition);
                                editor.apply();
                                animationlayer1.setY(imageYPosition + avatar.getHeight() - animationlayer1.getHeight());
                            }
                        });
                        //endregion

                    }
                    if (joystickdirection.equals("up")) {
                        up();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "0");
                        editor.apply();
                        editor.putString("upright", "0");
                        editor.apply();
                        editor.putString("downleft", "0");
                        editor.apply();
                        editor.putString("downright", "0");
                        editor.apply();
                        editor.putString("up", "1");
                        editor.apply();
                        editor.putString("downmoving", "0");
                        editor.apply();
                        editor.putString("left", "0");
                        editor.apply();
                        editor.putString("right", "0");
                        editor.apply();
                        animateavatary = animateavatary.ofFloat(avatar, "Y", avatar.getY(), avatar.getY() - avatar.getHeight() / slowspeed);
                        animateavatary.setDuration(animationduration);
                        animateavatary.setInterpolator(new LinearInterpolator());
                        animateavatary.start();
                        //layer
                        animateavatary2 = animateavatary2.ofFloat(avatarshadow, "Y", avatar.getY(), avatar.getY() - avatar.getHeight() / slowspeed);
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();

                        animateavatarx2 = animateavatarx2.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX());
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        //region listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        animateavatary2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageYPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageYPosition", imageYPosition);
                                editor.apply();
                                animationlayer1.setY(imageYPosition + avatar.getHeight() - animationlayer1.getHeight());
                            }
                        });
                        //endregion

                    }
                    if (joystickdirection.equals("down")) {
                        downmoving();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "0");
                        editor.apply();
                        editor.putString("upright", "0");
                        editor.apply();
                        editor.putString("downleft", "0");
                        editor.apply();
                        editor.putString("downright", "0");
                        editor.apply();
                        editor.putString("up", "0");
                        editor.apply();
                        editor.putString("downmoving", "1");
                        editor.apply();
                        editor.putString("left", "0");
                        editor.apply();
                        editor.putString("right", "0");
                        editor.apply();
                        animateavatary = animateavatary.ofFloat(avatar, "Y", avatar.getY(), avatar.getY() + avatar.getHeight() / slowspeed);
                        animateavatary.setDuration(animationduration);
                        animateavatary.setInterpolator(new LinearInterpolator());
                        animateavatary.start();
                        //layer
                        animateavatary2 = animateavatary2.ofFloat(avatarshadow, "Y", avatar.getY(), avatar.getY() + avatar.getHeight() / slowspeed);
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();

                        animateavatarx2 = animateavatarx2.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX());
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        //region listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        animateavatary2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageYPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageYPosition", imageYPosition);
                                editor.apply();
                                animationlayer1.setY(imageYPosition + avatar.getHeight() - animationlayer1.getHeight());
                            }
                        });
                        //endregion

                    }
                    if (joystickdirection.equals("upright")) {
                        upright();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "0");
                        editor.apply();
                        editor.putString("upright", "1");
                        editor.apply();
                        editor.putString("downleft", "0");
                        editor.apply();
                        editor.putString("downright", "0");
                        editor.apply();
                        editor.putString("up", "0");
                        editor.apply();
                        editor.putString("downmoving", "0");
                        editor.apply();
                        editor.putString("left", "0");
                        editor.apply();
                        editor.putString("right", "0");
                        editor.apply();
                        animateavatary = animateavatary.ofFloat(avatar, "Y", avatar.getY(), avatar.getY() - avatar.getHeight() / slowspeed);
                        animateavatary.setDuration(animationduration);
                        animateavatary.setInterpolator(new LinearInterpolator());
                        animateavatary.start();
                        animateavatarx = animateavatarx.ofFloat(avatar, "x", avatar.getX(), avatar.getX() + avatar.getWidth() / slowspeed);
                        animateavatarx.setDuration(animationduration);
                        animateavatarx.setInterpolator(new LinearInterpolator());
                        animateavatarx.start();
                        //layer
                        animateavatary2 = animateavatary2.ofFloat(avatarshadow, "Y", avatar.getY(), avatar.getY() - avatar.getHeight() / slowspeed);
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();
                        animateavatarx2 = animateavatarx2.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX() + avatar.getWidth() / slowspeed);
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        //region listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        animateavatary2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageYPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageYPosition", imageYPosition);
                                editor.apply();
                                animationlayer1.setY(imageYPosition + avatar.getHeight() - animationlayer1.getHeight());
                            }
                        });
                        //endregion

                    }
                    if (joystickdirection.equals("downright")) {
                        downright();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "0");
                        editor.apply();
                        editor.putString("upright", "0");
                        editor.apply();
                        editor.putString("downleft", "0");
                        editor.apply();
                        editor.putString("downright", "1");
                        editor.apply();
                        editor.putString("up", "0");
                        editor.apply();
                        editor.putString("downmoving", "0");
                        editor.apply();
                        editor.putString("left", "0");
                        editor.apply();
                        editor.putString("right", "0");
                        editor.apply();
                        animateavatary = animateavatary.ofFloat(avatar, "Y", avatar.getY(), avatar.getY() + avatar.getHeight() / slowspeed);
                        animateavatary.setDuration(animationduration);
                        animateavatary.setInterpolator(new LinearInterpolator());
                        animateavatary.start();
                        animateavatarx = animateavatarx.ofFloat(avatar, "x", avatar.getX(), avatar.getX() + avatar.getWidth() / slowspeed);
                        animateavatarx.setDuration(animationduration);
                        animateavatarx.setInterpolator(new LinearInterpolator());
                        animateavatarx.start();
                        //layer
                        animateavatary2 = animateavatary2.ofFloat(avatarshadow, "Y", avatar.getY(), avatar.getY() + avatar.getHeight() / slowspeed);
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();
                        animateavatarx2 = animateavatarx2.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX() + avatar.getWidth() / slowspeed);
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        //region listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        animateavatary2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageYPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageYPosition", imageYPosition);
                                editor.apply();
                                animationlayer1.setY(imageYPosition + avatar.getHeight() - animationlayer1.getHeight());
                            }
                        });
                        //endregion
                    }
                    if (joystickdirection.equals("downleft")) {
                        downleft();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "0");
                        editor.apply();
                        editor.putString("upright", "0");
                        editor.apply();
                        editor.putString("downleft", "1");
                        editor.apply();
                        editor.putString("downright", "0");
                        editor.apply();
                        editor.putString("up", "0");
                        editor.apply();
                        editor.putString("downmoving", "0");
                        editor.apply();
                        editor.putString("left", "0");
                        editor.apply();
                        editor.putString("right", "0");
                        editor.apply();
                        animateavatary = animateavatary.ofFloat(avatar, "Y", avatar.getY(), avatar.getY() + avatar.getHeight() / slowspeed);
                        animateavatary.setDuration(animationduration);
                        animateavatary.setInterpolator(new LinearInterpolator());
                        animateavatary.start();
                        animateavatarx = animateavatarx.ofFloat(avatar, "x", avatar.getX(), avatar.getX() - avatar.getWidth() / slowspeed);
                        animateavatarx.setDuration(animationduration);
                        animateavatarx.setInterpolator(new LinearInterpolator());
                        animateavatarx.start();
                        //layer
                        animateavatary2 = animateavatary2.ofFloat(avatarshadow, "Y", avatar.getY(), avatar.getY() + avatar.getHeight() / slowspeed);
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();
                        animateavatarx2 = animateavatarx2.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX() - avatar.getWidth() / slowspeed);
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        //region listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        animateavatary2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageYPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageYPosition", imageYPosition);
                                editor.apply();
                                animationlayer1.setY(imageYPosition + avatar.getHeight() - animationlayer1.getHeight());
                            }
                        });
                        //endregion
                    }
                    if (joystickdirection.equals("upleft")) {
                        upleft();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("upleft", "1");
                        editor.apply();
                        editor.putString("upright", "0");
                        editor.apply();
                        editor.putString("downleft", "0");
                        editor.apply();
                        editor.putString("downright", "0");
                        editor.apply();
                        editor.putString("up", "0");
                        editor.apply();
                        editor.putString("downmoving", "0");
                        editor.apply();
                        editor.putString("left", "0");
                        editor.apply();
                        editor.putString("right", "0");
                        editor.apply();
                        animateavatary = animateavatary.ofFloat(avatar, "Y", avatar.getY(), avatar.getY() - avatar.getHeight() / slowspeed);
                        animateavatary.setDuration(animationduration);
                        animateavatary.setInterpolator(new LinearInterpolator());
                        animateavatary.start();
                        animateavatarx = animateavatarx.ofFloat(avatar, "x", avatar.getX(), avatar.getX() - avatar.getWidth() / slowspeed);
                        animateavatarx.setDuration(animationduration);
                        animateavatarx.setInterpolator(new LinearInterpolator());
                        animateavatarx.start();
                        //layer
                        animateavatary2 = animateavatary.ofFloat(avatarshadow, "Y", avatar.getY(), avatar.getY() - avatar.getHeight() / slowspeed);
                        animateavatary2.setDuration(animationduration);
                        animateavatary2.setInterpolator(new LinearInterpolator());
                        animateavatary2.start();
                        animateavatarx2 = animateavatarx.ofFloat(avatarshadow, "x", avatar.getX(), avatar.getX() - avatar.getWidth() / slowspeed);
                        animateavatarx2.setDuration(animationduration);
                        animateavatarx2.setInterpolator(new LinearInterpolator());
                        animateavatarx2.start();

                        //region listener to update values
                        animateavatarx2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageXPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageXPosition", imageXPosition);
                                editor.apply();
                                animationlayer1.setX(imageXPosition - animationlayer1.getWidth() / 2 + avatar.getWidth() / 2);
                            }
                        });
                        animateavatary2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                Float imageYPosition = (Float) animation.getAnimatedValue();
                                editor.putFloat("imageYPosition", imageYPosition);
                                editor.apply();
                                animationlayer1.setY(imageYPosition + avatar.getHeight() - animationlayer1.getHeight());
                            }
                        });
                        //endregion

                    }

                }
                handler1.postDelayed(this, 10);
            }
        }, 10);
        //endregion
        //region Joystick OnTouchListener
//        layout_joystick.setOnTouchListener(new OnTouchListener() {
//            public boolean onTouch(View arg0, MotionEvent arg1) {
//                js.drawStick(arg1);
//                if(arg1.getAction() == MotionEvent.ACTION_DOWN
//                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
//                    int direction = js.get8Direction();
//                    if(direction == JoyStickClass.STICK_UP) {
//                        //this saves the joystick direction to the string "joystickdirection"
//                        //it is retrievable using sp.getString("joystickdirection","0");
//                        //"0" is the default return when string is null (i.e. empty).
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "up");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_UPRIGHT) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "upright");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_RIGHT) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "right");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_DOWNRIGHT) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "downright");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_DOWN) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "down");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_DOWNLEFT) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "downleft");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_LEFT) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "left");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_UPLEFT) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "upleft");
//                        editor.apply();
//                    } else if(direction == JoyStickClass.STICK_NONE) {
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("joystickdirection", "stop");
//                        editor.apply();
//                    }
//                } else if(arg1.getAction() == MotionEvent.ACTION_UP) {
//                    down();
//                    SharedPreferences.Editor editor = sp.edit();
//                    editor.putString("upleft", "0");
//                    editor.apply();
//                    editor.putString("upright", "0");
//                    editor.apply();
//                    editor.putString("downleft", "0");
//                    editor.apply();
//                    editor.putString("downright", "0");
//                    editor.apply();
//                    editor.putString("up", "0");
//                    editor.apply();
//                    editor.putString("downmoving", "0");
//                    editor.apply();
//                    editor.putString("left", "0");
//                    editor.apply();
//                    editor.putString("right", "0");
//                    editor.apply();
//                    editor.putString("joystickdirection", "stop");
//                    editor.apply();
//                }
//                return true;
//            }
//        });
        layout_joystick.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js.drawStick(arg1);

                // Get screen dimensions
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;

                if (arg1.getAction() == MotionEvent.ACTION_DOWN || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    int direction = js.get8Direction();
                    SharedPreferences.Editor editor = sp.edit();
                    float avatarX = avatar.getX();
                    float avatarY = avatar.getY();
                    float avatarWidth = avatar.getWidth();
                    float avatarHeight = avatar.getHeight();

                    if (direction == JoyStickClass.STICK_UP) {
                        editor.putString("joystickdirection", "up");
                        editor.apply();
                        avatarY = clamp(avatarY - 10, 0, screenHeight - avatarHeight);
                    } else if (direction == JoyStickClass.STICK_UPRIGHT) {
                        editor.putString("joystickdirection", "upright");
                        editor.apply();
                        avatarX = clamp(avatarX + 10, 0, screenWidth - avatarWidth);
                        avatarY = clamp(avatarY - 10, 0, screenHeight - avatarHeight);
                    } else if (direction == JoyStickClass.STICK_RIGHT) {
                        editor.putString("joystickdirection", "right");
                        editor.apply();
                        avatarX = clamp(avatarX + 10, 0, screenWidth - avatarWidth);
                    } else if (direction == JoyStickClass.STICK_DOWNRIGHT) {
                        editor.putString("joystickdirection", "downright");
                        editor.apply();
                        avatarX = clamp(avatarX + 10, 0, screenWidth - avatarWidth);
                        avatarY = clamp(avatarY + 10, 0, screenHeight - avatarHeight);
                    } else if (direction == JoyStickClass.STICK_DOWN) {
                        editor.putString("joystickdirection", "down");
                        editor.apply();
                        avatarY = clamp(avatarY + 10, 0, screenHeight - avatarHeight);
                    } else if (direction == JoyStickClass.STICK_DOWNLEFT) {
                        editor.putString("joystickdirection", "downleft");
                        editor.apply();
                        avatarX = clamp(avatarX - 10, 0, screenWidth - avatarWidth);
                        avatarY = clamp(avatarY + 10, 0, screenHeight - avatarHeight);
                    } else if (direction == JoyStickClass.STICK_LEFT) {
                        editor.putString("joystickdirection", "left");
                        editor.apply();
                        avatarX = clamp(avatarX - 10, 0, screenWidth - avatarWidth);
                    } else if (direction == JoyStickClass.STICK_UPLEFT) {
                        editor.putString("joystickdirection", "upleft");
                        editor.apply();
                        avatarX = clamp(avatarX - 10, 0, screenWidth - avatarWidth);
                        avatarY = clamp(avatarY - 10, 0, screenHeight - avatarHeight);
                    } else if (direction == JoyStickClass.STICK_NONE) {
                        editor.putString("joystickdirection", "stop");
                        editor.apply();
                    }

                    avatar.setX(avatarX);
                    avatar.setY(avatarY);

                } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                    down();
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("upleft", "0");
                    editor.putString("upright", "0");
                    editor.putString("downleft", "0");
                    editor.putString("downright", "0");
                    editor.putString("up", "0");
                    editor.putString("downmoving", "0");
                    editor.putString("left", "0");
                    editor.putString("right", "0");
                    editor.putString("joystickdirection", "stop");
                    editor.apply();
                }
                return true;
            }
        });

        //endregion
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    // Collision check function
    private void checkCollision() {
        checkIndividualCollision(npc1);
    }

    private void checkIndividualCollision(GifTextView npc) {
        // Convert both avatar and NPC to bitmaps for precise collision
        Bitmap avatarBitmap = getViewBitmap(avatar);
        Bitmap npcBitmap = getViewBitmap(npc);

        // Determine the overlapping area
        int left = (int) Math.max(avatar.getX(), npc.getX());
        int top = (int) Math.max(avatar.getY(), npc.getY());
        int right = (int) Math.min(avatar.getX() + avatar.getWidth(), npc.getX() + npc.getWidth());
        int bottom = (int) Math.min(avatar.getY() + avatar.getHeight(), npc.getY() + npc.getHeight());

        if (left >= right || top >= bottom) {
            // No overlapping region
            return;
        }

        // Loop through the overlapping area and check for non-transparent pixels
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                int avatarBitmapX = (int) (x - avatar.getX());
                int avatarBitmapY = (int) (y - avatar.getY());
                int npcBitmapX = (int) (x - npc.getX());
                int npcBitmapY = (int) (y - npc.getY());

                if (isPixelNonTransparent(avatarBitmap, avatarBitmapX, avatarBitmapY) &&
                        isPixelNonTransparent(npcBitmap, npcBitmapX, npcBitmapY)) {
                    handleCollision(npc);
                    return;
                }
            }
        }
    }


    private void handleCollision(GifTextView npc) {
        if (!isContactOngoing) {
            isContactOngoing = true;
            contactStartTime = System.currentTimeMillis();

            // Start a handler to check contact duration
            contactHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isContactOngoing && System.currentTimeMillis() - contactStartTime >= 4000) {
                        onPlayerDeath();
                    }
                }
            }, 4000);
        }

        collisionCount++;
        if (collisionCount >= 4) {
            onPlayerDeath();
        }
    }

    private Bitmap getViewBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private boolean isPixelNonTransparent(Bitmap bitmap, int x, int y) {
        if (x < 0 || y < 0 || x >= bitmap.getWidth() || y >= bitmap.getHeight()) {
            return false;
        }
        return (bitmap.getPixel(x, y) & 0xFF000000) != 0; // Check if alpha is non-zero
    }


    // Call this method to trigger a "death" event
    private void onPlayerDeath() {
        isContactOngoing = false; // Stop contact tracking
        collisionCount = 0; // Reset collision count
        npc1HitCount = 0;
        stopCollisionCheck(); // Ensure collision tracking is halted
        isGameOver = true;

        // Hide the avatar and NPC
        avatar.setVisibility(View.INVISIBLE);
        npc1.setVisibility(View.INVISIBLE);

        // Optional: Log for debugging
        System.out.println("Game Over!");

        // Navigate back to EntryActivity
        Intent intent = new Intent(MainActivity.this, EntryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the back stack
        startActivity(intent);
        finish(); // End the current activity
    }


    private Handler collisionCheckHandler = new Handler();

    // Call `checkCollision` periodically (e.g., every 100ms in a handler)

    private void startCollisionCheck() {
        collisionCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (npc1.getVisibility() == View.VISIBLE) { // Only check collision if NPC is active
                    checkCollision();
                    collisionCheckHandler.postDelayed(this, 100); // Repeat every 100ms
                }
            }
        }, 100);
    }
    private void stopCollisionCheck() {
        collisionCheckHandler.removeCallbacksAndMessages(null); // Stop all collision checks
    }

    //region Avatar's animation based on direction of joystick
    public void right() {
        String right = sp.getString("right", "0");
        if (right.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.right);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }
    public void left() {
        String left = sp.getString("left", "0");
        if (left.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.left);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }
    public void down() {
        Animation fadeout = new AlphaAnimation(1f, 1f);
        fadeout.setDuration(2500); // You can modify the duration here
        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                avatar.setBackgroundResource(R.drawable.down);//your gif file
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });
        avatar.startAnimation(fadeout);
    }

    public void downmoving() {
        String downmoving = sp.getString("downmoving", "0");
        if (downmoving.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.downmoving);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }
    public void up() {
        String up = sp.getString("up", "0");
        if (up.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.up);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }

    public void downleft() {
        String downleft = sp.getString("downleft", "0");
        if (downleft.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.downleft);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }
    public void downright() {
        String downright = sp.getString("downright", "0");
        if (downright.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.downright);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }
    public void upright() {
        String upright = sp.getString("upright", "0");
        if (upright.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.upright);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }
    public void upleft() {
        String upleft = sp.getString("upleft", "0");
        if (upleft.equals("0")) {
            Animation fadeout = new AlphaAnimation(1f, 1f);
            fadeout.setDuration(2500); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    avatar.setBackgroundResource(R.drawable.upleft);//your gif file
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            avatar.startAnimation(fadeout);
        }
    }
    //endregion
    //region NPC1 being hit
    public void npc1beinghit() {
        npc1HitCount++; // Increment hit count
        score += 10; // Increment score by 10 for each hit
        updateScoreUI(); // Update UI
        // Log or update UI with the current score
        System.out.println("NPC hit! Current Score: " + score);

        // Play hit animation
        Animation fadeout = new AlphaAnimation(1f, 1f);
        fadeout.setDuration(2500); // Adjust duration as needed
        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                npc1.setBackgroundResource(R.drawable.hit); // NPC1 hit GIF
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                npc1down(); // Return to default state
            }
        });
        npc1.startAnimation(fadeout);

        // Respawn logic remains the same, but NPC won't "die"
        if (npc1HitCount >= 3) {
            System.out.println("NPC temporarily retreating...");

            // Hide NPC and respawn after a delay
            npc1.setVisibility(View.INVISIBLE);
            stopCollisionCheck();

            final Handler respawnHandler = new Handler();
            respawnHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Generate a new spawn point
                    float[] newSpawnPoint = getRandomSpawnPoint(avatar, 300f,
                            getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels);
                    npc1.setX(newSpawnPoint[0]);
                    npc1.setY(newSpawnPoint[1]);
                    npc1.setVisibility(View.VISIBLE);
                    npc1HitCount = 0; // Reset hit count

                    System.out.println("NPC respawned at: X=" + npc1.getX() + ", Y=" + npc1.getY());

                    // Restart collision checks
                    startCollisionCheck();
                }
            }, 2000); // 2-second delay
        }
    }

    private void updateScoreUI() {
//        runOnUiThread(() -> scoreTextView.setText("Score: " + score));
        System.out.println("Score: "+score);
    }

    private void resetGame() {
        // Reset game variables
        isGameOver = false;
        collisionCount = 0;
        npc1HitCount = 0;

        // Reset positions
        avatar.setVisibility(View.VISIBLE);
        npc1.setVisibility(View.VISIBLE);
        avatar.setX(100); // Set to initial position
        avatar.setY(100);
        npc1.setX(800); // Set to initial position
        npc1.setY(800);

        // Hide Game Over message
        findViewById(R.id.gameOverMessage).setVisibility(View.GONE);

        // Restart collision checks and movements
        startCollisionCheck();
        npc1MoveTowardsAvatar(npc1);

        System.out.println("Game restarted!");
    }


    //endregion
    //region NPC1's face direction: downleft
    public void npc1down() {
        Animation fadeout = new AlphaAnimation(1f, 1f);
        fadeout.setDuration(2500); // You can modify the duration here
        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                npc1.setBackgroundResource(R.drawable.downleft);//your gif file
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });
        npc1.startAnimation(fadeout);
    }

    // Function to move NPC towards the avatar in 4 directions
    // Updated movement logic for NPCs
    public void npc1MoveTowardsAvatar(GifTextView npc) {
        final Handler npcHandler = new Handler();
        npcHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                float avatarX = avatar.getX();
                float avatarY = avatar.getY();
                float npcX = npc.getX();
                float npcY = npc.getY();

                String direction = "none";

                if (Math.abs(avatarX - npcX) > Math.abs(avatarY - npcY)) {
                    if (avatarX > npcX) {
                        direction = "right";
                    } else {
                        direction = "left";
                    }
                } else {
                    if (avatarY > npcY) {
                        direction = "down";
                    } else {
                        direction = "up";
                    }
                }

                switch (direction) {
                    case "up":
                        moveNPC(npc, "y", npcY, npcY - npc.getHeight() / 8);
                        break;
                    case "down":
                        moveNPC(npc, "y", npcY, npcY + npc.getHeight() / 8);
                        break;
                    case "left":
                        moveNPC(npc, "x", npcX, npcX - npc.getWidth() / 8);
                        break;
                    case "right":
                        moveNPC(npc, "x", npcX, npcX + npc.getWidth() / 8);
                        break;
                }

                npcHandler.postDelayed(this, 100);
            }
        }, 100);
    }
    // Helper function to move NPC in a specific direction
    private void moveNPC(GifTextView npc, String axis, float start, float end) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(npc, axis, start, end);
        animator.setDuration(100);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    //endregion
    //region NPC1's route (move downwards for 18s and subsequently, upwards for the next 18s)
    public void npc1movedown() {
        npc1movedown = npc1movedown.ofFloat(npc1, "Y", npc1.getY(),1800);
        npc1movedown.setDuration(18000);
        npc1movedown.start();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Float npc1YPosition = sp.getFloat("npc1YPosition",0);
                npc1movedown = npc1movedown.ofFloat(npc1, "Y", npc1YPosition,100);
                npc1movedown.setDuration(18000);
                npc1movedown.start();
                npc1movedown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Float npc1YPosition = (Float) animation.getAnimatedValue();
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putFloat("npc1YPosition", npc1YPosition);
                        editor.apply();
                    }
                });
            }
        }, 18000);

        npc1movedown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float npc1YPosition = (Float) animation.getAnimatedValue();
                SharedPreferences.Editor editor = sp.edit();
                editor.putFloat("npc1YPosition", npc1YPosition);
                editor.apply();
            }
        });
    }
    //endregion
    //region Three Skills - fireball, lightaura and lightning
//    public void fireball() {
//        String fireballstopatone = sp.getString("fireballstopatone","0");
//        if (fireballstopatone.equals("0")) {
//            Float imageXPosition = (Float)avatar.getX();
//            Float imageYPosition = (Float)avatar.getY();
//            SharedPreferences.Editor editor = sp.edit();
//            editor.putString("fireballstopatone", "1");
//            editor.apply();
//            editor.putFloat("imageXPosition", imageXPosition);
//            editor.apply();
//            editor.putFloat("imageYPosition", imageYPosition);
//            editor.apply();
//            Float x = sp.getFloat("imageXPosition",0);
//            Float y = sp.getFloat("imageYPosition",0);
//            animationlayer2.setX(x+avatar.getWidth());
//            animationlayer2.setY(y+avatar.getHeight()/4);
//            avatar.setBackgroundResource(R.drawable.right);
//            animationlayer2.setVisibility(View.VISIBLE);
//            animatefireball1 = animatefireball1.ofFloat(animationlayer2, "X", animationlayer2.getX(),animationlayer2.getX()+5*animationlayer2.getWidth());
//            animatefireball1.setDuration(1000);
//            animatefireball1.start();
//            animatefireball1.addListener(new Animator.AnimatorListener() {
//                @Override
//                public void onAnimationStart(Animator animator) {
//                }
//                @Override
//                public void onAnimationEnd(Animator animator) {
//                    editor.putString("fireballstopatone", "0");
//                    editor.apply();
//                    animationlayer2.setVisibility(View.INVISIBLE);
//                }
//                @Override
//                public void onAnimationCancel(Animator animator) {
//                }
//                @Override
//                public void onAnimationRepeat(Animator animator) {
//                }
//            });
//
//            animatefireball1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                @Override
//                public void onAnimationUpdate(ValueAnimator animation) {
//                    Float fireball1XPosition = (Float)animation.getAnimatedValue();
//                    //damaged zone of fireball1
//                    Float x2 = sp.getFloat("imageXPosition",0);
//                    Float y2 = sp.getFloat("imageYPosition",0);
//                    //from 'start of length (start)' to 'end of length (end)'
//                    Float damagedstartx2 = fireball1XPosition;
//                    Float damagedendx2 = fireball1XPosition + animationlayer2.getWidth();
//                    //from 'bottom of height (start)' to 'top of height (end)'
//                    Float damagedstarty2 = y2 + avatar.getHeight();
//                    Float damagedendy2 = y2 + avatar.getHeight() - animationlayer2.getHeight();
//                    //npc1 zone
//                    Float npc1YPosition = sp.getFloat("npc1YPosition",0);
//                    Float npc1midx = npc1.getX() + npc1.getWidth()/2;
//                    Float npc1midy = npc1YPosition + npc1.getHeight()/2;
//                    String fireballstopatone = sp.getString("fireballstopatone","0");
//                    String npc1beinghit = sp.getString("npc1beinghit","0");
//                    //check for npc1 damaged
//                    if (npc1midx >= damagedstartx2 && npc1midx <= damagedendx2
//                            && npc1midy <= damagedstarty2 && npc1midy >= damagedendy2)
//                    {
//                        animationlayer2.setVisibility(View.INVISIBLE);
//                        npc1beinghit();
//                    }
//                    //
//                }
//            });
//        }
//    }

    public void fireball() {
        String fireballstopatone = sp.getString("fireballstopatone", "0");
        if (fireballstopatone.equals("0")) {
            // Get positions of the avatar and npc1
            float avatarX = avatar.getX();
            float avatarY = avatar.getY();
            float npc1X = npc1.getX();
            float npc1Y = npc1.getY();

            // Determine the direction to fire (restrict to 4 directions)
            String direction;
            if (Math.abs(avatarX - npc1X) > Math.abs(avatarY - npc1Y)) {
                // Horizontal movement
                if (npc1X > avatarX) {
                    direction = "right";
                } else {
                    direction = "left";
                }
            } else {
                // Vertical movement
                if (npc1Y > avatarY) {
                    direction = "down";
                } else {
                    direction = "up";
                }
            }

            // Set fireball initial position
            animationlayer2.setX(avatarX + avatar.getWidth() / 2);
            animationlayer2.setY(avatarY + avatar.getHeight() / 2);

            // Set fireball rotation based on direction
            switch (direction) {
                case "right":
                    animationlayer2.setRotation(0); // No rotation
                    break;
                case "left":
                    animationlayer2.setRotation(180); // Rotate 180 degrees
                    break;
                case "down":
                    animationlayer2.setRotation(90); // Rotate 90 degrees
                    break;
                case "up":
                    animationlayer2.setRotation(270); // Rotate 270 degrees
                    break;
            }

            // Ensure visibility
            animationlayer2.setVisibility(View.VISIBLE);

            // Target positions based on direction
            float targetX = animationlayer2.getX();
            float targetY = animationlayer2.getY();

            switch (direction) {
                case "right":
                    targetX += 1000; // Move right
                    break;
                case "left":
                    targetX -= 1000; // Move left
                    break;
                case "down":
                    targetY += 1000; // Move down
                    break;
                case "up":
                    targetY -= 1000; // Move up
                    break;
            }

            // Debug: Log the direction and target position
            System.out.println("Fireball direction: " + direction + ", TargetX: " + targetX + ", TargetY: " + targetY);

            // Create ObjectAnimators for fireball movement
            ObjectAnimator fireballAnimatorX = ObjectAnimator.ofFloat(animationlayer2, "x", animationlayer2.getX(), targetX);
            ObjectAnimator fireballAnimatorY = ObjectAnimator.ofFloat(animationlayer2, "y", animationlayer2.getY(), targetY);

            // Set animation duration
            fireballAnimatorX.setDuration(1000);
            fireballAnimatorY.setDuration(1000);

            fireballAnimatorX.setInterpolator(new LinearInterpolator());
            fireballAnimatorY.setInterpolator(new LinearInterpolator());

            fireballAnimatorX.start();
            fireballAnimatorY.start();

            // Collision detection
            fireballAnimatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fireballX = animationlayer2.getX();
                    float fireballY = animationlayer2.getY();
                    float fireballWidth = animationlayer2.getWidth();
                    float fireballHeight = animationlayer2.getHeight();

                    // Fireball bounding box
                    float fireballLeft = fireballX;
                    float fireballRight = fireballX + fireballWidth;
                    float fireballTop = fireballY;
                    float fireballBottom = fireballY + fireballHeight;

                    // NPC1's bounding box
                    float npc1Left = npc1.getX();
                    float npc1Right = npc1.getX() + npc1.getWidth();
                    float npc1Top = npc1.getY();
                    float npc1Bottom = npc1.getY() + npc1.getHeight();

                    // Check for exact overlap
                    boolean isHit = fireballRight >= npc1Left && fireballLeft <= npc1Right &&
                            fireballBottom >= npc1Top && fireballTop <= npc1Bottom;

                    if (isHit) {
                        System.out.println("Fireball hit NPC1!");
                        animationlayer2.setVisibility(View.INVISIBLE); // Hide fireball
                        fireballAnimatorX.cancel();
                        fireballAnimatorY.cancel();
                        npc1beinghit(); // Call hit logic
                    }
                }
            });


            // Reset fireball visibility after animation ends
            fireballAnimatorX.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("fireballstopatone", "1");
                    editor.apply();
                    animationlayer2.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animationlayer2.setVisibility(View.INVISIBLE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("fireballstopatone", "0");
                    editor.apply();
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    animationlayer2.setVisibility(View.INVISIBLE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("fireballstopatone", "0");
                    editor.apply();
                }

                @Override
                public void onAnimationRepeat(Animator animator) {}
            });
        }
    }

    private float[] getRandomSpawnPoint(View character, float safeDistance, int screenWidth, int screenHeight) {
        float randomX, randomY;

        do {
            randomX = (float) (Math.random() * (screenWidth - npc1.getWidth())); // Restrict to screen width
            randomY = (float) (Math.random() * (screenHeight - npc1.getHeight())); // Restrict to screen height
        } while (calculateDistance(randomX, randomY, character.getX(), character.getY()) < safeDistance);

        return new float[]{randomX, randomY};
    }

    private float calculateDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }


    public void lightaura() {
        Animation fadeout = new AlphaAnimation(0f, 1f);
        fadeout.setDuration(500); // You can modify the duration here
        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animationlayer1.setBackgroundResource(R.drawable.lightaura);//your gif file
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
            }
        });
        animationlayer1.startAnimation(fadeout);
    }

    public void lightning() {
        String lightningstopatone = sp.getString("lightningstopatone","0");
        if (lightningstopatone.equals("0")) {
            Float imageXPosition = (Float)avatar.getX();
            Float imageYPosition = (Float)avatar.getY();
            SharedPreferences.Editor editor = sp.edit();
            editor.putFloat("imageXPosition", imageXPosition);
            editor.apply();
            editor.putFloat("imageYPosition", imageYPosition);
            editor.apply();
            Float x = sp.getFloat("imageXPosition",0);
            Float y = sp.getFloat("imageYPosition",0);
            animationlayer5.setX(x+avatar.getWidth()-avatar.getWidth()/2);
            animationlayer5.setY(y+avatar.getHeight()+avatar.getHeight()/2-animationlayer5.getHeight());
            avatar.setBackgroundResource(R.drawable.right);
            Animation fadeout = new AlphaAnimation(1f, 0f);
            fadeout.setDuration(2000); // You can modify the duration here
            fadeout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    editor.putString("lightningstopatone", "1");
                    editor.apply();
                    animationlayer5.setBackgroundResource(R.drawable.lightning);//your gif file

                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    editor.putString("lightningstopatone", "0");
                    editor.apply();
                }
            });
            animationlayer5.startAnimation(fadeout);
        }
    }
    //endregion
}