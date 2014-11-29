package org.omp4j.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.HashMap;

public class DynamicExecutor implements IOMPExecutor {

	protected long numThreads;
	protected ExecutorService executor;
	protected HashMap<String, AtomicLong> barriers;

	public DynamicExecutor(int numThreads) {
		assert(numThreads > 0);
		this.numThreads = numThreads;
		executor = Executors.newFixedThreadPool(numThreads);
		barriers = new HashMap<String, AtomicLong>();
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
