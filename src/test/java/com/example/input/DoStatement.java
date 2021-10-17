package com.example.input;

/** do loop test cases
 * 
 */
public class DoStatement {

    
    public void testDo() {
        // gv: start of method
        boolean walopbamboom = true;
        do { 
            System.out.println("wopaloobah");
        } while (walopbamboom);
    }
    

    // gv-subgraph: something
    public void testNestedDo() {
        // gv: start of method
        boolean toTheLeft = true;
        boolean toTheRight = false;
        boolean jumpingUpAndDown = true;
        
        // aaa gaaaa
        do { 
            do {
                do {
                    System.out.println("push pineapple shake the tree");
                } while (toTheLeft);
            } while (toTheRight);
        } while (jumpingUpAndDown);
    }
    
    

    
}
