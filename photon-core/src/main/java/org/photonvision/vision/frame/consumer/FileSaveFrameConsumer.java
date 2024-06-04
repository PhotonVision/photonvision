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

package org.photonvision.vision.frame.consumer;

import edu.wpi.first.networktables.IntegerEntry;
import edu.wpi.first.networktables.NetworkTable;

import java.awt.Color;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.opencv.CVMat;


public class FileSaveFrameConsumer implements Consumer<CVMat> {
    private final Logger logger = new Logger(FileSaveFrameConsumer.class, LogGroup.General);

    // Formatters to generate unique, timestamped file names
    private static final String FILE_PATH = ConfigManager.getInstance().getImageSavePath().toString();
    private static final String FILE_EXTENSION = ".jpg";
    private static final String NT_SUFFIX = "SaveImgCmd";

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat tf = new SimpleDateFormat("hhmmssSS");

    private final NetworkTable rootTable;
    private NetworkTable subTable;
    private final String ntEntryName;
    private IntegerEntry saveFrameEntry;

    private final String cameraUniqueName;
    private String cameraNickname;
    private final String streamType;

    private long savedImagesCount = 0;

    private static final Mat LOST_MAT = new Mat(60, 15 * 7, CvType.CV_8UC3);

    static {
        LOST_MAT.setTo(ColorHelper.colorToScalar(Color.BLACK));
        var col = 0;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 15, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a2a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 15, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a300)),
                -1);
        col += 15;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 15, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a3a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 15, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a200)),
                -1);
        col += 15;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 15, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x440045)),
                -1);
        col += 15;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 15, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x0000a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 15, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(0, 50, LOST_MAT.width(), 10),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                LOST_MAT, new Rect(15, 50, 30, 10), ColorHelper.colorToScalar(Color.WHITE), -1);

        Imgproc.putText(
                LOST_MAT, "Camera", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.white), 2);
        Imgproc.putText(
                LOST_MAT,
                "Lost",
                new Point(14, 45),
                0,
                0.6,
                ColorHelper.colorToScalar(Color.white),
                2);
        Imgproc.putText(
                LOST_MAT, "Camera", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);
        Imgproc.putText(
                LOST_MAT, "Lost", new Point(14, 45), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);
    }


    public FileSaveFrameConsumer(String camNickname, String cameraUniqueName, String streamPrefix) {
        this.ntEntryName = streamPrefix + NT_SUFFIX;
        this.cameraNickname = camNickname;
        this.cameraUniqueName = cameraUniqueName;
        this.streamType = streamPrefix;

        this.rootTable = NetworkTablesManager.getInstance().kRootTable;
        updateCameraNickname(camNickname);
    }

    public void accept(CVMat image) {
        if (image == null || image.getMat() == null || image.getMat().empty()) {
            image.copyFrom(LOST_MAT);
        }
        long currentCount = saveFrameEntry.get();

        // Await save request
        if (currentCount == -1) return;

        // The requested count is greater than the actual count
        if (savedImagesCount < currentCount) {
            Date now = new Date();

            String fileName =
                    cameraNickname + "_" + streamType + "_" + df.format(now) + "T" + tf.format(now);

            // Check if the Unique Camera directory exists and create it if it doesn't
            String cameraPath = FILE_PATH + File.separator + this.cameraUniqueName;
            var cameraDir = new File(cameraPath);
            if (!cameraDir.exists()) {
                cameraDir.mkdir();
            }

            String saveFilePath = cameraPath + File.separator + fileName + FILE_EXTENSION;

            Imgcodecs.imwrite(saveFilePath, image.getMat());

            savedImagesCount++;
            logger.info("Saved new image at " + saveFilePath);
        } else if (savedImagesCount > currentCount) {
            // Reset local value with NT value in case of de-sync
            savedImagesCount = currentCount;
        }
    }

    public void updateCameraNickname(String newCameraNickname) {
        // Remove existing entries
        if (this.subTable != null) {
            if (this.subTable.containsKey(ntEntryName)) {
                this.subTable.getEntry(ntEntryName).close();
            }
        }

        // Recreate and re-init network tables structure
        this.cameraNickname = newCameraNickname;
        this.subTable = rootTable.getSubTable(this.cameraNickname);
        this.subTable.getEntry(ntEntryName).setInteger(savedImagesCount);
        this.saveFrameEntry = subTable.getIntegerTopic(ntEntryName).getEntry(-1); // Default negative
    }

    public void overrideTakeSnapshot() {
        // Simulate NT change
        saveFrameEntry.set(saveFrameEntry.get() + 1);
    }
}
