package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat {
    static BoatGrader bg;
    
    // Shared state variables
    static int adultsOnOahu;
    static int childrenOnOahu;
    static boolean boatOnOahu;
    
    // Keep track of each thread's state
    static boolean[] childOnOahu;
    static boolean[] adultOnOahu;
    
    // Synchronization variables
    static Lock lock;
    static Condition cv;
    
    public static void selfTest() {
        BoatGrader b = new BoatGrader();
        // System.out.println("\n ***Testing Boats with only 2 children***");
        // begin(0, 2, b);
        // System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        // begin(1, 2, b);
        // System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        // begin(3, 3, b);
            begin(4,4,b);
    }
    
    public static void begin(int adults, int children, BoatGrader b) {
        bg = b;
        
        // Initialize shared state
        adultsOnOahu = adults;
        childrenOnOahu = children;
        boatOnOahu = true;
        
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
        cv = new Condition(lock);
        
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
        
        // Wait until conditions are right for this adult to cross
        while (!(boatOnOahu && childrenOnOahu == 1 && adultOnOahu[id])) {
            System.out.println("DEBUG: Adult " + id + " waiting");
            cv.sleep();
        }
        
        // Adult crosses to Molokai
        System.out.println("DEBUG: Adult " + id + " rowing to Molokai");
        
        // Update state
        adultsOnOahu--;
        boatOnOahu = false;
        adultOnOahu[id] = false;
        
        // Row to Molokai
        bg.AdultRowToMolokai();
        
        System.out.println("DEBUG: After adult crosses: Adults on Oahu = " + adultsOnOahu + 
                          ", Children on Oahu = " + childrenOnOahu + 
                          ", Boat on Oahu = " + boatOnOahu);
        
        // Notify everyone of the state change
        cv.wakeAll();
        
        lock.release();
    }
    
    static void ChildItinerary(int id) {
        // Active flag to keep child thread running
        boolean active = true;
        
        while (active) {
            lock.acquire();
            
            // Check if everyone is on Molokai
            if (adultsOnOahu == 0 && childrenOnOahu == 0) {
                System.out.println("DEBUG: Everyone is on Molokai!");
                cv.wakeAll();
                lock.release();
                return;
            }
            
            // CASE 1: Child on Oahu with boat
            if (childOnOahu[id] && boatOnOahu) {
                // Two children can cross
                if (childrenOnOahu >= 2) {
                    System.out.println("DEBUG: Child " + id + " initiating crossing with another child");
                    
                    // Find another child on Oahu to be a passenger
                    int passengerId = -1;
                    for (int i = 0; i < childOnOahu.length; i++) {
                        if (i != id && childOnOahu[i]) {
                            passengerId = i;
                            break;
                        }
                    }
                    
                    if (passengerId != -1) {
                        // Update state for two children crossing
                        childrenOnOahu -= 2;
                        boatOnOahu = false;
                        childOnOahu[id] = false;
                        childOnOahu[passengerId] = false;
                        
                        // First child rows, second rides
                        bg.ChildRowToMolokai();
                        bg.ChildRideToMolokai();
                        
                        System.out.println("DEBUG: After children cross: Adults on Oahu = " + adultsOnOahu + 
                                          ", Children on Oahu = " + childrenOnOahu + 
                                          ", Boat on Oahu = " + boatOnOahu);
                        
                        // Notify everyone of the state change
                        cv.wakeAll();
                    }
                }
                // Child alone on Oahu with no adults - last one to cross
                else if (childrenOnOahu == 1 && adultsOnOahu == 0) {
                    System.out.println("DEBUG: Last child " + id + " crossing to Molokai");
                    
                    // Update state
                    childrenOnOahu--;
                    boatOnOahu = false;
                    childOnOahu[id] = false;
                    
                    // Row to Molokai
                    bg.ChildRowToMolokai();
                    
                    System.out.println("DEBUG: After last child crosses: Adults on Oahu = " + adultsOnOahu + 
                                      ", Children on Oahu = " + childrenOnOahu + 
                                      ", Boat on Oahu = " + boatOnOahu);
                    
                    // Notify everyone
                    cv.wakeAll();
                }
                // Only one child on Oahu with adults
                else if (childrenOnOahu == 1 && adultsOnOahu > 0) {
                    System.out.println("DEBUG: Child " + id + " waiting for adult to cross");
                    cv.wakeAll(); // Wake adults to check if they can cross
                    cv.sleep();
                }
                else {
                    // Shouldn't reach here, but just in case
                    System.out.println("DEBUG: Child " + id + " waiting for state change");
                    cv.sleep();
                }
            }
            // CASE 2: Child on Molokai with boat
            else if (!childOnOahu[id] && !boatOnOahu) {
                // If people still on Oahu, send child back
                if (adultsOnOahu > 0 || childrenOnOahu > 0) {
                    System.out.println("DEBUG: Child " + id + " rowing back to Oahu");
                    
                    // Update state
                    childrenOnOahu++;
                    boatOnOahu = true;
                    childOnOahu[id] = true;
                    
                    // Row to Oahu
                    bg.ChildRowToOahu();
                    
                    System.out.println("DEBUG: After child returns: Adults on Oahu = " + adultsOnOahu + 
                                      ", Children on Oahu = " + childrenOnOahu + 
                                      ", Boat on Oahu = " + boatOnOahu);
                    
                    // Notify everyone of the state change
                    cv.wakeAll();
                }
                else {
                    // Everyone is on Molokai - done!
                    System.out.println("DEBUG: Child " + id + " on Molokai - everyone is here");
                    cv.wakeAll();
                    active = false;
                }
            }
            // CASE 3: Child waiting for boat
            else {
                System.out.println("DEBUG: Child " + id + " waiting for boat, location=" + 
                                 (childOnOahu[id] ? "Oahu" : "Molokai") + 
                                 ", boat=" + (boatOnOahu ? "Oahu" : "Molokai"));
                cv.sleep();
            }
            
            lock.release();
            
            // Small yield to avoid thread starvation
            KThread.yield();
        }
    }
    
    static void SampleItinerary() {
        // Not used in our solution
    }
}
