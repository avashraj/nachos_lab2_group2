package nachos.threads;

import nachos.machine.*;

// public class Communicator {
//     private Lock lock;
//     private Condition2 speakerCondition;
//     private Condition2 listenerCondition;
//     private int message;
//     private boolean messageReady;

//     public Communicator() {
//         lock = new Lock();
//         speakerCondition = new Condition2(lock);
//         listenerCondition = new Condition2(lock);
//         messageReady = false;
//     }

//     public void speak(int word) {
//         lock.acquire();
        
//         while (messageReady) {
//             speakerCondition.sleep(); // Wait until the previous message is consumed
//         }

//         message = word;
//         messageReady = true;
//         listenerCondition.wake(); // Notify listener
//         speakerCondition.sleep(); // Wait for listener to acknowledge

//         lock.release();
//     }

//     public int listen() {
//         lock.acquire();
        
//         while (!messageReady) {
//             listenerCondition.sleep(); // Wait for a message
//         }

//         int receivedMessage = message;
//         messageReady = false;
//         speakerCondition.wake(); // Notify speaker that message is received

//         lock.release();
//         return receivedMessage;
//     }
// }



// package nachos.threads;

/**
 * A Communicator allows threads to synchronously exchange messages.
 * A speaker thread calls speak(int word) and waits until a listener receives the message.
 * A listener thread calls listen() and waits until a speaker provides a message.
 *
 * <p>Key points:
 * <ul>
 *   <li>Only one lock is used.
 *   <li>Two condition variables are used:
 *         one (speakerQueue) for waiting speakers and one (listenerQueue) for waiting listeners.
 *   <li>A speaker waits until there is at least one listener waiting and no unclaimed message.
 *   <li>A listener waits until a speaker has delivered a message.
 * </ul>
 */
public class Communicator {
    // The single lock that guards all shared state.
    private Lock lock;
    // Condition variable on which speakers wait.
    private Condition speakerQueue;
    // Condition variable on which listeners wait.
    private Condition listenerQueue;
    // Counters for waiting speakers and listeners.
    private int waitingSpeakers;
    private int waitingListeners;
    // The message to be transferred.
    private int word;
    // Indicates if a message is available (delivered but not yet received).
    private boolean messageReady;

    /**
     * Initialize the Communicator.
     */
    public Communicator() {
        lock = new Lock();
        speakerQueue = new Condition(lock);
        listenerQueue = new Condition(lock);
        waitingSpeakers = 0;
        waitingListeners = 0;
        messageReady = false;
    }

    /**
     * A speaker calls this method to deliver a message.
     * The calling thread will wait until a listener receives the message.
     *
     * @param word the message to speak.
     */
    public void speak(int word) {
        lock.acquire();
        waitingSpeakers++;
        // Wait until there is at least one listener waiting and no previous message is pending.
        while (waitingListeners == 0 || messageReady) {
            speakerQueue.sleep();
        }
        // Now it is safe to deliver the message.
        this.word = word;
        messageReady = true;
        // Signal one waiting listener that a message is available.
        listenerQueue.wake();
        // Wait until the listener has taken the message.
        while (messageReady) {
            speakerQueue.sleep();
        }
        waitingSpeakers--;
        // Wake up any additional speakers that might be waiting now that a listener has been paired.
        speakerQueue.wake();
        lock.release();
    }

    /**
     * A listener calls this method to receive a message.
     * The calling thread will wait until a speaker provides a message.
     *
     * @return the message received.
     */
    public int listen() {
        lock.acquire();
        waitingListeners++;
        // Wake a waiting speaker in case one is available.
        speakerQueue.wake();
        // Wait until a message is available.
        while (!messageReady) {
            listenerQueue.sleep();
        }
        // Retrieve the message.
        int result = word;
        // Mark the message as consumed.
        messageReady = false;
        waitingListeners--;
        // Wake the speaker that is waiting for acknowledgement.
        speakerQueue.wake();
        lock.release();
        return result;
    }
/**
 * Self test for the Communicator class.
 * Tests basic functionality of the implementation.
 */
/**
 * Self test for the Communicator class.
 * Tests basic functionality of the implementation.
 */
public static void selfTest() {
    System.out.println("Communicator.selfTest() starting...");
    
    testBasicCommunication();
    testSpeakerFirst();
    testListenerFirst();
    testMultipleCommunication();
    testPingPong();
    
    System.out.println("Communicator.selfTest() completed successfully!");
}

/**
 * Tests basic communication between one speaker and one listener.
 */
private static void testBasicCommunication() {
    System.out.println("Testing basic communication...");
    
    final Communicator com = new Communicator();
    final int testWord = 42;
    final int[] received = new int[1];
    
    // Listener thread
    KThread listener = new KThread(new Runnable() {
        public void run() {
            System.out.println("Listener waiting...");
            received[0] = com.listen();
            System.out.println("Listener received: " + received[0]);
        }
    }).setName("Listener");
    
    // Speaker thread
    KThread speaker = new KThread(new Runnable() {
        public void run() {
            System.out.println("Speaker sending: " + testWord);
            com.speak(testWord);
            System.out.println("Speaker done");
        }
    }).setName("Speaker");
    
    listener.fork();
    speaker.fork();
    
    speaker.join();
    listener.join();
    
    if (received[0] != testWord) {
        System.out.println("ERROR: Incorrect word received: expected " + testWord + ", got " + received[0]);
        Machine.halt();
    }
    
    System.out.println("Basic communication test passed!");
}

/**
 * Tests communication when the speaker starts before the listener.
 */
private static void testSpeakerFirst() {
    System.out.println("Testing speaker first scenario...");
    
    final Communicator com = new Communicator();
    final int testWord = 100;
    final int[] received = new int[1];
    
    // Speaker thread that starts first
    KThread speaker = new KThread(new Runnable() {
        public void run() {
            System.out.println("Speaker-First sending: " + testWord);
            com.speak(testWord);
            System.out.println("Speaker-First done");
        }
    }).setName("Speaker-First");
    
    speaker.fork();
    
    // Give speaker time to start and wait
    ThreadedKernel.alarm.waitUntil(500);
    
    // Listener thread that starts after
    KThread listener = new KThread(new Runnable() {
        public void run() {
            System.out.println("Listener-Second waiting...");
            received[0] = com.listen();
            System.out.println("Listener-Second received: " + received[0]);
        }
    }).setName("Listener-Second");
    
    listener.fork();
    
    listener.join();
    speaker.join();
    
    if (received[0] != testWord) {
        System.out.println("ERROR: Incorrect word received: expected " + testWord + ", got " + received[0]);
        Machine.halt();
    }
    
    System.out.println("Speaker first test passed!");
}

/**
 * Tests communication when the listener starts before the speaker.
 */
private static void testListenerFirst() {
    System.out.println("Testing listener first scenario...");
    
    final Communicator com = new Communicator();
    final int testWord = 200;
    final int[] received = new int[1];
    
    // Listener thread that starts first
    KThread listener = new KThread(new Runnable() {
        public void run() {
            System.out.println("Listener-First waiting...");
            received[0] = com.listen();
            System.out.println("Listener-First received: " + received[0]);
        }
    }).setName("Listener-First");
    
    listener.fork();
    
    // Give listener time to start and wait
    ThreadedKernel.alarm.waitUntil(500);
    
    // Speaker thread that starts after
    KThread speaker = new KThread(new Runnable() {
        public void run() {
            System.out.println("Speaker-Second sending: " + testWord);
            com.speak(testWord);
            System.out.println("Speaker-Second done");
        }
    }).setName("Speaker-Second");
    
    speaker.fork();
    
    speaker.join();
    listener.join();
    
    if (received[0] != testWord) {
        System.out.println("ERROR: Incorrect word received: expected " + testWord + ", got " + received[0]);
        Machine.halt();
    }
    
    System.out.println("Listener first test passed!");
}

/**
 * Tests multiple speakers and listeners interacting with the same communicator.
 */
private static void testMultipleCommunication() {
    System.out.println("Testing multiple speakers and listeners...");
    
    final Communicator com = new Communicator();
    final int numPairs = 3;
    final KThread[] speakers = new KThread[numPairs];
    final KThread[] listeners = new KThread[numPairs];
    final int[] words = new int[numPairs];
    final int[] received = new int[numPairs];
    
    // Initialize test words
    for (int i = 0; i < numPairs; i++) {
        words[i] = 300 + i;
    }
    
    // Create listener threads
    for (int i = 0; i < numPairs; i++) {
        final int idx = i;
        listeners[i] = new KThread(new Runnable() {
            public void run() {
                System.out.println("Listener-" + idx + " waiting...");
                received[idx] = com.listen();
                System.out.println("Listener-" + idx + " received: " + received[idx]);
            }
        }).setName("Listener-" + i);
    }
    
    // Create speaker threads
    for (int i = 0; i < numPairs; i++) {
        final int idx = i;
        speakers[i] = new KThread(new Runnable() {
            public void run() {
                System.out.println("Speaker-" + idx + " sending: " + words[idx]);
                com.speak(words[idx]);
                System.out.println("Speaker-" + idx + " done");
            }
        }).setName("Speaker-" + i);
    }
    
    // Start all listeners
    for (int i = 0; i < numPairs; i++) {
        listeners[i].fork();
    }
    
    // Give listeners time to start
    KThread.yield();
    
    // Start all speakers
    for (int i = 0; i < numPairs; i++) {
        speakers[i].fork();
    }
    
    // Wait for all threads to finish
    for (int i = 0; i < numPairs; i++) {
        speakers[i].join();
        listeners[i].join();
    }
    
    // Verify that all values were received (in any order)
    boolean[] foundWords = new boolean[numPairs];
    for (int i = 0; i < numPairs; i++) {
        boolean found = false;
        for (int j = 0; j < numPairs; j++) {
            if (received[i] == words[j] && !foundWords[j]) {
                foundWords[j] = true;
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("ERROR: Message not found or duplicate: " + received[i]);
            Machine.halt();
        }
    }
    
    System.out.println("Multiple speakers and listeners test passed!");
}

/**
 * Tests ping-pong communication between two threads.
 */
private static void testPingPong() {
    System.out.println("Testing ping-pong communication...");
    
    final Communicator com1 = new Communicator();
    final Communicator com2 = new Communicator();
    final int iterations = 3;
    final boolean[] success = new boolean[1];
    success[0] = true;
    
    // Thread A
    KThread threadA = new KThread(new Runnable() {
        public void run() {
            for (int i = 0; i < iterations; i++) {
                System.out.println("Thread A sending: " + i);
                com1.speak(i);
                System.out.println("Thread A waiting...");
                int response = com2.listen();
                System.out.println("Thread A received: " + response);
                if (response != i) {
                    success[0] = false;
                    System.out.println("ERROR: Thread A expected " + i + " but got " + response);
                }
            }
        }
    }).setName("ThreadA");
    
    // Thread B
    KThread threadB = new KThread(new Runnable() {
        public void run() {
            for (int i = 0; i < iterations; i++) {
                System.out.println("Thread B waiting...");
                int message = com1.listen();
                System.out.println("Thread B received: " + message);
                if (message != i) {
                    success[0] = false;
                    System.out.println("ERROR: Thread B expected " + i + " but got " + message);
                }
                System.out.println("Thread B sending: " + message);
                com2.speak(message);
            }
        }
    }).setName("ThreadB");
    
    threadB.fork();
    threadA.fork();
    
    threadA.join();
    threadB.join();
    
    if (!success[0]) {
        System.out.println("ERROR: Ping-pong test failed");
        Machine.halt();
    }
    
    System.out.println("Ping-pong test passed!");
}
}
