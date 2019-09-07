package flip.sim;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

class Timer extends Thread {

	private boolean start = false;
	private boolean finished = false;
	private Callable <?> task = null;
	private Exception error = null;
	private Object result = null;
	private long startTime, endTime;

	public <T> void call_start(Callable <T> task) {
		if (!isAlive()) throw new IllegalStateException();
		if (task == null) throw new IllegalArgumentException();
		this.task = task;
		synchronized (this) {
			start = true;
			this.startTime = System.currentTimeMillis();
			notify();
		}
	}

	public <T> T call_wait(long timeout) throws Exception {
		if (timeout < 0) throw new IllegalArgumentException();
		synchronized (this) {
			if (finished == false)
				try {
					wait(timeout);
				} catch (InterruptedException e) {}
		}
		if (finished == false)
			throw new TimeoutException();
		finished = false;
		if (error != null) throw error;
		@SuppressWarnings("unchecked")
		T result_T = (T) result;
		return result_T;
	}

	public void run() {
		for (;;) {
			synchronized (this) {
				if (start == false)
					try {
						wait();
					} catch (InterruptedException e) {}
			}
			start = false;
			error = null;
			try {
				result = task.call();
			} catch (Exception e) {
				error = e;
			}
			synchronized (this) {
				this.endTime = System.currentTimeMillis();
				finished = true;
				notify();
			}
		}
	}
	
	public long getElapsedTime() {
		return endTime - startTime;
	}
}