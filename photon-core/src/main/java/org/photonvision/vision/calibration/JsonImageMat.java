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

package org.photonvision.vision.calibration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.vision.opencv.Releasable;

/** JSON-serializable image. Data is stored as base64-encoded PNG data. */
public class JsonImageMat implements Releasable {
    public final int rows;
    public final int cols;
    public final int type;

    // We store image data as a base64-encoded PNG inside a Java string. This lets us serialize it
    // without too much overhead and still use JSON.
    public final String data;

    // Cached matrices to avoid object recreation
    @JsonIgnore private Mat wrappedMat = null;

    public JsonImageMat(Mat mat) {
        this.rows = mat.rows();
        this.cols = mat.cols();
        this.type = mat.type();

        // Convert from Mat -> png byte array -> base64
        var buf = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buf);
        data = Base64.getEncoder().encodeToString(buf.toArray());
        buf.release();
    }

    public JsonImageMat(
            @JsonProperty("rows") int rows,
            @JsonProperty("cols") int cols,
            @JsonProperty("type") int type,
            @JsonProperty("data") String data) {
        this.rows = rows;
        this.cols = cols;
        this.type = type;
        this.data = data;
    }

    @JsonIgnore
    public Mat getAsMat() {
        if (wrappedMat == null) {
            // Convert back from base64 string -> png -> Mat
            var bytes = Base64.getDecoder().decode(data);
            var pngData = new MatOfByte(bytes);
            this.wrappedMat = Imgcodecs.imdecode(pngData, Imgcodecs.IMREAD_COLOR);
        }
        return this.wrappedMat;
    }

    @Override
    public void release() {
        if (wrappedMat != null) wrappedMat.release();
    }

    @Override
    public String toString() {
        return "JsonImageMat [rows="
                + rows
                + ", cols="
                + cols
                + ", type="
                + type
                + ", datalen="
                + data.length()
                + "]";
    }
}
