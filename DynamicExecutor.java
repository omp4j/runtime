package org.omp4j.runtime;

import java.util.concurrent.*;

public class DynamicExecutor implements IOMPExecutor {

	private long numThreads;
	private ExecutorService executor;

	public DynamicExecutor(int numThreads) {
		assert(numThreads > 0);
		this.numThreads = numThreads;
		executor = Executors.newFixedThreadPool(numThreads);
	}

	@Override
	public void waitForExecution() {
		try {
			executor.shutdown();
			executor.awaitTermination(9999999, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO
		}
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
	public void execute(Runnable task) {
		executor.execute(task);
	}
}
