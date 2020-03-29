package com.chameleonvision.common.vision.pipeline;

import java.util.function.Function;

/**
* Defines a pipe. A pipe is a single step in a pipeline. This class is to be extended, never used
* on its own.
*
* @param <I> Input type for the pipe
* @param <O> Output type for the pipe
* @param <P> Parameters type for the pipe
*/
public abstract class CVPipe<I, O, P> implements Function<I, PipeResult<O>> {

	protected PipeResult<O> result = new PipeResult<>();
	protected P params;

	public void setParams(P params) {
		this.params = params;
	}

	/**
	* Runs the process for the pipe.
	*
	* @param in Input for pipe processing
	* @return Result of processing
	*/
	protected abstract O process(I in);

	/**
	* @param in Input for pipe processing
	* @return Result of processing
	*/
	@Override
	public PipeResult<O> apply(I in) {
		long pipeStartNanos = System.nanoTime();
		result.result = process(in);
		result.nanosElapsed = System.nanoTime() - pipeStartNanos;
		return result;
	}
}
