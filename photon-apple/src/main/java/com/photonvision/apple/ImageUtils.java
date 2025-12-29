package com.photonvision.apple;

/** Utility class for image pixel format constants and conversions */
public final class ImageUtils {
    private ImageUtils() {}

    // Pixel format constants
    public static final int PIXEL_FORMAT_BGR = 0;
    public static final int PIXEL_FORMAT_RGB = 1;
    public static final int PIXEL_FORMAT_BGRA = 2;
    public static final int PIXEL_FORMAT_RGBA = 3;
    public static final int PIXEL_FORMAT_GRAY = 4;

    /**
     * Get pixel format from number of channels
     *
     * @param channels Number of channels (1, 3, or 4)
     * @return Pixel format constant
     */
    public static int getPixelFormatFromChannels(int channels) {
        switch (channels) {
            case 1:
                return PIXEL_FORMAT_GRAY;
            case 3:
                return PIXEL_FORMAT_BGR;
            case 4:
                return PIXEL_FORMAT_BGRA;
            default:
                throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }
    }

    /**
     * Convert pixel format to string
     *
     * @param pixelFormat Pixel format constant
     * @return String representation
     */
    public static String pixelFormatToString(int pixelFormat) {
        switch (pixelFormat) {
            case PIXEL_FORMAT_BGR:
                return "BGR";
            case PIXEL_FORMAT_RGB:
                return "RGB";
            case PIXEL_FORMAT_BGRA:
                return "BGRA";
            case PIXEL_FORMAT_RGBA:
                return "RGBA";
            case PIXEL_FORMAT_GRAY:
                return "GRAY";
            default:
                return "UNKNOWN";
        }
    }
}
