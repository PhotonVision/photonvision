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

package org.photonvision.vision.processes;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.pipeline.CVPipeline;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

/** VisionRunner has a frame supplier, a pipeline supplier, and a result consumer */
@SuppressWarnings("rawtypes")
public class VisionRunner {

    private final Logger logger;
    private final Thread visionProcessThread;
    private final Supplier<Frame> frameSupplier;
    private final Supplier<CVPipeline> pipelineSupplier;
    private final Consumer<CVPipelineResult> pipelineResultConsumer;
    private final QuirkyCamera cameraQuirks;

    private long loopCount;

    /**
    * VisionRunner contains a thread to run a pipeline, given a frame, and will give the result to
    * the consumer.
    *
    * @param frameSupplier The supplier of the latest frame.
    * @param pipelineSupplier The supplier of the current pipeline.
    * @param pipelineResultConsumer The consumer of the latest result.
    */
    public VisionRunner(
            FrameProvider frameSupplier,
            Supplier<CVPipeline> pipelineSupplier,
            Consumer<CVPipelineResult> pipelineResultConsumer,
            QuirkyCamera cameraQuirks) {
        this.frameSupplier = frameSupplier;
        this.pipelineSupplier = pipelineSupplier;
        this.pipelineResultConsumer = pipelineResultConsumer;
        this.cameraQuirks = cameraQuirks;

        visionProcessThread = new Thread(this::update);
        visionProcessThread.setName("VisionRunner - " + frameSupplier.getName());
        logger = new Logger(VisionRunner.class, frameSupplier.getName(), LogGroup.VisionModule);
    }

    public void startProcess() {
        visionProcessThread.start();
    }

    private void update() {
        while (!Thread.interrupted()) {
            var pipeline = pipelineSupplier.get();
            var frame = frameSupplier.get();

            try {
                var pipelineResult = pipeline.run(frame, cameraQuirks);
                pipelineResultConsumer.accept(pipelineResult);
            } catch (Exception ex) {
                logger.error("Exception on loop " + loopCount);
                ex.printStackTrace();
            }
            loopCount++;
        }
    }
}
