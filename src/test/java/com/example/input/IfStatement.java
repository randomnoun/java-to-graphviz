package com.example.input;

/** for loop test cases
 * 
 */
public class IfStatement {

    /* gv-style: {
    // css doesn't normally allow // comments but we do here
    // statement classes
    .if {  
        shape : diamond; 
        gv-wordwrap: 20; 
    } 
    // custom classes
    .state { fontname : "Courier New"; style: filled; gv-idFormat: "${label}";  }
    }
    */


    // gv: comment just before the method
    public void testIf() {
        // gv: start of method
        boolean iThink = false;
        if (iThink) { // gv: This is a reasonably long comment to see how wordwrap works
            System.out.println("I am"); // gv.state: EXISTENCE_CONFIRMED { color: red }
        }
    }

    public void testIfElse() { // gv: comment on the method line
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
