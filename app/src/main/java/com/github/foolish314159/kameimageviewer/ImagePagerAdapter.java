package com.github.foolish314159.kameimageviewer;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Tom on 10.09.2016.
 */
public class ImagePagerAdapter extends PagerAdapter {

    private Context mContext;

    public ImagePagerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = new View(mContext);
        switch (position) {
            case 0:
                view.setBackgroundColor(Color.RED);
                break;
            case 1:
                view.setBackgroundColor(Color.GREEN);
                break;
            default:
                view.setBackgroundColor(Color.BLUE);
                break;
        }

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
