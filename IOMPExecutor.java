package org.omp4j.runtime;

import java.util.concurrent.Executor;

public interface IOMPExecutor extends Executor {
	public void waitForExecution();
	public long getThreadNum();
	public long getNumThreads();
	public void hitBarrier(String barrierName);
}
