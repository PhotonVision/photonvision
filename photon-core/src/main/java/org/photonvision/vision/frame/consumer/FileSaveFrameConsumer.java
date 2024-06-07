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
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.StaticFrames;
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

    public FileSaveFrameConsumer(String camNickname, String cameraUniqueName, String streamPrefix) {
        this.ntEntryName = streamPrefix + NT_SUFFIX;
        this.cameraNickname = camNickname;
        this.cameraUniqueName = cameraUniqueName;
        this.streamType = streamPrefix;

        this.rootTable = NetworkTablesManager.getInstance().kRootTable;
        updateCameraNickname(camNickname);
    }

    public void accept(CVMat image) {
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

            if (image == null || image.getMat() == null || image.getMat().empty()) {
                Imgcodecs.imwrite(saveFilePath, StaticFrames.LOST_MAT);
            } else {
                Imgcodecs.imwrite(saveFilePath, image.getMat());
            }

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
