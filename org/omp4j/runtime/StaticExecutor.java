package org.omp4j.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.*;

/**
 * Thread-safe, non-blocking, lock-free thread pool executor.
 */
public class StaticExecutor extends AbstractExecutor implements IOMPExecutor {

	/** List of threads. */
	final private ArrayList<Thread> threads;

	/** List of task queues for each thread. */
	final private ArrayList<BlockingQueue<Runnable>> queues;

	/** List of locks for each thread. */
	final private Lock lock = new ReentrantLock();
	final private Condition condvar = lock.newCondition();
	
	/** True if waitForExecution was called, False otherwise. */
	private AtomicBoolean isTerminating = new AtomicBoolean(false);
	private AtomicInteger remaining = new AtomicInteger(0);
	private AtomicInteger nextQueueIndex = new AtomicInteger(0);

	public StaticExecutor(int numThreads) {
		super(numThreads);

		// init task queues
		this.queues = new ArrayList<BlockingQueue<Runnable>>(numThreads);
		for (int i = 0; i < numThreads; i++) {
			this.queues.add(new LinkedBlockingQueue<Runnable>());
		}

		// init threads
		this.threads = new ArrayList<Thread>(numThreads);
		for (int i = 0; i < numThreads; i++) {
			this.threads.add(new StaticExecutorThread(i));
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

		int addIdx = nextQueueIndex.getAndIncrement();
		int threadIdx = addIdx % numThreads;

		queues.get(threadIdx).add(task);
	}


	/** Threadpool worker class */
	private class StaticExecutorThread extends Thread {

		/** ID of this particular thread. */
		private final int threadID;

		/**
		 * Worker constructor.
		 * @param threadID ID of this particular thread.
		 * @throws IllegalArgumentException if threadID isn't positive integer.
		*/
		public StaticExecutorThread(int threadID) {
			super();
	
			if (numThreads <= 0) {
				throw new IllegalArgumentException("Number of threads must be positive integer.");
			}
	
			this.threadID = threadID;
		}

		@Override
		public void run() {

			BlockingQueue<Runnable> queue = queues.get(threadID);

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
