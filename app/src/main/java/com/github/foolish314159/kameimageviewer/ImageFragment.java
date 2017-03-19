package com.github.foolish314159.kameimageviewer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.chrisbanes.photoview.PhotoView;

public class ImageFragment extends Fragment {

    private PhotoView photoView;
    private String path;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        path = getArguments().getString(ImagePagerAdapter.ARGUMENT_PATH);

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_image, container, false);

        photoView = (PhotoView) rootView.findViewById(R.id.fragment_image_photoView);

        return rootView;
    }

    public String getPath() {
        return path;
    }

    public void setBitmap(Bitmap bitmap) {
        if (photoView != null) {
            photoView.setImageBitmap(bitmap);
        }
    }

}
