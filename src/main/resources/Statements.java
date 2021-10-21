package com.jacobistrategies.web.struts;

public class AuthenticateInterceptorGVOLD extends AbstractInterceptor {
    // $NON-NLS$ you say

    /* gv-style: {
       // css doesn't normally allow // comments but we do here
       // statement classes
       .if {  
           shape : diamond; 
           // gv-wordwrap: 10; 
       } 
       // custom classes
       .state { fontname : "Courier New"; style: filled; }
       .clearSession { color : red; style:filled; fillcolor:pink; }
       .newSession { color : green; style:filled; fillcolor:palegreen; }
       .literal { gv-literal: true }
       }
    */
    

    /*
    @Override
    public void forStatement() {
        // gv: start of method
        something();
        
        for (var i=0; i<10; i++) { // gv: loop de loop
            something(); // gv: in the loop
        }
        return invocation.invoke();
    }
    */


    /*
   @Override
    public void enhancedForStatement() {
        // gv: start of method
        something();
        String[] things;
        
        for (String thing : things) { // gv: extended loop de loop
            something(); // gv: in the loop
        }
        return invocation.invoke();
    }
    */

    /*
    public void switchStatement() {
        // gv: start of method
        something();
        int thing;
        switch (thing) {
        
            case 1: something(); //gv: case 1
                if (condition) {
                    conditional();
                    break;
                }
                // conditional fallthrough
                
            case 2: // comment on case 2
                something(); // comment on case 2 body, fallthrough
              
            case 3:
                something();
                break;
                
            // no default + break out of last case
                
        }
        somethingElse();
    }
    */

    /*
    public void anotherStatement() {
        // gv: start of method
        something();
        int thing;
        switch (thing) {
        
            case 1: something(); //gv: case 1
                if (condition) {
                    conditional();
                    break;
                }
                // conditional fallthrough
                
            case 2: // comment on case 2
                something(); // comment on case 2 body, fallthrough
              
            case 3:
                something();
                break;
                
            default:
               something(); // the default body
                
        }
        somethingElse();
    }
    */
    
    /*
    public void whileStatement() {
        // gv: start of method
        something();
        int thing;
        while (condition) {
            something();  
            if (skipCondition) {
                something(); //
                continue; // gv: continue
            } else if (breakCondition) {
                something(); //
                break; // gv: end
            }
            something(); // 
        }
        somethingElse();
    }
    */

    /*
    public void doStatement() {
        // gv: start of method
        something();
        int thing;
        do {
            something();  
            goto really;
            
            if (skipCondition) {
                something(); //
                continue; // gv: continue
            } else if (breakCondition) {
                something(); //
                break; // gv: end
            } else if (throwCondition) {
                throw new something(); // gv: throw

            } else if (returnCondition) {
                return;
            }
            something(); // 
        } while (condition);
        
        somethingElse();
    }*/
    
    
    public void labels() {
        // gv: start of method
        something();
        
        doLabel: do {
            something();
            
            whileLabel: while (wc) {
                something(); // 
                
                if (rc) { // gv.specialIf: OR IS IT
                    something(); //
                    break doLabel;
                } else if (breakCondition) {
                    somethingElse();
                    break whileLabel;
                }
            }
            something(); // 
        } while (condition);
        
        somethingElse();
    }
    
	   
}