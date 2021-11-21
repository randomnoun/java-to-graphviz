package com.example.input;

/** do loop test cases
 * 
 */
public class Expressions {

   public Expressions a(boolean b) { return this; }
   public Expressions b(boolean b) { return this; }
   public Expressions c(boolean b) { return this; }
    
   public void testExpressions() {
        
       Expressions e = new Expressions();
       e.a(true).b(false).c(true);
       
       boolean x = true;
       boolean y = true;
       e.a(x ? x : y).b(x && y).c(x || y);
       
    }
    
    

    
}
