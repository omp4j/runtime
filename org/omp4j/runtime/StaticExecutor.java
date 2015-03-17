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
	final private ArrayList<Queue<Runnable>> queues;

	/** List of locks for each thread. */
	final private ArrayList<Lock> locks;

	/** List of conditional variables for each thread. */
	final private ArrayList<Condition> condvars;
	
	/** Id of next task queue. */
	private AtomicInteger nextQueueIndex = new AtomicInteger(0);

	/** True if waitForExecution was called, False otherwise. */
	private AtomicBoolean isTerminating = new AtomicBoolean(false);

	public StaticExecutor(int numThreads) {
		super(numThreads);

		// init task queues
		this.queues = new ArrayList<>(numThreads);
		for (int i = 0; i < numThreads; i++) {
			this.queues.add(new ConcurrentLinkedQueue<>());
		}

		// init locks and condvars
		this.locks = new ArrayList<>(numThreads);
		this.condvars = new ArrayList<>(numThreads);
		for (int i = 0; i < numThreads; i++) {
			Lock l = new ReentrantLock();
			this.locks.add(l);
			this.condvars.add(l.newCondition());
		}

		// init threads
		this.threads = new ArrayList<>(numThreads);
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
		isTerminating.set(true);
		try {
			for (int i = 0; i < numThreads; i++) {
				final Lock lock = locks.get(i);
				final Condition condvar = condvars.get(i);

				lock.lock();
				condvar.signal();
				lock.unlock();
			}
			for (int i = 0; i < numThreads; i++) {
				threads.get(i).join();
			}
		} catch (InterruptedException e) {
			// TODO
		}
	}

	@Override
	public void execute(Runnable task) {
		if (isTerminating.get()) {
			throw new IllegalStateException("Terminating poll.");
		}

		int addIdx = nextQueueIndex.getAndIncrement();
		int threadIdx = addIdx % 4;

		queues.get(threadIdx).add(task);

		final Lock lock = locks.get(threadIdx);
		final Condition condvar = condvars.get(threadIdx);

		lock.lock();
		condvar.signal();
		lock.unlock();
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

		/**
		 * Walk through the queue of threads given and execute them.
		 * @param q the queue to be executed
		*/
		private void runAll(Queue<Runnable> q) {
			while (!q.isEmpty()) {
				Runnable task = q.poll();
				task.run();
			}
		}

		@Override
		public void run() {
			Queue<Runnable> threadQueue = queues.get(threadID);
			boolean lastRun = false;
			final Lock lock = locks.get(threadID);
			final Condition condvar = condvars.get(threadID);

			while (true) {

				// first run
				if (isTerminating.get()) {
					runAll(threadQueue);
					return;
				}

				// spurious wakeups and termination
				lock.lock();
				try {
					while (threadQueue.isEmpty()) {
						condvar.await();
						if (isTerminating.get()) {
							runAll(threadQueue);
							return;
						}
					}
				} catch (InterruptedException e) {
					return;
				} finally {
					lock.unlock();
				}

				runAll(threadQueue);
			}
		}
	}
}
