package com.example.input;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** method reference and inner class test cases
 * 
 */
public class Expressions3a {

    public class MyNumber {
        long i;
        public MyNumber(Long i) {
            this.i = i;
        }
        public Long getNumber() { return i; }
    }

    public class MyOtherNumber extends MyNumber {
        public MyOtherNumber(Long i) {
            super(i);
        }
        public Long getNumber() { return i * 2; }
        public Long getOrigNumber() {
            Supplier<Long> thisGetter = this::getNumber;
            Supplier<Long> superGetter = super::getNumber;
            return ((Math.random() > 0.5) ? thisGetter : superGetter).get();
        }
        public void qualifiedThis() {
            Expressions3a.this.testExpressions();
        }
        public Long callSuperMethod() {
            return super.getNumber();
        }
        public Long getSuperField() {
            return super.i;
        }

    }

    public void testExpressions() {

       // references
       List<Long> numbers = Arrays.asList(3L, 2L, 1L);
       numbers.stream().sorted(Long::compareTo);
       
       MyNumber[] myNumbers = numbers.stream()
         .map(MyOtherNumber::new)
         .toArray(MyOtherNumber[]::new);
       
       Supplier<Long> aGetter = myNumbers[0]::getNumber;
       // aGetter.get();
       
       MyNumber arrayAccess = myNumbers[2];
       
    }
    
    

    
}
