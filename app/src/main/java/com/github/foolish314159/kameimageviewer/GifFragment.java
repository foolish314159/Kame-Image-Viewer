package com.github.foolish314159.kameimageviewer;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import pl.droidsonroids.gif.GifImageView;

/**
 * Created by Tom on 19.03.2017.
 */

public class GifFragment extends Fragment {

    private GifImageView gifImageView;
    private String path;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        path = getArguments().getString(ImagePagerAdapter.ARGUMENT_PATH);

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_gif, container, false);

        gifImageView = (GifImageView) rootView.findViewById(R.id.fragment_gif_gifView);
        gifImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity();
                if (activity instanceof ViewImageActivity) {
                    ((ViewImageActivity) activity).performContentClick();
                }
            }
        });

        gifImageView.setImageURI(Uri.fromFile(new File(path)));

        return rootView;
    }

}
