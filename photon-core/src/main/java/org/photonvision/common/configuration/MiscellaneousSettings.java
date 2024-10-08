package org.photonvision.common.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MiscellaneousSettings {
    /**
     * If we should ONLY match cameras by path, and NEVER only by base-name. For now default to false
     * to preserve old matching logic.
     *
     * <p>This also disables creating new CameraConfigurations for detected "new" cameras.
     */
    public boolean matchCamerasOnlyByPath = false;

    public MiscellaneousSettings() {}

    @JsonCreator
    public MiscellaneousSettings(
            @JsonProperty("matchCamerasOnlyByPath") boolean matchCamerasOnlyByPath) {
        this.matchCamerasOnlyByPath = matchCamerasOnlyByPath;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(matchCamerasOnlyByPath);
    }
}
