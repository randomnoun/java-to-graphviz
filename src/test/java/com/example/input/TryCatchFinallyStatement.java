package com.example.input;

/** try / catch / finally test cases
 * 
 */
public class TryCatchFinallyStatement {

    
    public void testTenthBirthday() {
        System.out.println("thank's for the ball");
        System.out.println("come on let's play");
        try {
            if (true) {
                throw new Exception();
            } else {
                System.out.println("I'm gonna be like him");
            }
        } catch (Exception e) {
            System.out.println("not today");
            System.out.println("I got a lot to do");
        } finally {
            System.out.println("that's okay");
        }
    }
    
}
