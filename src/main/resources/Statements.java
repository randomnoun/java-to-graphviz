package com.jacobistrategies.web.struts;

public class AuthenticateInterceptorGVOLD extends AbstractInterceptor {



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
	   
}