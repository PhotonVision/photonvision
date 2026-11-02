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

import io.avaje.jsonb.Jsonb;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.configuration.ConfigProvider;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.StaticFrames;
import org.photonvision.vision.opencv.CVMat;
import org.wpilib.driverstation.MatchType;
import org.wpilib.networktables.IntegerEntry;
import org.wpilib.networktables.IntegerSubscriber;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.StringSubscriber;

public class FileSaveFrameConsumer implements Consumer<CVMat> {
    private final Logger logger = new Logger(FileSaveFrameConsumer.class, LogGroup.General);

    // Formatters to generate unique, timestamped file names
    private static final String FILE_PATH =
            ConfigProvider.getInstance().getImageSavePath().toString();
    static final String FILE_EXTENSION = ".jpg";
    static final String METADATA_EXTENSION = ".json";
    private static final String NT_SUFFIX = "SaveImgCmd";

    static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter tf = DateTimeFormatter.ofPattern("HHmmssSSS");

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

        NetworkTable fmsTable =
                NetworkTablesManager.getInstance().getNTInst().getTable("DriverStation");
        this.ntEventName = fmsTable.getStringTopic("EventName").subscribe("UNKNOWN");
        this.ntMatchNum = fmsTable.getIntegerTopic("MatchNumber").subscribe(0);
        this.ntMatchType = fmsTable.getIntegerTopic("MatchType").subscribe(0);

        updateCameraNickname(camNickname);
    }

    public void accept(CVMat image) {
        accept(image, new Date(), null);
    }

    /** metadataSupplier is only called when a snapshot is saved. */
    public void accept(CVMat image, Supplier<Map<String, Object>> metadataSupplier) {
        accept(image, new Date(), metadataSupplier);
    }

    public void accept(CVMat image, Date now) {
        accept(image, now, null);
    }

    public void accept(CVMat image, Date now, Supplier<Map<String, Object>> metadataSupplier) {
        long currentCount = saveFrameEntry.get();

        // Await save request
        if (currentCount == -1) return;

        // The requested count is greater than the actual count
        if (savedImagesCount < currentCount) {
            String matchData = getMatchData();
            var localNow = LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault());

            String fileName =
                    cameraNickname
                            + "_"
                            + streamType
                            + "_"
                            + df.format(localNow)
                            + "T"
                            + tf.format(localNow)
                            + "_"
                            + matchData;

            // Check if the Unique Camera directory exists and create it if it doesn't
            var cameraDir = new File(FILE_PATH, this.cameraUniqueName);
            if (!cameraDir.exists()) {
                cameraDir.mkdir();
            }
            var saveFilePath = cameraDir.toPath().resolve(fileName + FILE_EXTENSION);

            logger.info("Saving image to: " + saveFilePath);
            boolean saved;
            if (image == null || image.getMat() == null || image.getMat().empty()) {
                saved = Imgcodecs.imwrite(saveFilePath.toString(), StaticFrames.LOST_MAT);
            } else {
                saved = Imgcodecs.imwrite(saveFilePath.toString(), image.getMat());
            }

            savedImagesCount++;
            if (!saved) {
                logger.error("Failed to save image to " + saveFilePath);
                return;
            }
            logger.info("Saved new image at " + saveFilePath);

            if (metadataSupplier != null) {
                saveMetadata(
                        cameraDir.toPath().resolve(fileName + METADATA_EXTENSION),
                        fileName + FILE_EXTENSION,
                        now,
                        matchData,
                        metadataSupplier);
            }
        } else if (savedImagesCount > currentCount) {
            // Reset local value with NT value in case of de-sync
            savedImagesCount = currentCount;
        }
    }

    private void saveMetadata(
            Path path,
            String imageFileName,
            Date now,
            String matchData,
            Supplier<Map<String, Object>> metadataSupplier) {
        try {
            var data = new LinkedHashMap<String, Object>();
            data.put("imageFile", imageFileName);
            data.put("cameraNickname", cameraNickname);
            data.put("cameraUniqueName", cameraUniqueName);
            data.put("streamType", streamType);
            data.put("timestamp", now.toInstant().toString());
            data.put("matchData", matchData);
            data.putAll(metadataSupplier.get());

            Files.writeString(path, Jsonb.instance().type(Object.class).toJson(data));
            logger.info("Saved snapshot metadata at " + path);
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to save snapshot metadata to " + path, e);
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

        MatchType wpiMatchType = MatchType.NONE; // Default is to be unknown
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
