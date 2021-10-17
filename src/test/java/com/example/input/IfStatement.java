package com.example.input;

import java.util.ArrayList;
import java.util.List;

/** for loop test cases
 * 
 */
public class IfStatement {

    public void testIf() {
        // gv: start of method
        boolean iThink = false;
        if (iThink) {
            System.out.println("I am");
        }
    }

    public void testIfElse() {
        // gv: start of method
        boolean toBe = false;
        if (toBe) {
            System.out.println("that is the question");
        } else if (!toBe) {
            System.out.println("that is another question");
        }
    }
    
    public void testNestedIf() {
        // variations on a theme
        boolean youWantMyBody = false;
        boolean youThinkImSexy = false;
        if (youWantMyBody) {
            if (youThinkImSexy) {
                System.out.println("come on baby let me know");
            }
        }
    }
    
}
