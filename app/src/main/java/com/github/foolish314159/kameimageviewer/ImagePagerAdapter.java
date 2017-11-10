package com.github.foolish314159.kameimageviewer;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Tom on 10.09.2016.
 */
public class ImagePagerAdapter extends FragmentStatePagerAdapter {

    public interface ImageListener {
        void indexChanged(int index, int count);
    }

    public static final String[] SupportedImageFormats = {".bmp", ".gif", ".jpg", ".jpeg", ".png", ".webp"};
    public static final String ARGUMENT_PATH = "ImagePagerAdapterArgumentPath";

    private List<File> files;
    private ImageListener listener = null;

    private SparseArray<WeakReference<Fragment>> fragments = new SparseArray<>();

    public ImagePagerAdapter(final Activity activity, FragmentManager fm, final File directory) {
        super(fm);

        if (activity instanceof ImageListener) {
            listener = (ImageListener) activity;
        }

        files = new ArrayList<>();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final List<File> tmp = listFilesRecursive(directory, new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        String extension = "";
                        if (pathname.getName().contains(".")) {
                            extension = pathname.getName().substring(pathname.getName().lastIndexOf(".")).toLowerCase();
                        }
                        return pathname.isFile() && Arrays.asList(SupportedImageFormats).contains(extension);
                    }
                });

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        files = tmp;
                        notifyDataSetChanged();

                        if (listener != null) {
                            listener.indexChanged(1, files.size());
                        }
                    }
                });

            }
        });
        t.start();
    }

    public List<File> listFilesRecursive(File dir, FileFilter filter) {
        List<File> result = new ArrayList<>();

        if (dir == null) {
            return result;
        }

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                result.addAll(listFilesRecursive(f, filter));
            } else {
                if (filter.accept(f)) {
                    result.add(f);
                }
            }
        }

        return result;
    }

    public int nextFolderIndex(int current, boolean reverse) {
        if (files == null || files.size() == 0 || current >= files.size() || current < 1) {
            return 0;
        }

        int step = reverse ? -1 : 1;
        int startIndex = reverse ? current - 1 : current + 1;

        String currentFolder = files.get(current).getParent();
        for (int i = startIndex; i < files.size(); i += step) {
            String folder = files.get(i).getParent();
            if (!currentFolder.equals(folder)) {
                return i;
            }
        }

        return startIndex;
    }

    public int randomIndex() {
        if (files == null || files.size() == 0) {
            return 0;
        }

        Random rand = new Random();
        return rand.nextInt(files.size());
    }

    @Override
    public Fragment getItem(int position) {
        File currentFile = files.get(position);

        Fragment fragment = null;
        if (currentFile.getName().toLowerCase().endsWith(".gif")) {
            fragment = new GifFragment();
        } else {
            fragment = new ImageFragment();
        }

        Bundle args = new Bundle();
        args.putString(ARGUMENT_PATH, currentFile.getAbsolutePath());
        fragment.setArguments(args);

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

    @Override
    public int getCount() {
        return files.size();
    }
}
