/*
 * Copyright (C) 2020 Photon Vision.
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

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.Frame;

public class MJPGFrameFileSaveConsumer {

    private static String FILE_PATH = ConfigManager.getInstance().getImageSavePath().toString();
    private static String FILE_EXTENSION = ".jpg";
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat tf = new SimpleDateFormat("hhmmssSS");

    private final String ntEntryName = "saveImgCmd";

    private final Logger logger;

    private final NetworkTable table;

    private boolean prevCommand = false;

    private String sourceName;

    public MJPGFrameFileSaveConsumer(String sourceName) {
        this.sourceName = sourceName;
        
        this.table = NetworkTableInstance.getDefault().getTable("/CameraFrameSaver").getSubTable(this.sourceName);

        this.logger = new Logger(MJPGFrameFileSaveConsumer.class, this.sourceName, LogGroup.General);

        this.table.getEntry(ntEntryName).setBoolean(false);

    }

    public void accept(Frame frame) {
        if (frame != null && !frame.image.getMat().empty()) {

            boolean curCommand = table.getEntry(ntEntryName).getBoolean(false);

            if (curCommand == true && prevCommand == false) {

                Date now = new Date();
                String savefile = FILE_PATH + File.separator +
                                  this.sourceName + "_" + 
                                  df.format(now) + "T" + 
                                  tf.format(now) + 
                                  FILE_EXTENSION;

                Imgcodecs.imwrite(savefile.toString(),frame.image.getMat());

                logger.info("Saved Image " + savefile);
            }

            prevCommand = curCommand;
        }
    }

}
