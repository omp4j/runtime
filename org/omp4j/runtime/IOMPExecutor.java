package org.omp4j.runtime;

import java.util.concurrent.Executor;

/**
 * Extension to the Executor class. Provides methods for omp4j preprocessor.
 */
public interface IOMPExecutor extends Executor {

	/** Block current thread until all tasks are finished. */
	public void waitForExecution();

	/** Get unique id of the thread from which the method is called. The range is [0, threadNum) */
	public int getThreadNum();

	/** Return total number of thread used. This number is usually the same as the one given to the constructor. */
	public int getNumThreads();

	/**
	 * Simulate barrier hit. Delay this thread until all threads have hit the barrier.
	 * @param barrierName name of the barrier hit.
	 */
	public void hitBarrier(String barrierName);
}
