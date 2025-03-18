package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat {
    static BoatGrader bg;
    
    // Shared state variables
    static int adultsOnOahu;
    static int childrenOnOahu;
    static int adultsOnMolokai;
    static int childrenOnMolokai;
    static boolean boatOnOahu;
    static boolean simulationDone;
    static int totalPeople;
    
    // Synchronization variables
    static Lock lock;
    static Condition waitForFinish;
    static Condition waitToAct;
    
    public static void selfTest() {
        BoatGrader b = new BoatGrader();
        // System.out.println("\n ***Testing Boats with only 2 children***");
        // begin(0, 2, b);
        // System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        // begin(1, 2, b);
        System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        begin(5, 5, b);
    }
    
    public static void begin(int adults, int children, BoatGrader b) {
        // Store the grader
        bg = b;
        
        // Initialize shared state
        adultsOnOahu = adults;
        childrenOnOahu = children;
        adultsOnMolokai = 0;
        childrenOnMolokai = 0;
        boatOnOahu = true;
        simulationDone = false;
        totalPeople = adults + children;
        
        System.out.println("DEBUG: Starting simulation with " + adults + " adults and " + children + " children");
        
        // Initialize synchronization
        lock = new Lock();
        waitForFinish = new Condition(lock);
        waitToAct = new Condition(lock);
        
        // Create threads for each child
        for (int i = 0; i < children; i++) {
            KThread t = new KThread(new Runnable() {
                public void run() {
                    ChildItinerary();
                }
            });
            t.setName("Child " + i);
            t.fork();
        }
        
        // Create threads for each adult
        for (int i = 0; i < adults; i++) {
            KThread t = new KThread(new Runnable() {
                public void run() {
                    AdultItinerary();
                }
            });
            t.setName("Adult " + i);
            t.fork();
        }
        
        // Wait for simulation to complete
        lock.acquire();
        while (!simulationDone) {
            System.out.println("DEBUG: Main thread waiting for simulation to complete");
            waitForFinish.sleep();
        }
        System.out.println("DEBUG: Simulation complete, all people (" + totalPeople + ") are now on Molokai");
        lock.release();
    }
    
    static void AdultItinerary() {
        boolean done = false;
        
        while (!done) {
            lock.acquire();
            
            // Check if simulation is done
            if (simulationDone) {
                lock.release();
                return;
            }
            
            // An adult can only go when the boat is on Oahu, they're on Oahu,
            // and there's exactly one child left on Oahu
            if (boatOnOahu && childrenOnOahu == 1 && adultsOnOahu > 0) {
                // Adult crosses to Molokai
                System.out.println("DEBUG: Adult rowing to Molokai");
                
                // Update state
                adultsOnOahu--;
                adultsOnMolokai++;
                boatOnOahu = false;
                
                // Row to Molokai
                bg.AdultRowToMolokai();
                
                System.out.println("DEBUG: After adult crosses: Adults on Oahu = " + adultsOnOahu + 
                                  ", Children on Oahu = " + childrenOnOahu + 
                                  ", Adults on Molokai = " + adultsOnMolokai +
                                  ", Children on Molokai = " + childrenOnMolokai);
                
                // Check if simulation is complete
                if (adultsOnOahu == 0 && childrenOnOahu == 0) {
                    simulationDone = true;
                    waitForFinish.wake();
                }
                
                // Wake up waiting threads
                waitToAct.wakeAll();
                
                // Adult is done
                done = true;
            } else {
                // Adult must wait
                waitToAct.sleep();
            }
            
            lock.release();
            // Yield to allow other threads to run
            KThread.yield();
        }
    }
    
    static void ChildItinerary() {
        while (true) {
            lock.acquire();
            
            // Check if simulation is complete
            if (simulationDone) {
                lock.release();
                return;
            }
            
            // Check if everyone is now on Molokai
            if (adultsOnOahu == 0 && childrenOnOahu == 0) {
                System.out.println("DEBUG: Everyone is on Molokai!");
                simulationDone = true;
                waitForFinish.wake();
                waitToAct.wakeAll();
                lock.release();
                return;
            }
            
            // CASE 1: Boat on Oahu
            if (boatOnOahu) {
                // Case 1a: Two or more children on Oahu - send two across
                if (childrenOnOahu >= 2) {
                    System.out.println("DEBUG: Two children crossing to Molokai");
                    
                    // Update state
                    childrenOnOahu -= 2;
                    childrenOnMolokai += 2;
                    boatOnOahu = false;
                    
                    // First child rows, second rides
                    bg.ChildRowToMolokai();
                    bg.ChildRideToMolokai();
                    
                    System.out.println("DEBUG: After children cross: Adults on Oahu = " + adultsOnOahu + 
                                      ", Children on Oahu = " + childrenOnOahu + 
                                      ", Adults on Molokai = " + adultsOnMolokai +
                                      ", Children on Molokai = " + childrenOnMolokai);
                    
                    // Check if everyone is now on Molokai
                    if (adultsOnOahu == 0 && childrenOnOahu == 0) {
                        simulationDone = true;
                        waitForFinish.wake();
                        waitToAct.wakeAll();
                        lock.release();
                        return;
                    }
                    
                    waitToAct.wakeAll();
                }
                // Case 1b: Last child crosses alone (only if no adults remain)
                else if (childrenOnOahu == 1 && adultsOnOahu == 0) {
                    System.out.println("DEBUG: Last child crossing to Molokai");
                    
                    // Update state
                    childrenOnOahu--;
                    childrenOnMolokai++;
                    boatOnOahu = false;
                    
                    // Row to Molokai
                    bg.ChildRowToMolokai();
                    
                    System.out.println("DEBUG: After last child crosses: Adults on Oahu = " + adultsOnOahu + 
                                      ", Children on Oahu = " + childrenOnOahu + 
                                      ", Adults on Molokai = " + adultsOnMolokai +
                                      ", Children on Molokai = " + childrenOnMolokai);
                    
                    // Everyone is now on Molokai
                    simulationDone = true;
                    waitForFinish.wake();
                    waitToAct.wakeAll();
                    lock.release();
                    return;
                }
                // Case 1c: One child needs to wait for another child to return,
                // or for an adult to go (if conditions are right)
                else {
                    waitToAct.sleep();
                }
            }
            // CASE 2: Boat on Molokai
            else {
                // Only send a child back if there are still people on Oahu
                if ((adultsOnOahu > 0 || childrenOnOahu > 0) && childrenOnMolokai > 0) {
                    System.out.println("DEBUG: Child returning to Oahu from Molokai");
                    
                    // Update state
                    childrenOnOahu++;
                    childrenOnMolokai--;
                    boatOnOahu = true;
                    
                    // Row to Oahu
                    bg.ChildRowToOahu();
                    
                    System.out.println("DEBUG: After child returns: Adults on Oahu = " + adultsOnOahu + 
                                      ", Children on Oahu = " + childrenOnOahu + 
                                      ", Adults on Molokai = " + adultsOnMolokai +
                                      ", Children on Molokai = " + childrenOnMolokai);
                    
                    waitToAct.wakeAll();
                }
                else {
                    waitToAct.sleep();
                }
            }
            
            lock.release();
            // Yield to allow other threads to run
            KThread.yield();
        }
    }
    
    static void SampleItinerary() {
        // Not used in this implementation
    }
}
