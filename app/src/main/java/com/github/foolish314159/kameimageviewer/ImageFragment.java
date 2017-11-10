package com.github.foolish314159.kameimageviewer;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator;
import com.github.piasy.biv.view.BigImageView;


public class ImageFragment extends Fragment {

    private BigImageView photoView;
    private String path;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_image, container, false);

        photoView = (BigImageView) rootView.findViewById(R.id.fragment_image_photoView);
        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity();
                if (activity instanceof ViewImageActivity) {
                    ((ViewImageActivity) activity).performContentClick();
                }
            }
        });

        path = getArguments().getString(ImagePagerAdapter.ARGUMENT_PATH);
        photoView.setProgressIndicator(new ProgressPieIndicator());
        photoView.showImage(Uri.parse(path));

        return rootView;
    }

}
