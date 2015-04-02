package org.omp4j.runtime;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.*;

/**
 * Wrapper class of fixed thread pool executor.
 */
public class DynamicExecutor extends AbstractExecutor implements IOMPExecutor {

	final private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	/** List of threads. */
	final private ArrayList<Thread> threads;
	final private Lock lock = new ReentrantLock();
	final private Condition condvar = lock.newCondition();

	/** True if waitForExecution was called, False otherwise. */
	private AtomicInteger remaining = new AtomicInteger(0);

	public DynamicExecutor(int numThreads) {
		super(numThreads);

		// init threads
		this.threads = new ArrayList<Thread>(numThreads);
		for (int i = 0; i < numThreads; i++) {
			this.threads.add(new DynamicExecutorThread(i));
		}

		// start threads
		for (int i = 0; i < numThreads; i++) {
			threads.get(i).start();
		}
	}

	@Override
	synchronized public void waitForExecution() {
		try {
			lock.lock();
			while (remaining.get() > 0) {
				condvar.await();
			}

			lock.unlock();

			for (int i = 0; i < numThreads; i++) {
				threads.get(i).interrupt();
				threads.get(i).join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void execute(Runnable task) {
		remaining.incrementAndGet();
		queue.add(task);
	}


	/** Threadpool worker class */
	private class DynamicExecutorThread extends Thread {

		/** ID of this particular thread. */
		private final int threadID;

		/**
		 * Worker constructor.
		 * @param threadID ID of this particular thread.
		 * @throws IllegalArgumentException if threadID isn't positive integer.
		 */
		public DynamicExecutorThread(int threadID) {
			super();

			if (numThreads <= 0) {
				throw new IllegalArgumentException("Number of threads must be positive integer.");
			}

			this.threadID = threadID;
		}


		@Override
		public void run() {

			try {
				while (true) {
					Runnable r = queue.take();
					r.run();

					if (remaining.decrementAndGet() == 0) {
						try {
							lock.lock();
							condvar.signal();
						} finally {
							lock.unlock();
						}
					}
				}
			} catch (InterruptedException e) {
				return;
			}
		}
	}
}
