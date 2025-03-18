package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		this.waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        boolean intStatus = Machine.interrupt().disable();
        
        waitQueue.waitForAccess(KThread.currentThread());
        
        conditionLock.release();
        KThread.sleep();
        
        Machine.interrupt().restore(intStatus);
        
        conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        boolean intStatus = Machine.interrupt().disable();
        
        KThread thread = waitQueue.nextThread();
        if (thread != null) {
            thread.ready();
        }
        
        Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        boolean intStatus = Machine.interrupt().disable();
        
        KThread thread = waitQueue.nextThread();
        while (thread != null) {
            thread.ready();
            thread = waitQueue.nextThread();
        }
        
        Machine.interrupt().restore(intStatus);
	}

        /**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
    public void sleepFor(long timeout) { // Not needed according to TA
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	}

    private Lock conditionLock;
	private ThreadQueue waitQueue;
}
