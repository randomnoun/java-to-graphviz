package com.example.input;

/** named break/continue test cases
 * 
 * <p>This didn't work nearly as well as I thought it would.
 * 
 * <p>If we can position the literal '{ rank }' parts of the graphviz diagram inside the subgraphs 
 * that contain the nodes in that rank then it looks slightly better but it's still a bit shit.
 * 
 */
public class ContinueBreakRank {

    // gv-style: { @import "JavaToGraphviz.css"; }
    
    public void testAmenBreak() {
        boolean beatBoxing = true;
        boolean interruptingCowbell = false;
        boolean bass; // base ?
        boolean snare;
        boolean hihat;
    
        // that's bar1 (one), not barl (barl), by the way
        
        
        bar1: for (int semiquaver = 0; semiquaver < 16; semiquaver++) {
            hihat = (semiquaver % 2) == 0; // hihats on the quavers
            
            // gv-subgraph: around the switch
            
            switch (semiquaver) {
                case 0: bass = true; break; // gv#case0:
                case 1: break; // gv#case1:
                case 2: bass = true; break; // gv#case2:
                case 3: break; // gv#case3
                case 4: snare = true; break; // gv#case4
                case 5: break; // gv#case5
                case 6: break; // gv#case6
                case 7: snare = true; if (interruptingCowbell) { break bar1; } // gv#case7
                case 8: break; // gv#case8
                case 9: snare = true; break; // gv#case9
                case 10: bass = true; break; // gv#case10
                case 11: bass = true; break; // gv#case11
                case 12: snare = true; break; // gv#case12
                case 13: break; // gv#case13
                case 14: break; // gv#case14
                case 15: snare = true; break; // gv#case15
                default: // gv#caseDefault
                    throw new IllegalStateException("Out of time");
            };
            
            // gv-end
            
            // gv-literal: { rank = same; case0; case1; case2; case3; case4; case5; case6; case7; case8; case9; case10; case11; case12; case13; case14; case15; caseDefault }
            if (beatBoxing && semiquaver == 0) {
                System.out.println("bpppff");
                continue bar1;
            }
            if (beatBoxing && semiquaver == 12) {
                System.out.println("shaka laka");
                break bar1;
            }
            System.out.print(".");
        }
        if (interruptingCowbell) {
            System.out.println("bong");
        }
        System.out.println();
    }
    
}
