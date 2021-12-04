package com.example.input;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** anonymous inner classes
 * same as Expressions5 but ye olde anonymous classes instead
 *  
 */
public class Expressions6 {

    public void testFunction() {
       Function<String, Integer> toInteger = new Function<>() {
            @Override
            public Integer apply(String a) {
                return Integer.parseInt(a);
            }
       };
       Function<Integer, Integer> addOne = new Function<>() {
           @Override
           public Integer apply(Integer i) {
               return i + 1;
           }
       };
       
       int result = toInteger.andThen(addOne).apply("eleven");
       System.out.println(result);
    }
    
    public void testBiConsumer() {
        BiConsumer<String, Integer> bc = new BiConsumer<>() {
            @Override
            public void accept(String a, Integer b) {
                System.out.println("consumed " + a);
                System.out.println("consumed " + b);
                System.out.println("*burp*");
            }
        };
        bc.accept("one", 23);
    }

    public void testProducer() {
        Supplier<String> stringSupplier = new Supplier<>() {
            @Override
            public String get() {
                return "everybody loves string";
            }
        };
        String s = stringSupplier.get();
    }

    public void testPredicate() {
        Predicate<String> isEven = new Predicate<>() {
            @Override
            public boolean test(String s) {
                return s.length() % 2 == 0;
            }
            
        };
        System.out.println(isEven.test("eleven"));
    }
    
}
