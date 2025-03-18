package nachos.threads;

import nachos.threads.Lock;

import nachos.machine.*;
import nachos.threads.Condition2;
import nachos.threads.KThread;
import nachos.threads.ThreadedKernel;

/**
 * Test cases for Condition2 implementation.
 */
public class Condition2Test {
    
    /**
     * Tests the sleep/wake functionality.
     */
    public static void testSleepWake() {
        System.out.println("=== Condition2Test: testSleepWake ===");
        
        final Lock lock = new Lock();
        final Condition2 condition = new Condition2(lock);
        
        // Thread that will sleep until woken up
        KThread sleeper = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                System.out.println("Sleeper: going to sleep");
                condition.sleep();
                System.out.println("Sleeper: woken up!");
                lock.release();
            }
        });
        sleeper.setName("sleeper");
        
        // Thread that will wake up the sleeper
        KThread waker = new KThread(new Runnable() {
            public void run() {
                // Give sleeper a chance to start sleeping
                ThreadedKernel.alarm.waitUntil(1000);
                
                lock.acquire();
                System.out.println("Waker: waking up sleeper");
                condition.wake();
                lock.release();
            }
        });
        waker.setName("waker");
        
        sleeper.fork();
        waker.fork();
        
        // Give enough time for test to complete
        ThreadedKernel.alarm.waitUntil(2000);
        System.out.println("Test complete");
    }
    
    /**
     * Tests the wakeAll functionality.
     */
    public static void testWakeAll() {
        System.out.println("=== Condition2Test: testWakeAll ===");
        
        final Lock lock = new Lock();
        final Condition2 condition = new Condition2(lock);
        final int threadCount = 5;
        
        // Create multiple sleeping threads
        KThread[] sleepers = new KThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            sleepers[i] = new KThread(new Runnable() {
                public void run() {
                    lock.acquire();
                    // Use separate print statements to avoid string concatenation
                    System.out.print("Sleeper ");
                    System.out.print(id);
                    System.out.println(": going to sleep");
                    
                    condition.sleep();
                    
                    System.out.print("Sleeper ");
                    System.out.print(id);
                    System.out.println(": woken up!");
                    lock.release();
                }
            });
            
            // Avoid string concatenation when setting the name
            char[] nameChars = {'s', 'l', 'e', 'e', 'p', 'e', 'r', (char)('0' + i)};
            String threadName = new String(nameChars);
            sleepers[i].setName(threadName);
            
            sleepers[i].fork();
        }
        
        // Give time for all threads to start sleeping
        ThreadedKernel.alarm.waitUntil(1000);
        
        // Wake all threads at once
        KThread wakeAllThread = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                System.out.println("WakeAll: waking up all sleepers");
                condition.wakeAll();
                lock.release();
            }
        });
        wakeAllThread.setName("wakeAll");
        wakeAllThread.fork();
        
        // Give time for test to complete
        ThreadedKernel.alarm.waitUntil(2000);
        System.out.println("Test complete");
    }

    
    /**
     * Entry point for the tests.
     */
    public static void runTests() {
        System.out.println("Starting Condition2 tests");
        testSleepWake();
        testWakeAll();
        System.out.println("All Condition2 tests completed");
    }
}
