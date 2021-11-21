package com.example.input;

/** array creation, initialiser and class instance creation test cases
 * 
 */
public class Expressions4 {

    public class MyNumber {
        long i;
        public MyNumber(Long i) {
            this.i = i;
        }
    }

    public void testExpressions() {

       // references
       long[] numbers = new long[] { 3L, 2L, 1L };
       long[] moreNumbers = new long[100];
       long[][][] evenMoreNumbers = new long[100][3][1];

       MyNumber n = new MyNumber(1234L);
       
    }
    
    

    
}
