package com.github.foolish314159.kameimageviewer;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Created by Tom on 10.09.2016.
 */
public class ImagePagerAdapter extends FragmentStatePagerAdapter {

    public static final String ARGUMENT_PATH = "ImagePagerAdapterArgumentPath";

    private File[] files;
    private ImageLoader imageLoader;

    private SparseArray<WeakReference<Fragment>> fragments = new SparseArray<>();

    public ImagePagerAdapter(FragmentManager fm, ImageLoader.ImageLoaderListener listener, File directory) {
        super(fm);

        // TODO read storage
        files = new File(directory + "/").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String extension = "";
                if (pathname.getName().contains(".")) {
                    extension = pathname.getName().substring(pathname.getName().lastIndexOf(".")).toLowerCase();
                }
                return pathname.isFile() && Arrays.asList(ImageLoader.SupportedImageFormats).contains(extension);
            }
        });
        if (files == null) {
            files = new File[]{};
        }

        if (listener instanceof Activity) {
            Activity activity = (Activity) listener;

            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            imageLoader = new ImageLoader(metrics.widthPixels / 5, metrics.heightPixels / 5, listener);
            imageLoader.start();
        } else {
            imageLoader = new ImageLoader(100, 100, listener);
            imageLoader.start();
        }
    }

    @Override
    public Fragment getItem(int position) {
        imageLoader.clearJobs();

        File currentFile = files[position];

        Fragment fragment = null;
        if (currentFile.getName().toLowerCase().endsWith(".gif")) {
            fragment = new GifFragment();
        } else {
            fragment = new ImageFragment();
        }

        Bundle args = new Bundle();
        args.putString(ARGUMENT_PATH, currentFile.getAbsolutePath());
        fragment.setArguments(args);

        imageLoader.loadImage(currentFile.getAbsolutePath());

        return fragment;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        fragments.put(position, new WeakReference<>(fragment));
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        fragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getFragment(String path) {
        for (int i = 0; i <= fragments.size(); i++) {
            Fragment f = fragments.get(fragments.keyAt(i)).get();

            if (f instanceof ImageFragment) {
                ImageFragment imageFragment = (ImageFragment) f;
                if (path.equals(imageFragment.getPath())) {
                    return f;
                }
            }

            if (f instanceof GifFragment) {
                GifFragment gifFragment = (GifFragment) f;
                if (path.equals(gifFragment.getPath())) {
                    return f;
                }
            }
        }

        return null;
    }

    @Override
    public int getCount() {
        return files.length;
    }
}
