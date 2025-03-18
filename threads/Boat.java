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
    
    // Keep track of each thread's state
    static boolean[] childOnOahu;
    static boolean[] adultOnOahu;
    
    // Synchronization variables
    static Lock lock;
    static Condition adultCV;
    static Condition childOahuCV;
    static Condition childMolokaiCV;
    static boolean done;
    
    public static void selfTest() {
        BoatGrader b = new BoatGrader();
        // System.out.println("\n ***Testing Boats with only 2 children***");
        // begin(0, 2, b);
        // System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        // begin(1, 2, b);
        System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        begin(3, 3, b);
    }
    
    public static void begin(int adults, int children, BoatGrader b) {
        bg = b;
        
        // Initialize shared state
        adultsOnOahu = adults;
        childrenOnOahu = children;
        adultsOnMolokai = 0;
        childrenOnMolokai = 0;
        boatOnOahu = true;
        done = false;
        
        // Initialize location arrays
        childOnOahu = new boolean[children];
        adultOnOahu = new boolean[adults];
        
        for (int i = 0; i < children; i++) {
            childOnOahu[i] = true;
        }
        
        for (int i = 0; i < adults; i++) {
            adultOnOahu[i] = true;
        }
        
        System.out.println("DEBUG: Starting simulation with " + adults + " adults and " + children + " children");
        
        // Initialize synchronization
        lock = new Lock();
        adultCV = new Condition(lock);
        childOahuCV = new Condition(lock);
        childMolokaiCV = new Condition(lock);
        
        // Create threads for each child
        for (int i = 0; i < children; i++) {
            final int childId = i;
            Runnable childRunnable = new Runnable() {
                public void run() {
                    ChildItinerary(childId);
                }
            };
            KThread t = new KThread(childRunnable);
            t.setName("Child " + i);
            t.fork();
        }
        
        // Create threads for each adult
        for (int i = 0; i < adults; i++) {
            final int adultId = i;
            Runnable adultRunnable = new Runnable() {
                public void run() {
                    AdultItinerary(adultId);
                }
            };
            KThread t = new KThread(adultRunnable);
            t.setName("Adult " + i);
            t.fork();
        }
    }
    
    static void AdultItinerary(int id) {
        lock.acquire();
        
        // Wait until conditions are right for this adult to cross:
        // 1. Boat must be on Oahu
        // 2. Adult must be on Oahu
        // 3. There should be exactly one child on Oahu (to bring boat back)
        while (!(boatOnOahu && adultOnOahu[id] && childrenOnOahu == 1) || done) {
            System.out.println("DEBUG: Adult " + id + " waiting");
            adultCV.sleep();
            
            // Check if everyone is across already
            if (done) {
                lock.release();
                return;
            }
        }
        
        // Adult crosses to Molokai
        System.out.println("DEBUG: Adult " + id + " rowing to Molokai");
        
        // Update state
        adultsOnOahu--;
        adultsOnMolokai++;
        boatOnOahu = false;
        adultOnOahu[id] = false;
        
        // Row to Molokai
        bg.AdultRowToMolokai();
        
        System.out.println("DEBUG: After adult crosses: Adults on Oahu = " + adultsOnOahu + 
                          ", Children on Oahu = " + childrenOnOahu + 
                          ", Boat on Oahu = " + boatOnOahu);
        
        // Wake up children on Molokai so one can row back
        childMolokaiCV.wakeAll();
        
        lock.release();
    }
    
    static void ChildItinerary(int id) {
        while (true) {
            lock.acquire();
            
            // Check if everyone is on Molokai
            if (adultsOnOahu == 0 && childrenOnOahu == 0) {
                System.out.println("DEBUG: Everyone is on Molokai!");
                done = true;
                adultCV.wakeAll();
                childOahuCV.wakeAll();
                childMolokaiCV.wakeAll();
                lock.release();
                return;
            }
            
            // CASE 1: Child on Oahu with boat
            if (childOnOahu[id] && boatOnOahu) {
                // Two children crossing scenario
                if (childrenOnOahu >= 2) {
                    // Find another child on Oahu to be a passenger
                    int passengerId = -1;
                    for (int i = 0; i < childOnOahu.length; i++) {
                        if (i != id && childOnOahu[i]) {
                            passengerId = i;
                            break;
                        }
                    }
                    
                    if (passengerId != -1) {
                        System.out.println("DEBUG: Child " + id + " rowing to Molokai with Child " + passengerId);
                        
                        // Update state for driver
                        childOnOahu[id] = false;
                        childrenOnOahu--;
                        childrenOnMolokai++;
                        
                        // Update state for passenger
                        childOnOahu[passengerId] = false;
                        childrenOnOahu--;
                        childrenOnMolokai++;
                        
                        // Update boat location
                        boatOnOahu = false;
                        
                        // Perform boat actions
                        bg.ChildRowToMolokai();  // Driver
                        bg.ChildRideToMolokai(); // Passenger
                        
                        System.out.println("DEBUG: After children cross: Adults on Oahu = " + adultsOnOahu + 
                                           ", Children on Oahu = " + childrenOnOahu + 
                                           ", Boat on Oahu = " + boatOnOahu);
                        
                        // Wake up children on Molokai
                        childMolokaiCV.wakeAll();
                        lock.release();
                        continue;
                    }
                }
                // Last child crossing alone
                else if (childrenOnOahu == 1 && adultsOnOahu == 0) {
                    System.out.println("DEBUG: Last child " + id + " crossing to Molokai");
                    
                    // Update state
                    childrenOnOahu--;
                    childrenOnMolokai++;
                    boatOnOahu = false;
                    childOnOahu[id] = false;
                    
                    // Row to Molokai
                    bg.ChildRowToMolokai();
                    
                    System.out.println("DEBUG: After last child crosses: Adults on Oahu = " + adultsOnOahu + 
                                       ", Children on Oahu = " + childrenOnOahu + 
                                       ", Boat on Oahu = " + boatOnOahu);
                    
                    // Wake everyone up in case it's the last crossing
                    childMolokaiCV.wakeAll();
                    adultCV.wakeAll();
                    lock.release();
                    continue;
                }
                // Only one child on Oahu with adults - wake adults to check if one can cross
                else if (childrenOnOahu == 1 && adultsOnOahu > 0) {
                    System.out.println("DEBUG: Single child " + id + " on Oahu, waking adults");
                    adultCV.wakeAll();
                }
            }
            // CASE 2: Child on Molokai with boat
            else if (!childOnOahu[id] && !boatOnOahu) {
                // Check if people are still on Oahu
                if (adultsOnOahu > 0 || childrenOnOahu > 0) {
                    System.out.println("DEBUG: Child " + id + " rowing back to Oahu");
                    
                    // Update state
                    childrenOnOahu++;
                    childrenOnMolokai--;
                    boatOnOahu = true;
                    childOnOahu[id] = true;
                    
                    // Row to Oahu
                    bg.ChildRowToOahu();
                    
                    System.out.println("DEBUG: After child returns: Adults on Oahu = " + adultsOnOahu + 
                                      ", Children on Oahu = " + childrenOnOahu + 
                                      ", Boat on Oahu = " + boatOnOahu);
                    
                    // Wake up everyone on Oahu
                    childOahuCV.wakeAll();
                    adultCV.wakeAll();
                    
                    // Sleep to allow other children on Molokai to act
                    childMolokaiCV.sleep();
                }
                else {
                    // Everyone's on Molokai - we're done!
                    System.out.println("DEBUG: Child " + id + " on Molokai - everyone is here");
                    childMolokaiCV.wakeAll();
                    childOahuCV.wakeAll();
                    adultCV.wakeAll();
                }
            }
            // CASE 3: Child waiting based on location
            else if (childOnOahu[id]) {
                System.out.println("DEBUG: Child " + id + " waiting on Oahu");
                childOahuCV.sleep();
            }
            else {
                System.out.println("DEBUG: Child " + id + " waiting on Molokai");
                childMolokaiCV.sleep();
            }
            
            lock.release();
            KThread.yield();
        }
    }
    
    static void SampleItinerary() {
        // Not used in our solution
    }
}
