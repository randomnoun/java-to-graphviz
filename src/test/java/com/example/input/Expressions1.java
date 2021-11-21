package com.example.input;

/** do loop test cases
 * 
 */
public class Expressions1 {

   public void testExpressions() {
       
       boolean x = true || false;
       boolean y = true;
       boolean z = true;
       System.out.println( x || y || z);
       System.out.println( x && y && z);
       System.out.println( x || y && z);
       
       Integer i = 0;
       i++;
       ++i;
       System.out.println(i instanceof Object);
       System.out.println((Number) i);
       System.out.println((((x))));
       
       
    }
    
    

    
}
