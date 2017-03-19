package com.github.foolish314159.kameimageviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by Tom on 19.03.2017.
 */

public class ImageLoader {

    /**
     * Notify callback when image data is ready
     */
    public interface ImageLoaderListener {
        void imageAvailable(String forPath, Bitmap image);

        void imageThumbnailAvailable(String forPath, Bitmap image);

        void gifAvailable(String forPath, GifDrawable gif);

        void loadFailed(String forPath);
    }

    public class LoadJob {
        private final String path;

        public LoadJob(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public boolean isGif() {
            return path.toLowerCase().endsWith(".gif");
        }
    }

    public static final String[] SupportedImageFormats = {".bmp", ".gif", ".jpg", ".jpeg", ".png", ".webp"};

    private ImageLoaderListener callback;

    private int thumbnailWidth;
    private int thumbnailHeight;

    private Thread loadingThread;
    private LinkedBlockingDeque<LoadJob> loadQueue;

    /**
     * Create a new image loader, has to be started calling {@link #start()}
     *
     * @param thumbnailWidth  Thumbnail width, if 0 no thumbnail will be loaded
     * @param thumbnailHeight Thumbnail height, if 0 no thumbnail will be loaded
     * @param callback        Callback to be notified when image data has been loaded
     */
    public ImageLoader(int thumbnailWidth, int thumbnailHeight, ImageLoaderListener callback) {
        this.thumbnailWidth = thumbnailWidth;
        this.thumbnailHeight = thumbnailHeight;
        this.callback = callback;

        loadQueue = new LinkedBlockingDeque<>();
    }

    // Public interface

    public void start() {
        cancelled = false;
        loadingThread = new Thread(new LoadingTask());
        loadingThread.start();
    }

    public void stop() {
        cancelled = true;
        loadingThread.interrupt();
        loadingThread = null;
    }

    public synchronized void loadImage(String path) {
        loadQueue.add(new LoadJob(path));
    }

    public synchronized void cancelJob(String path) {
        for (LoadJob job : loadQueue) {
            if (job.getPath().equals(path)) {
                loadQueue.remove(job);
                return;
            }
        }
    }

    public synchronized void clearJobs() {
        loadQueue.clear();
    }

    // Image loading

    private boolean cancelled;

    private class LoadingTask implements Runnable {

        @Override
        public void run() {
            while (!cancelled) {
                try {
                    // Wait if no jobs available
                    LoadJob currentJob = loadQueue.take();

                    // Gif
                    if (currentJob.isGif()) {
                        try {
                            GifDrawable gif = new GifDrawable(currentJob.getPath());
                            if (callback != null) {
                                callback.gifAvailable(currentJob.getPath(), gif);
                            }
                        } catch (IOException e) {
                            if (callback != null) {
                                callback.loadFailed(currentJob.getPath());
                            }
                        }
                    }
                    // Normal image
                    else {
                        // Load thumbnail first
                        if (thumbnailHeight > 0 && thumbnailWidth > 0) {
                            Bitmap image = decodeSampledBitmapFromFile(currentJob.getPath(), thumbnailWidth, thumbnailHeight);
                            if (callback != null) {
                                callback.imageThumbnailAvailable(currentJob.getPath(), image);
                            }
                        }

                        // Finally load full size
                        Bitmap image = decodeBitmapFromFile(currentJob.getPath());
                        if (callback != null) {
                            callback.imageAvailable(currentJob.getPath(), image);
                        }
                    }
                    // Load thumbnail first
                } catch (InterruptedException e) {
                }
            }
        }

        // Image decoding as described in android developer guide

        private int calculateInSampleSize(
                BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }

        private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        }

        private Bitmap decodeBitmapFromFile(String path) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Maximum size is 4096x4096
            double aspect = options.outWidth / (double) options.outHeight;
            if (options.outHeight > options.outWidth) {
                if (options.outHeight > 4096) {
                    options.inSampleSize = calculateInSampleSize(options, (int) (aspect * 2048), 2048);
                }
            } else {
                if (options.outWidth > 4096) {
                    options.inSampleSize = calculateInSampleSize(options, 2048, (int) (2048 / aspect));
                }
            }


            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        }

    }

}
