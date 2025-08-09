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
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.StringSubscriber;
import edu.wpi.first.wpilibj.DriverStation.MatchType;
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
    static final String FILE_EXTENSION = ".jpg";
    private static final String NT_SUFFIX = "SaveImgCmd";

    static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    static final DateFormat tf = new SimpleDateFormat("hhmmssSS");

    private final NetworkTable rootTable;
    private NetworkTable subTable;
    private final String ntEntryName;
    IntegerEntry saveFrameEntry;

    private StringSubscriber ntEventName;
    private IntegerSubscriber ntMatchNum;
    private IntegerSubscriber ntMatchType;

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

        NetworkTable fmsTable = NetworkTablesManager.getInstance().getNTInst().getTable("FMSInfo");
        this.ntEventName = fmsTable.getStringTopic("EventName").subscribe("UNKNOWN");
        this.ntMatchNum = fmsTable.getIntegerTopic("MatchNumber").subscribe(0);
        this.ntMatchType = fmsTable.getIntegerTopic("MatchType").subscribe(0);

        updateCameraNickname(camNickname);
    }

    public void accept(CVMat image) {
        accept(image, new Date());
    }

    public void accept(CVMat image, Date now) {
        long currentCount = saveFrameEntry.get();

        // Await save request
        if (currentCount == -1) return;

        // The requested count is greater than the actual count
        if (savedImagesCount < currentCount) {
            String matchData = getMatchData();

            String fileName =
                    cameraNickname
                            + "_"
                            + streamType
                            + "_"
                            + df.format(now)
                            + "T"
                            + tf.format(now)
                            + "_"
                            + matchData;

            // Check if the Unique Camera directory exists and create it if it doesn't
            var cameraDir = new File(FILE_PATH, this.cameraUniqueName);
            if (!cameraDir.exists()) {
                cameraDir.mkdir();
            }
            var saveFilePath = cameraDir.toPath().resolve(fileName + FILE_EXTENSION);

            logger.info("Saving image to: " + saveFilePath);
            if (image == null || image.getMat() == null || image.getMat().empty()) {
                Imgcodecs.imwrite(saveFilePath.toString(), StaticFrames.LOST_MAT);
            } else {
                Imgcodecs.imwrite(saveFilePath.toString(), image.getMat());
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

    /**
     * Returns the match Data collected from the NT. eg : Q58 for qualification match 58. If not in
     * event, returns None-0-Unknown
     */
    private String getMatchData() {
        var matchType = ntMatchType.getAtomic();
        if (matchType.timestamp == 0) {
            // no NT info yet
            logger.warn("Did not receive match type, defaulting to None");
        }

        var matchNum = ntMatchNum.getAtomic();
        if (matchNum.timestamp == 0) {
            logger.warn("Did not receive match num, defaulting to 0");
        }

        var eventName = ntEventName.getAtomic();
        if (eventName.timestamp == 0) {
            logger.warn("Did not receive event name, defaulting to 'UNKNOWN'");
        }

        MatchType wpiMatchType = MatchType.None; // Default is to be unknown
        if (matchType.value < 0 || matchType.value >= MatchType.values().length) {
            logger.error("Invalid match type from FMS: " + matchType.value);
        } else {
            wpiMatchType = MatchType.values()[(int) matchType.value];
        }

        return wpiMatchType.name() + "-" + matchNum.value + "-" + eventName.value;
    }

    public void close() {
        saveFrameEntry.close();
        ntEventName.close();
        ntMatchNum.close();
        ntMatchType.close();
    }
}
