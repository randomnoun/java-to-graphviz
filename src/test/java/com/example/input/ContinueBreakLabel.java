package com.example.input;

/** named break/continue test cases
 * 
 */
public class ContinueBreakLabel {

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
            switch (semiquaver) {
                case 0: bass = true; break;
                case 1: break;
                case 2: bass = true; break;
                case 3: break;
                case 4: snare = true; break;
                case 5: break;
                case 6: break;
                case 7: snare = true; if (interruptingCowbell) { break bar1; }
                case 8: break;
                case 9: snare = true; break;
                case 10: bass = true; break;
                case 11: bass = true; break;
                case 12: snare = true; break;
                case 13: break;
                case 14: break;
                case 15: snare = true; break;
                default:
                    throw new IllegalStateException("Out of time");
            }
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
    
    public void testWhile() {
        boolean mean = false;
        boolean erst = true;
        boolean cromulent = false;
        System.out.println("don't you mean");
        grammarNaziInspection: while (erst) {
            if (erst) {
                erst = false;
                mean = true;
                continue grammarNaziInspection;
            } else if (mean) {
                mean = false;
                erst = true;
                break;
            } else if (cromulent) {
                mean = false;
                erst = false;
            }
        }
        if (mean) { System.out.println("meanwhile"); }
        if (erst) { System.out.println("erstwhile"); }
        if (cromulent) { System.out.println("perfectly cromulent"); }
        
    }
    
}
