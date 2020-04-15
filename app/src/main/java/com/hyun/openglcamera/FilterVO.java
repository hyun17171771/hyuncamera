package com.hyun.openglcamera;

import android.graphics.drawable.Drawable;

public class FilterVO {

    private Drawable sampleImage;
    private FilterType type;

    public FilterVO(FilterType type, Drawable sampleImage) {
        this.type = type;
        this.sampleImage = sampleImage;
    }


    public Drawable getSampleImage() {
        return sampleImage;
    }

    public FilterType getType() {
        return type;
    }


}
