package com.example.streetfighter;

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
    //endregion
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        npc1movedown();
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
                lightning();
                Float x = sp.getFloat("imageXPosition",0);
                Float y = sp.getFloat("imageYPosition",0);
                //damaged zone
                Float damagedstartx = x + avatar.getWidth() + (animationlayer5.getWidth())/2;
                Float damagedendx = x + avatar.getWidth() + animationlayer5.getWidth();
                Float damagedstarty = y + avatar.getHeight();
                Float damagedendy = y + avatar.getHeight() - (animationlayer5.getHeight())/2;
                //npc1 zone
                Float npc1midx = npc1.getX();
                Float npc1midy = npc1.getY() + npc1.getHeight();
                String lightningstopatone = sp.getString("lightningstopatone","0");
                String npc1beinghit = sp.getString("npc1beinghit","0");
                //Note: the lag in timing is about 2 to 3 seconds for writing string into sharedpreferences
                //and relaying it here. (not instantaneous)
                //check for npc1 damaged
                if (npc1midx >= damagedstartx && npc1midx <= damagedendx && npc1midy <= damagedstarty && npc1midy >= damagedendy && lightningstopatone.equals("0"))
                {
                    editor.putString("npc1beinghit", "1");
                    editor.apply();
                    if (npc1beinghit.equals("0")) {
                        npc1beinghit();
                    }
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
        layout_joystick.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js.drawStick(arg1);
                if(arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    int direction = js.get8Direction();
                    if(direction == JoyStickClass.STICK_UP) {
                        //this saves the joystick direction to the string "joystickdirection"
                        //it is retrievable using sp.getString("joystickdirection","0");
                        //"0" is the default return when string is null (i.e. empty).
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "up");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_UPRIGHT) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "upright");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_RIGHT) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "right");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_DOWNRIGHT) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "downright");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_DOWN) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "down");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_DOWNLEFT) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "downleft");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_LEFT) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "left");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_UPLEFT) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "upleft");
                        editor.apply();
                    } else if(direction == JoyStickClass.STICK_NONE) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("joystickdirection", "stop");
                        editor.apply();
                    }
                } else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    down();
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
                    editor.putString("right", "0");
                    editor.apply();
                    editor.putString("joystickdirection", "stop");
                    editor.apply();
                }
                return true;
            }
        });
        //endregion
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
        Animation fadeout = new AlphaAnimation(1f, 1f);
        fadeout.setDuration(2500); // You can modify the duration here
        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                npc1.setBackgroundResource(R.drawable.hit);//your gif file
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                SharedPreferences.Editor editor = sp.edit();
                npc1down();
            }
        });
        npc1.startAnimation(fadeout);
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
    public void fireball() {
        String fireballstopatone = sp.getString("fireballstopatone","0");
        if (fireballstopatone.equals("0")) {
            Float imageXPosition = (Float)avatar.getX();
            Float imageYPosition = (Float)avatar.getY();
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("fireballstopatone", "1");
            editor.apply();
            editor.putFloat("imageXPosition", imageXPosition);
            editor.apply();
            editor.putFloat("imageYPosition", imageYPosition);
            editor.apply();
            Float x = sp.getFloat("imageXPosition",0);
            Float y = sp.getFloat("imageYPosition",0);
            animationlayer2.setX(x+avatar.getWidth());
            animationlayer2.setY(y+avatar.getHeight()/4);
            avatar.setBackgroundResource(R.drawable.right);
            animationlayer2.setVisibility(View.VISIBLE);
            animatefireball1 = animatefireball1.ofFloat(animationlayer2, "X", animationlayer2.getX(),animationlayer2.getX()+5*animationlayer2.getWidth());
            animatefireball1.setDuration(1000);
            animatefireball1.start();
            animatefireball1.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }
                @Override
                public void onAnimationEnd(Animator animator) {
                    editor.putString("fireballstopatone", "0");
                    editor.apply();
                    animationlayer2.setVisibility(View.INVISIBLE);
                }
                @Override
                public void onAnimationCancel(Animator animator) {
                }
                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });

            animatefireball1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Float fireball1XPosition = (Float)animation.getAnimatedValue();
                    //damaged zone of fireball1
                    Float x2 = sp.getFloat("imageXPosition",0);
                    Float y2 = sp.getFloat("imageYPosition",0);
                    //from 'start of length (start)' to 'end of length (end)'
                    Float damagedstartx2 = fireball1XPosition;
                    Float damagedendx2 = fireball1XPosition + animationlayer2.getWidth();
                    //from 'bottom of height (start)' to 'top of height (end)'
                    Float damagedstarty2 = y2 + avatar.getHeight();
                    Float damagedendy2 = y2 + avatar.getHeight() - animationlayer2.getHeight();
                    //npc1 zone
                    Float npc1YPosition = sp.getFloat("npc1YPosition",0);
                    Float npc1midx = npc1.getX() + npc1.getWidth()/2;
                    Float npc1midy = npc1YPosition + npc1.getHeight()/2;
                    String fireballstopatone = sp.getString("fireballstopatone","0");
                    String npc1beinghit = sp.getString("npc1beinghit","0");
                    //check for npc1 damaged
                    if (npc1midx >= damagedstartx2 && npc1midx <= damagedendx2
                            && npc1midy <= damagedstarty2 && npc1midy >= damagedendy2)
                    {
                        animationlayer2.setVisibility(View.INVISIBLE);
                        npc1beinghit();
                    }
                    //
                }
            });
        }
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