package com.github.foolish314159.kameimageviewer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import pl.droidsonroids.gif.GifDrawable;


public class ViewImageActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, ImageLoader.ImageLoaderListener {

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private ViewPager mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
//            ActionBar actionBar = getSupportActionBar();
//            if (actionBar != null) {
//                actionBar.show();
//            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private GestureDetectorCompat mTapGestureDetector;

    private class TapGestureListener extends android.view.GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            toggle();
            return false;
        }
    }

    private ImagePagerAdapter pagerAdapter;

    private static final int REQUEST_READ_PERMISSION_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view_image);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = (ViewPager) findViewById(R.id.activity_view_image_content);
        mContentView.setOffscreenPageLimit(1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pagerAdapter = new ImagePagerAdapter(getSupportFragmentManager(), this, Environment.getExternalStorageDirectory());
                mContentView.setAdapter(pagerAdapter);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_PERMISSION_CODE);
            }
        } else {
            pagerAdapter = new ImagePagerAdapter(getSupportFragmentManager(), this, Environment.getExternalStorageDirectory());
            mContentView.setAdapter(pagerAdapter);
        }

        // Set up the user interaction to manually show or hide the system UI.
        mTapGestureDetector = new GestureDetectorCompat(this, new TapGestureListener());
        mContentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mTapGestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pagerAdapter = new ImagePagerAdapter(getSupportFragmentManager(), this, Environment.getExternalStorageDirectory());
                mContentView.setAdapter(pagerAdapter);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.hide();
//        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);

        delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void imageAvailable(String forPath, final Bitmap image) {
        Fragment currentFragment = pagerAdapter.getFragment(forPath);

        if (currentFragment instanceof ImageFragment) {
            final ImageFragment imageFragment = (ImageFragment) currentFragment;
            if (forPath.equals(imageFragment.getPath())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageFragment.setBitmap(image);
                    }
                });
            }
        }
    }

    @Override
    public void imageThumbnailAvailable(String forPath, final Bitmap image) {
        Fragment currentFragment = pagerAdapter.getFragment(forPath);

        if (currentFragment instanceof ImageFragment) {
            final ImageFragment imageFragment = (ImageFragment) currentFragment;
            if (forPath.equals(imageFragment.getPath())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageFragment.setBitmap(image);
                    }
                });
            }
        }
    }

    @Override
    public void gifAvailable(String forPath, final GifDrawable gif) {
        Fragment currentFragment = pagerAdapter.getFragment(forPath);

        if (currentFragment instanceof GifFragment) {
            final GifFragment imageFragment = (GifFragment) currentFragment;
            if (forPath.equals(imageFragment.getPath())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageFragment.setGifDrawable(gif);
                    }
                });
            }
        }
    }

    @Override
    public void loadFailed(String forPath) {
        Toast.makeText(this, "Could not load file " + forPath, Toast.LENGTH_SHORT).show();
    }

}
