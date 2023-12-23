/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.calibrator;

import java.util.zip.Deflater;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                     ArrayUtils class                                            */
/*                                     ArrayUtils class                                            */
/*                                     ArrayUtils class                                            */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
public class ArrayUtils {
    private static final Logger logger = new Logger(ArrayUtils.class, LogGroup.General);

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     argmax                                                      */
    /*                                     argmax                                                      */
    /*                                     argmax                                                      */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Find index of the maximum value in an array
     *
     * @param array an array
     * @return the argmax (lowest index in case of duplicate values)
     */
    static int argmax(double[] array) {
        int locationOfExtreme = 0;
        double extreme = array[locationOfExtreme];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > extreme) {
                extreme = array[i];
                locationOfExtreme = i;
            }
        }
        return locationOfExtreme;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     argmin                                                      */
    /*                                     argmin                                                      */
    /*                                     argmin                                                      */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Find index of the minimum value in an array
     *
     * @param array an array
     * @return the argmin (lowest index in case of duplicate values)
     */
    static int argmin(double[] array) {
        int locationOfExtreme = 0;
        double extreme = array[locationOfExtreme];

        for (int i = 1; i < array.length; i++) {
            if (array[i] < extreme) {
                extreme = array[i];
                locationOfExtreme = i;
            }
        }
        return locationOfExtreme;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     isAllTrue                                                   */
    /*                                     isAllTrue                                                   */
    /*                                     isAllTrue                                                   */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Test for all values in a boolean array are true
     *
     * @param array
     * @return true if all true or false if any false
     */
    static boolean isAllTrue(boolean[] array) {
        for (boolean element : array) {
            if (!element) return false;
        }
        return true;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                     brief                                                       */
    /*                                     brief                                                       */
    /*                                     brief                                                       */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Partially print an OpenCV Mat
     *
     * <p>Convenience method of brief(Mat, int, int, int, int) with defaults of first 4 rows, last 4
     * rows, first 12 values in a row, last 12 values in a row.
     *
     * <p>Each ".r." or ".c." represents 2000 items or the remainder.
     *
     * @param mat OpenCV Mat to be partially printed
     * @return String of the values in the corners of the Mat if large or entire Mat if small
     */
    static String brief(Mat mat) {
        return brief(mat, 4, 4, 12, 12);
    }

    /**
     * Partially print an OpenCV Mat
     *
     * <p>Each ".r." or ".c." represents 2000 items or the remainder.
     *
     * @param mat OpenCV Mat to be partially printed
     *     <p>
     * @param firstRows - count first few rows
     * @param lastRows - count last few rows
     * @param firstRowData - count first few column/channel values
     * @param lastRowData - count last few column/channel values
     * @return String of the values in the corners of the Mat if large or entire Mat if small
     */
    static String brief(Mat mat, int firstRows, int lastRows, int firstRowData, int lastRowData) {
        final int acknowledgeRowsSkipped = 2000; // skip count to print an icon
        final int acknowledgeRowDataSkipped = 2000; // skip count to print an icon

        StringBuilder sb = new StringBuilder();
        sb.append(mat);
        sb.append("\n");

        double[] matRowD = null;
        float[] matRowF = null;
        byte[] matRowB = null;
        int[] matRowI = null;
        short[] matRowS = null;

        switch (CvType.depth(mat.type())) {
            case CvType.CV_64F: // double
                matRowD = new double[mat.channels() * mat.cols()];
                break;

            case CvType.CV_32F: // float
                matRowF = new float[mat.channels() * mat.cols()];
                break;

            case CvType.CV_8U: // byte
            case CvType.CV_8S:
                matRowB = new byte[mat.channels() * mat.cols()];
                break;

            case CvType.CV_32S: // int
                matRowI = new int[mat.channels() * mat.cols()];
                break;

            case CvType.CV_16U: // short
            case CvType.CV_16S:
                matRowS = new short[mat.channels() * mat.cols()];
                break;

            default:
                logger.error(
                        "Print Mat Error - Unknown OpenCV Mat depth. Not printing requested data. "
                                + CvType.depth(mat.type()));
                return "Print Mat Error";
        }

        int printCountRow = 0;
        int printCountColChan = 0;
        boolean skippedRow = false;

        for (int row = 0; row < mat.rows(); row++) {
            if (row >= firstRows && row < mat.rows() - lastRows) {
                skippedRow = true;
                if (printCountRow % acknowledgeRowsSkipped == 0) {
                    printCountRow = 0;
                    sb.append(".r.");
                }
                printCountRow++;
                continue;
            }
            if (skippedRow) {
                sb.append("\n");
                skippedRow = false;
            }

            switch (CvType.depth(mat.type())) {
                case CvType.CV_64F: // double
                    mat.get(row, 0, matRowD);
                    break;

                case CvType.CV_32F: // float
                    mat.get(row, 0, matRowF);
                    break;

                case CvType.CV_8U: // byte
                case CvType.CV_8S:
                    mat.get(row, 0, matRowB);
                    break;

                case CvType.CV_32S: // int
                    mat.get(row, 0, matRowI);
                    break;

                case CvType.CV_16U: // short
                case CvType.CV_16S:
                    mat.get(row, 0, matRowS);
                    break;

                default:
                    return "ArrayUtils.brief(Mat) should not be here.";
            }
            printCountColChan = 0;
            for (int colChan = 0; colChan < mat.cols() * mat.channels(); colChan++) {
                if (colChan >= firstRowData && colChan < mat.cols() * mat.channels() - lastRowData) {
                    if (printCountColChan % acknowledgeRowDataSkipped == 0) {
                        printCountColChan = 0;
                        sb.append(".c. ");
                    }
                    printCountColChan++;
                    continue;
                }
                switch (CvType.depth(mat.type())) {
                    case CvType.CV_64F: // double
                        sb.append(matRowD[colChan]);
                        break;

                    case CvType.CV_32F: // float
                        sb.append(matRowF[colChan]);
                        break;

                    case CvType.CV_8U: // byte
                    case CvType.CV_8S:
                        sb.append(matRowB[colChan]);
                        break;

                    case CvType.CV_32S: // int
                        sb.append(matRowI[colChan]);
                        break;

                    case CvType.CV_16U: // short
                    case CvType.CV_16S:
                        sb.append(matRowS[colChan]);
                        break;

                    default:
                        return "ArrayUtils.brief(Mat) should not be here.";
                }
                sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                   intToByteArray                                                */
    /*                                   intToByteArray                                                */
    /*                                   intToByteArray                                                */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Convert one 4-byte int variable to the equivalent four element byte array The 4 bytes are
     * located starting at "offset" from the designated output array
     *
     * @param toBeConvertedInt - effectively considered an unsigned int; unless you like the
     *     complemented value of a negative number you should limit this to positive numbers
     * @param dst - array segment for the converted int; array length must be >= offset + 4
     * @param offset - starting element to be changed in the output array (number of changed elements
     *     is always 4 for the int type)
     * @return byte array of the input
     */
    static void intToByteArray(int toBeConvertedInt, byte[] dst, int offset) {
        dst[offset + 3] = (byte) (toBeConvertedInt & 0xff); // least significant byte

        toBeConvertedInt >>= 8;
        dst[offset + 2] = (byte) (toBeConvertedInt & 0xff);

        toBeConvertedInt >>= 8;
        dst[offset + 1] = (byte) (toBeConvertedInt & 0xff);

        toBeConvertedInt >>= 8;
        dst[offset + 0] = (byte) (toBeConvertedInt); // most significant byte
    }

    /**
     * @param bytesToCompress
     * @return
     */
    static byte[] compress(byte[] bytesToCompress) {
        Deflater deflater = new Deflater();
        deflater.setInput(bytesToCompress);
        deflater.finish();

        byte[] bytesCompressed =
                new byte
                        [bytesToCompress
                                .length]; // assumes compressed data no longer than input - not always true but it
        // is for the ChArUcoBoard

        int numberOfBytesAfterCompression = deflater.deflate(bytesCompressed);

        byte[] returnValues = new byte[numberOfBytesAfterCompression];

        System.arraycopy(bytesCompressed, 0, returnValues, 0, numberOfBytesAfterCompression);

        return returnValues;
    }

    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    /*                                                                                                 */
    /*                                   ArrayUtils constructor                                        */
    /*                                   ArrayUtils constructor                                        */
    /*                                   ArrayUtils constructor                                        */
    /*                                                                                                 */
    /*-------------------------------------------------------------------------------------------------*/
    /*-------------------------------------------------------------------------------------------------*/
    private ArrayUtils() {
        throw new UnsupportedOperationException("This is a utility class");
    }
}
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/
/*                                                                                                 */
/*                                   End ArrayUtils class                                          */
/*                                   End ArrayUtils class                                          */
/*                                   End ArrayUtils class                                          */
/*                                                                                                 */
/*-------------------------------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------------------------------*/

/*
CV_8U - 8-bit unsigned integers ( 0..255 )
CV_8S - 8-bit signed integers ( -128..127 )
CV_16U - 16-bit unsigned integers ( 0..65535 )
CV_16S - 16-bit signed integers ( -32768..32767 )
CV_32S - 32-bit signed integers ( -2147483648..2147483647 )
CV_32F - 32-bit floating-point numbers ( -FLT_MAX..FLT_MAX, INF, NAN )
CV_64F - 64-bit floating-point numbers ( -DBL_MAX..DBL_MAX, INF, NAN )
 */
