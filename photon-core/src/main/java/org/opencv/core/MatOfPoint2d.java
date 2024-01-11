package org.opencv.core;

import java.util.Arrays;
import java.util.List;

/**
 * Copy of cv::MatofPoint2f, but with doubles
 */
public class MatOfPoint2d extends Mat {
    // 32FC2
    private static final int _depth = CvType.CV_64F;
    private static final int _channels = 2;

    public MatOfPoint2d() {
        super();
    }

    protected MatOfPoint2d(long addr) {
        super(addr);
        if( !empty() && checkVector(_channels, _depth) < 0 )
            throw new IllegalArgumentException("Incompatible Mat");
        //FIXME: do we need release() here?
    }

    public static MatOfPoint2f fromNativeAddr(long addr) {
        return new MatOfPoint2f(addr);
    }

    public MatOfPoint2d(Mat m) {
        super(m, Range.all());
        if( !empty() && checkVector(_channels, _depth) < 0 )
            throw new IllegalArgumentException("Incompatible Mat");
        //FIXME: do we need release() here?
    }

    public MatOfPoint2d(Point...a) {
        super();
        fromArray(a);
    }

    public void alloc(int elemNumber) {
        if(elemNumber>0)
            super.create(elemNumber, 1, CvType.makeType(_depth, _channels));
    }

    public void fromArray(Point...a) {
        if(a==null || a.length==0)
            return;
        int num = a.length;
        alloc(num);
        double buff[] = new double[num * _channels];
        for(int i=0; i<num; i++) {
            Point p = a[i];
            buff[_channels*i+0] = p.x;
            buff[_channels*i+1] = p.y;
        }
        put(0, 0, buff); //TODO: check ret val!
    }

    public Point[] toArray() {
        int num = (int) total();
        Point[] ap = new Point[num];
        if(num == 0)
            return ap;
        double buff[] = new double[num * _channels];
        get(0, 0, buff); //TODO: check ret val!
        for(int i=0; i<num; i++)
            ap[i] = new Point(buff[i*_channels], buff[i*_channels+1]);
        return ap;
    }

    public void fromList(List<Point> lp) {
        Point ap[] = lp.toArray(new Point[0]);
        fromArray(ap);
    }

    public List<Point> toList() {
        Point[] ap = toArray();
        return Arrays.asList(ap);
    }
}
