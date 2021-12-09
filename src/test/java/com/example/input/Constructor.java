package com.example.input;

/** array creation, initialiser and class instance creation test cases
 * 
 */
public class Constructor {

    public class MyNumber {
        long i;
        public MyNumber(Long i) {
            this.i = i;
        }
        public MyNumber(Long i, int factor) {
            this(i);
            this.i = i * factor;
        }
    }

    public class TripleNumber extends MyNumber {
        public TripleNumber(Long i) {
            super(i, 3);
        }
    }

    public void testConstructors() {

       MyNumber a = new MyNumber(3L);
       TripleNumber b = new TripleNumber(1L);
    }
    
    

    
}
