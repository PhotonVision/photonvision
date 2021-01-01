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

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.vision.frame.Frame;

public class FileSaveFrameConsumer {

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
    private boolean prevCommand = false;
    private String camNickname;
    private String fnamePrefix;
    private final long CMD_RESET_TIME_MS = 500;
    private final NetworkTableEntry entry;
    // Helps prevent race conditions between user set & auto-reset logic
    private ReentrantLock lock;

    public FileSaveFrameConsumer(String camNickname, String streamPrefix) {
        this.lock = new ReentrantLock();
        this.fnamePrefix = camNickname + "_" + streamPrefix;
        this.ntEntryName = streamPrefix + NT_SUFFIX;
        this.rootTable = NetworkTablesManager.getInstance().kRootTable;
        updateCameraNickname(camNickname);
        entry = subTable.getEntry(ntEntryName);
        entry.setBoolean(false);
        this.logger = new Logger(FileSaveFrameConsumer.class, this.camNickname, LogGroup.General);
    }

    public void accept(Frame frame) {
        if (frame != null && !frame.image.getMat().empty()) {

            if (lock.tryLock()) {
                boolean curCommand = entry.getBoolean(false);

                if (curCommand == true && prevCommand == false) {
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

                    Imgcodecs.imwrite(savefile.toString(), frame.image.getMat());

                    // Help the user a bit - set the NT entry back to false after 500ms
                    TimedTaskManager.getInstance().addOneShotTask(() -> resetCommand(), CMD_RESET_TIME_MS);

                    logger.info("Saved new image at " + savefile);
                }

                prevCommand = curCommand;
                lock.unlock();
            }
        }
    }

    private void resetCommand() {
        lock.lock();
        this.subTable.getEntry(ntEntryName).setBoolean(false);
        lock.unlock();
    }

    private void removeEntries() {
        if (this.subTable != null) {
            if (this.subTable.containsKey(ntEntryName)) {
                this.subTable.delete(ntEntryName);
            }
        }
    }

    public void updateCameraNickname(String newCameraNickname) {
        removeEntries();
        this.camNickname = newCameraNickname;
        this.subTable = rootTable.getSubTable(this.camNickname);
        resetCommand();
    }
}
