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

package org.photonvision.vision.pipe;

/**
 * Defines a pipe. A pipe is a single step in a pipeline. This class is to be extended, never used
 * on its own.
 *
 * @param <I> Input type for the pipe
 * @param <O> Output type for the pipe
 * @param <P> Parameters type for the pipe
 */
public abstract class CVPipe<I, O, P> {
    protected CVPipeResult<O> result = new CVPipeResult<>();
    protected P params;

    public void setParams(P params) {
        this.params = params;
    }

    /**
     * Runs the process for the pipe.
     *
     * @param in Input for pipe processing.
     * @return Result of processing.
     */
    protected abstract O process(I in);

    /**
     * @param in Input for pipe processing.
     * @return Result of processing.
     */
    public CVPipeResult<O> run(I in) {
        long pipeStartNanos = System.nanoTime();
        result.output = process(in);
        result.nanosElapsed = System.nanoTime() - pipeStartNanos;
        return result;
    }

    public static class CVPipeResult<O> {
        public O output;
        public long nanosElapsed;
    }
}
