package org.omp4j.runtime;

import java.util.concurrent.*;
import java.util.HashMap;

/**
 * Class implementing common methods for executors.
 */
public abstract class AbstractExecutor implements IOMPExecutor {

	/** Number of threads to be used */
	protected final int numThreads;

	/** Map of barriers */
	protected final ConcurrentHashMap<String, CyclicBarrier> barriers;

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
		this.barriers = new ConcurrentHashMap<String, CyclicBarrier>();
	}

	@Override
	public int getThreadNum() {
		return (int) Thread.currentThread().getId() % numThreads;
	}

	@Override
	public int getNumThreads() {
		return this.numThreads;
	}

	@Override
	public void hitBarrier(String barrierName) {
		
		CyclicBarrier barr = null;

		// if barrier already exists, fetch it
		if (barriers.containsKey(barrierName)) {
			barr = barriers.get(barrierName);
		} else {
			// else block
			synchronized(barriers) {
				// check whether some other thread have created it while this thread was waiting before synchronized block
				if (barriers.containsKey(barrierName)) {
					barr = barriers.get(barrierName);
				// if not, create it by itself
				} else {
					barr = new CyclicBarrier(numThreads);
					barriers.put(barrierName, barr);
				}
			}
		}

		try {
			barr.await();
		} catch (BrokenBarrierException e) {
			System.err.println("An BrokenBarrierException occurred while processing barrier '" + barrierName +"'. This is unexpected behavior probably caused by thread manipulation. Please do not access threads created by the executors.");
			System.exit(1);
		} catch (InterruptedException e) {
			System.err.println("An InterruptedException occurred while processing barrier '" + barrierName +"'. This is unexpected behavior probably caused by thread manipulation. Please do not access threads created by the executors.");
			System.exit(1);
		}
	}
}
