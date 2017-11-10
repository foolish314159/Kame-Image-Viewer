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
    private List<File> favourites;
    private ImageListener listener = null;
    private boolean showFavourites = false;

    private SparseArray<WeakReference<Fragment>> fragments = new SparseArray<>();

    public ImagePagerAdapter(final Activity activity, FragmentManager fm, final File directory) {
        super(fm);

        if (activity instanceof ImageListener) {
            listener = (ImageListener) activity;
        }

        files = new ArrayList<>();
        favourites = new ArrayList<>();

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

    public boolean getShowFavourites() {
        return showFavourites;
    }

    public void setShowFavourites(boolean showFavourites) {
        this.showFavourites = showFavourites;
    }

    public void addFavourite(int index) {
        favourites.add(files.get(index));
        notifyDataSetChanged();
    }

    public void delFavourite(int index) {
        favourites.remove(index);
        notifyDataSetChanged();
    }

    public int nextFolderIndex(int current, boolean reverse) {
        List<File> folder = showFavourites ? favourites : files;

        if (folder == null || folder.size() == 0 || current > folder.size() || current < 0) {
            return 0;
        }

        String currentFolder = folder.get(current).getParent();
        if (reverse) {
            for (int i = current - 1; i > 0; i--) {
                String folderName = folder.get(i).getParent();
                if (!currentFolder.equals(folderName)) {
                    return i;
                }
            }
        } else {
            for (int i = current + 1; i < folder.size(); i++) {
                String folderName = folder.get(i).getParent();
                if (!currentFolder.equals(folderName)) {
                    return i;
                }
            }
        }

        return current;
    }

    public int randomIndex() {
        List<File> folder = showFavourites ? favourites : files;

        if (folder == null || folder.size() == 0) {
            return 0;
        }

        Random rand = new Random();
        return rand.nextInt(folder.size());
    }

    @Override
    public Fragment getItem(int position) {
        List<File> folder = showFavourites ? favourites : files;

        File currentFile = folder.get(position);

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
        List<File> folder = showFavourites ? favourites : files;
        return folder.size();
    }
}
