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
import org.photonvision.vision.opencv.CVMat;

public class FileSaveFrameConsumer implements Consumer<CVMat> {
    // Formatters to generate unique, timestamped file names
    private static String FILE_PATH = ConfigManager.getInstance().getImageSavePath().toString();
    private static String FILE_EXTENSION = ".jpg";
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat tf = new SimpleDateFormat("hhmmssSS");
    private final String NT_SUFFIX = "SaveImgCmd";
    private final String ntEntryName;
    private NetworkTable subTable;
    private final NetworkTable rootTable;
    private final Logger logger;
    private long imgSaveCountInternal = 0;
    private String camNickname;
    private String fnamePrefix;
    private IntegerEntry entry;

    public FileSaveFrameConsumer(String camNickname, String streamPrefix) {
        this.fnamePrefix = camNickname + "_" + streamPrefix;
        this.ntEntryName = streamPrefix + NT_SUFFIX;
        this.rootTable = NetworkTablesManager.getInstance().kRootTable;
        updateCameraNickname(camNickname);
        this.logger = new Logger(FileSaveFrameConsumer.class, this.camNickname, LogGroup.General);
    }

    public void accept(CVMat image) {
        if (image != null && image.getMat() != null && !image.getMat().empty()) {
            var curCommand = entry.get(); // default to just our current count
            if (curCommand >= 0) {
                // Only do something if we got a valid current command
                if (imgSaveCountInternal < curCommand) {
                    // Save one frame.
                    // Create the filename
                    Date now = new Date();
                    String savefile =
                            FILE_PATH
                                    + File.separator
                                    + fnamePrefix
                                    + "_"
                                    + df.format(now)
                                    + "T"
                                    + tf.format(now)
                                    + FILE_EXTENSION;

                    // write to file
                    Imgcodecs.imwrite(savefile, image.getMat());

                    // Count one more image saved
                    imgSaveCountInternal++;
                    logger.info("Saved new image at " + savefile);

                } else if (imgSaveCountInternal > curCommand) {
                    imgSaveCountInternal = curCommand;
                }
            }
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
        this.camNickname = newCameraNickname;
        this.subTable = rootTable.getSubTable(this.camNickname);
        this.subTable.getEntry(ntEntryName).setInteger(imgSaveCountInternal);
        this.entry = subTable.getIntegerTopic(ntEntryName).getEntry(-1); // Default negative
    }
}
