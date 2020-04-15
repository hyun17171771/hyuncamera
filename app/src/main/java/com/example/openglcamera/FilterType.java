package com.example.openglcamera;

public enum FilterType {
    ORIGINAL("Original", 0),
    RAINBOW("Rainbow", 1),
    GRAYSCALE("Grayscale", 2),
    INVERSION("Inversion", 3),
    CARTOON("Cartoon", 4),
    METAL("Metal", 5);

    private String filterName;
    private int filterIndex;

    FilterType(String filterName, int filterIndex) {
        this.filterName = filterName;
        this.filterIndex = filterIndex;
    }

    public String getFilterName() {
        return filterName;
    }

    public int getFilterIndex() {
        return filterIndex;
    }
}
