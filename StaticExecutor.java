package org.omp4j.runtime;

import java.util.concurrent.*;

// TODO: implement static executor
public class StaticExecutor extends DynamicExecutor implements IOMPExecutor {
	public StaticExecutor(int numThreads) {
		super(numThreads);
	}
}
