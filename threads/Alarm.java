package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;
import java.util.HashMap;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    // HashMap to store threads and their wake times
    private HashMap<KThread, Long> waitMap;
    // Priority queue to store wake times
    private PriorityQueue<Long> waitQueue;
    
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     * 
     * <p>
     * <b>Note</b>: Nachos will not function correctly with more than one alarm.
     */
    public Alarm() {
        waitMap = new HashMap<KThread, Long>();
        waitQueue = new PriorityQueue<Long>();
        
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread that
     * should be run.
     * 
     * Also wakes up any threads that have waited long enough.
     */
    public void timerInterrupt() {
        // Interrupts are already disabled when this method is called
        
        long currentTime = Machine.timer().getTime();
        
        // Check if any threads need to be woken up
        while (!waitQueue.isEmpty() && waitQueue.peek() <= currentTime) {
            long wakeTime = waitQueue.poll();
            
            // Find all threads that need to wake up at this time
            for (KThread thread : waitMap.keySet()) {
                if (waitMap.get(thread) != null && waitMap.get(thread) == wakeTime) {
                    thread.ready();
                    waitMap.put(thread, null); // Mark as processed
                }
            }
        }
        
        // Clean up the map by removing processed entries
        HashMap<KThread, Long> newMap = new HashMap<KThread, Long>();
        for (KThread thread : waitMap.keySet()) {
            if (waitMap.get(thread) != null) {
                newMap.put(thread, waitMap.get(thread));
            }
        }
        waitMap = newMap;
        
        // Yield the current thread to allow other threads to run
        KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
     * in the timer interrupt handler. The thread must be woken up (placed in
     * the scheduler ready set) during the first timer interrupt where
     * 
     * <p>
     * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
     * 
     * @param x the minimum number of clock ticks to wait.
     * 
     * @see nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        // If wait time is zero or negative, return immediately
        if (x <= 0)
            return;
            
        Machine.interrupt().disable();
        
        // Calculate the absolute wake-up time
        long wakeTime = Machine.timer().getTime() + x;
        
        // Get current thread
        KThread currentThread = KThread.currentThread();
        
        // Add to data structures
        waitMap.put(currentThread, wakeTime);
        waitQueue.add(wakeTime);
        
        // Put current thread to sleep
        currentThread.sleep();
        
        Machine.interrupt().enable();
    }
    public static void alarmTest1() {
        int durations[] = {1000, 10*1000, 100*1000};
        long t0, t1;
        
        for (int d : durations) {
            t0 = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(d);
            t1 = Machine.timer().getTime();
            System.out.println("alarmTest1: waited for " + (t1 - t0) + " ticks");
        }
    }
    public static void alarmTest2() {
        System.out.println("=== Testing zero/negative wait times ===");
        long t0, t1;
        
        // Test with zero wait time
        t0 = Machine.timer().getTime();
        ThreadedKernel.alarm.waitUntil(0);
        t1 = Machine.timer().getTime();
        System.out.println("Zero wait: waited for " + (t1 - t0) + " ticks");
        
        // Test with negative wait time
        t0 = Machine.timer().getTime();
        ThreadedKernel.alarm.waitUntil(-100);
        t1 = Machine.timer().getTime();
        System.out.println("Negative wait: waited for " + (t1 - t0) + " ticks");
    }
    public static void alarmTest3() {
        System.out.println("=== Testing multiple threads with different wait times ===");
        
        // Create threads with different wait times and track their wake order
        final int[] wakeOrder = new int[3];
        final Lock orderLock = new Lock();
        
        KThread t1 = new KThread(new Runnable() {
            public void run() {
                ThreadedKernel.alarm.waitUntil(2000);
                orderLock.acquire();
                wakeOrder[0] = 1;
                orderLock.release();
                System.out.println("Thread with 2000 tick wait woke up");
            }
        });
        
        KThread t2 = new KThread(new Runnable() {
            public void run() {
                ThreadedKernel.alarm.waitUntil(1000);
                orderLock.acquire();
                wakeOrder[1] = 1;
                orderLock.release();
                System.out.println("Thread with 1000 tick wait woke up");
            }
        });
        
        KThread t3 = new KThread(new Runnable() {
            public void run() {
                ThreadedKernel.alarm.waitUntil(3000);
                orderLock.acquire();
                wakeOrder[2] = 1;
                orderLock.release();
                System.out.println("Thread with 3000 tick wait woke up");
            }
        });
        
        t1.setName("thread-2000");
        t2.setName("thread-1000");
        t3.setName("thread-3000");
        
        t1.fork();
        t2.fork();
        t3.fork();
        
        // Wait for all threads to complete
        t1.join();
        t2.join();
        t3.join();
    }
    public static void selfTest() {
        alarmTest1();
        alarmTest2();
        alarmTest3();
    }
}
