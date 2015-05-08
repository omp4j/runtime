package org.omp4j.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.*;

/**
 * Non-blocking, lock-free thread pool executor.
 */
public class StaticExecutor extends AbstractExecutor implements IOMPExecutor {

	/** List of threads. */
	final private ArrayList<Thread> threads;

	/** List of task queues for each thread. */
	final private ArrayList<Queue<Runnable>> queues;

	private int nextQueueIndex = 0;

	public StaticExecutor(int numThreads) {
		super(numThreads);

		// init task queues
		this.queues = new ArrayList<Queue<Runnable>>(numThreads);
		for (int i = 0; i < numThreads; i++) {
			this.queues.add(new LinkedList<Runnable>());
		}

		// init threads
		this.threads = new ArrayList<Thread>(numThreads);
		for (int i = 0; i < numThreads; i++) {
			final Queue<Runnable> q = queues.get(i);

			this.threads.add(new Thread(new Runnable(){
				@Override
				public void run() {
					while (!q.isEmpty()) {
						q.poll().run();
					}
				}
			}));
		}
	}

	@Override
	synchronized public void waitForExecution() {
		// start threads
		for (int i = 0; i < numThreads; i++) {
			threads.get(i).start();
		}

		try {
			for (int i = 0; i < numThreads; i++) {
				threads.get(i).join();
			}
		} catch (InterruptedException e) {
			System.err.println("An InterruptedException occurred while waiting fot StaticExecutor termination. This is unexpected behavior probably caused by thread manipulation. Please do not access threads created by the executors.");
			System.exit(1);
		}
	}

	@Override
	public void execute(Runnable task) {
		queues.get(nextQueueIndex++ % 4).add(task);
	}
}
