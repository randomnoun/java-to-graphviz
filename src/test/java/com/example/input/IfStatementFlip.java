package com.example.input;

/** for loop test cases
 * 
 */
public class IfStatementFlip {

    // gv-style: { @import "JavaToGraphviz.css"; }
    /* gv-style: {
         edge.if.true { gv-labelFormat: "Yes"; }
         edge.if.false { gv-labelFormat: "No"; }
         // these two rules should be ~="flipYn", not *="flipYn"
         // in jsoup, ~=xxx treats xxx as a regex and includes quotes in that regex so that it never matches
         edge.if.true[inNodeClass*="flipYn"] { gv-labelFormat: "No"; }  
         edge.if.false[inNodeClass*="flipYn"] { gv-labelFormat: "Yes"; }
       }
    */


    // gv: comment just before the method
    public void testIf() {
        // gv: start of method
        Object user = null;
        
        // gv: user login tests
        
        if (user == null) { // gv: null user ?
            System.out.println("user not logged in");
        } else {
            System.out.println("user logged in");
        }
        
        // gv: the exact same test but labelled differently
        
        if (user == null) { // gv.flipYn: logged in ?
            System.out.println("user not logged in");
        } else {
            System.out.println("user logged in");
        }
    }
    
}
