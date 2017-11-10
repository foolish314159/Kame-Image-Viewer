package com.github.foolish314159.kameimageviewer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.fresco.FrescoImageLoader
import kotlinx.android.synthetic.main.activity_view_image.*
import java.io.File


class ViewImageActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, ImagePagerAdapter.ImageListener, ViewPager.OnPageChangeListener {

    private val mHideHandler = Handler()
    private var mContentView: ViewPager? = null
    private var mBackgroundView: View? = null
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        mContentView!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private var mControlsView: View? = null
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        //            ActionBar actionBar = getSupportActionBar();
        //            if (actionBar != null) {
        //                actionBar.show();
        //            }
        mControlsView!!.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    private var mTapGestureDetector: GestureDetectorCompat? = null

    private inner class TapGestureListener : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            toggle()
            return false
        }
    }

    fun performContentClick() {
        toggle()
    }

    private var pagerAdapter: ImagePagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_image)

        BigImageViewer.initialize(FrescoImageLoader.with(applicationContext))

        mVisible = true
        mBackgroundView = findViewById(R.id.backgroundView)
        mControlsView = findViewById(R.id.fullscreen_content_controls)
        mContentView = findViewById(R.id.activity_view_image_content)
        mContentView!!.offscreenPageLimit = 1
        mContentView!!.addOnPageChangeListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pagerAdapter = ImagePagerAdapter(this, supportFragmentManager, null)
                mContentView?.adapter = pagerAdapter
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_PERMISSION_CODE)
            }
        } else {
            pagerAdapter = ImagePagerAdapter(this, supportFragmentManager, null)
            mContentView?.adapter = pagerAdapter
        }

        // Set up the user interaction to manually show or hide the system UI.
        mTapGestureDetector = GestureDetectorCompat(this, TapGestureListener())
        mContentView?.setOnTouchListener { view, motionEvent ->
            mTapGestureDetector!!.onTouchEvent(motionEvent)
            false
        }
        mBackgroundView?.setOnTouchListener { view, motionEvent ->
            mTapGestureDetector!!.onTouchEvent(motionEvent)
            false
        }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        buttonOpenFolder.setOnTouchListener(mDelayHideTouchListener)
        buttonOpenFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 4321)
        }

        buttonFolderPrevious.setOnTouchListener(mDelayHideTouchListener)
        buttonFolderPrevious.setOnClickListener {
            goToNextPage(true)
        }

        buttonFolderNext.setOnTouchListener(mDelayHideTouchListener)
        buttonFolderNext.setOnClickListener {
            goToNextPage(false)
        }

        buttonRandomImage.setOnTouchListener(mDelayHideTouchListener)
        buttonRandomImage.setOnClickListener {
            selectRandomPage()
        }

        buttonSlideshow.setOnTouchListener(mDelayHideTouchListener)
        buttonSlideshow.setOnClickListener {
            slideshow()
        }
    }

    private fun goToNextPage(reverse: Boolean) {
        mContentView?.currentItem?.let { current ->
            pagerAdapter?.nextFolderIndex(current, reverse)?.let {
                mContentView?.setCurrentItem(it, false)
            }
        }
    }

    private fun selectRandomPage() {
        pagerAdapter?.randomIndex()?.let {
            this.runOnUiThread {
                mContentView?.setCurrentItem(it, false)
            }
        }
    }

    private val SlideShowInterval = 3000L
    private var isSlideshowRunning = false
    private var slideShowThread: Thread? = null

    private val SlideShowRunnable: Runnable = Runnable {
        while (isSlideshowRunning) {
            selectRandomPage()

            try {
                Thread.sleep(SlideShowInterval)
            } catch (ie: InterruptedException) {
            }
        }
    }

    private fun slideshow() {
        if (isSlideshowRunning) {
            isSlideshowRunning = false

            slideShowThread?.interrupt()
            slideShowThread = null
        } else {
            isSlideshowRunning = true

            slideShowThread = Thread(SlideShowRunnable)
            slideShowThread?.start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 4321 && resultCode == Activity.RESULT_OK) {
            data?.data?.path?.let {
                // TODO: proper directory picker, will crash for other sources
                val dir = File(Environment.getExternalStorageDirectory().absolutePath + "/" + it.replace("tree/primary:", ""))
                pagerAdapter = ImagePagerAdapter(this, supportFragmentManager, dir)
                mContentView!!.adapter = pagerAdapter
            }
        }
    }

    private var pageCount = 0

    override fun indexChanged(index: Int, count: Int) {
        pageCount = count
        textViewImageCount.text = getString(R.string.textView_image_count, "$index", "$count")
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        val index = position + 1
        textViewImageCount.text = getString(R.string.textView_image_count, "$index", "$pageCount")

        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_READ_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pagerAdapter = ImagePagerAdapter(this, supportFragmentManager, null)
                mContentView!!.adapter = pagerAdapter
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(1000)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        //        ActionBar actionBar = getSupportActionBar();
        //        if (actionBar != null) {
        //            actionBar.hide();
        //        }
        mControlsView!!.visibility = View.GONE
        textViewImageCount.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    @SuppressLint("InlinedApi")
    private fun show() {
        // Show the system bar
        mContentView!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        textViewImageCount.visibility = View.VISIBLE
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())

        delayedHide(AUTO_HIDE_DELAY_MILLIS)
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {

        /**
         * Whether or not the system UI should be auto-hidden after
         * [.AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [.AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300

        private val REQUEST_READ_PERMISSION_CODE = 123
    }

}
