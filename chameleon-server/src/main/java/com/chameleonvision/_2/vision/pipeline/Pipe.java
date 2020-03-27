package com.chameleonvision._2.vision.pipeline;

import org.apache.commons.lang3.tuple.Pair;

public interface Pipe<I, O> {
    /**
     *
     * @param input Input object for pipe
     * @return Returns a Pair containing the process time in Nanoseconds,
     *         and the output object
     */
    Pair<O, Long> run(I input);
}
