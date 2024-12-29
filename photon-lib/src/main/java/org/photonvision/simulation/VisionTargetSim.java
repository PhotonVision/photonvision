/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision.simulation;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.List;
import org.photonvision.estimation.TargetModel;

/** Describes a vision target located somewhere on the field that your vision system can detect. */
public class VisionTargetSim {
    private Pose3d pose;
    private TargetModel model;

    public final int fiducialID;

    /**
     * Describes a vision target located somewhere on the field that your vision system can detect.
     *
     * @param pose Pose3d of the tag in field-relative coordinates
     * @param model TargetModel which describes the shape of the target
     */
    public VisionTargetSim(Pose3d pose, TargetModel model) {
        this.pose = pose;
        this.model = model;
        this.fiducialID = -1;
    }

    /**
     * Describes a fiducial tag located somewhere on the field that your vision system can detect.
     *
     * @param pose Pose3d of the tag in field-relative coordinates
     * @param model TargetModel which describes the shape of the target(tag)
     * @param id The ID of this fiducial tag
     */
    public VisionTargetSim(Pose3d pose, TargetModel model, int id) {
        this.pose = pose;
        this.model = model;
        this.fiducialID = id;
    }

    public void setPose(Pose3d pose) {
        this.pose = pose;
    }

    public void setModel(TargetModel model) {
        this.model = model;
    }

    public Pose3d getPose() {
        return pose;
    }

    public TargetModel getModel() {
        return model;
    }

    /** This target's vertices offset from its field pose. */
    public List<Translation3d> getFieldVertices() {
        return model.getFieldVertices(pose);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                && obj instanceof VisionTargetSim o
                && pose.equals(o.pose)
                && model.equals(o.model);
    }
}
