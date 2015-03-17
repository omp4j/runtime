package org.omp4j.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.HashMap;

/**
 * Class implementing common methods for executors.
 */
public abstract class AbstractExecutor implements IOMPExecutor {

	/** Number of threads to be used */
	protected long numThreads;

	/** Map of barriers */
	protected HashMap<String, AtomicLong> barriers;

	/**
	 * Construct new executor.
	 * @param numThreads number of threads to be used.
	 * @throws IllegalArgumentException if numThreads isn't positive integer.
	*/
	public AbstractExecutor(int numThreads) {
		if (numThreads <= 0) {
			throw new IllegalArgumentException("Number of threads must be positive integer.");
		}

		this.numThreads = numThreads;
		this.barriers = new HashMap<String, AtomicLong>();
	}

	@Override
	public long getThreadNum() {
		return Thread.currentThread().getId() % numThreads + 1;
	}

	@Override
	public long getNumThreads() {
		return this.numThreads;
	}

	@Override
	public synchronized void hitBarrier(String barrierName) {
		// TODO: really sycnhronized? atomic?
		AtomicLong counter = null;
		if (barriers.containsKey(barrierName)) {
			counter = barriers.get(barrierName);
		} else {
			counter = new AtomicLong(numThreads);
			barriers.put(barrierName, counter);
		}

		long remaining = counter.decrementAndGet();

		try {
			if (remaining == 0) notifyAll();
			else wait();
		} catch (Exception e) {
			// TODO:
		}
	}

}
