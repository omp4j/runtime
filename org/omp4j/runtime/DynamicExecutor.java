package org.omp4j.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.HashMap;

/**
 * Wrapper class of fixed thread pool executor.
 */
public class DynamicExecutor extends AbstractExecutor implements IOMPExecutor {

	/** The executor */
	private ExecutorService executor;

	public DynamicExecutor(int numThreads) {
		super(numThreads);
		this.executor = Executors.newFixedThreadPool(numThreads);
	}

	@Override
	public void waitForExecution() {
		try {
			executor.shutdown();
			executor.awaitTermination(9999999, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			System.err.println("An InterruptedException occurred while waiting fot DynamicExecutor termination. This is unexpected behavior probably caused by thread manipulation. Please do not access threads created by the executors.");
			System.exit(1);
		}
	}

	@Override
	public void execute(Runnable task) {
		executor.execute(task);
	}
}
