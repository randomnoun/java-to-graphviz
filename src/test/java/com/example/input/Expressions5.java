package com.example.input;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** lambdas
 * 
 */
public class Expressions5 {

    public void testFunction() {
       Function<String, Integer> toInteger = (String a) -> {
           return Integer.parseInt(a);
       };
       Function<Integer, Integer> addOne = i -> i + 1;
       
       int result = toInteger.andThen(addOne).apply("eleven");
       System.out.println(result);
    }
    
    public void testBiConsumer() {
        BiConsumer<String, Integer> bc = (String a, Integer b) -> {
            System.out.println("consumed " + a);
            System.out.println("consumed " + b);
            System.out.println("*burp*");
        };
        bc.accept("one", 23);
    }

    public void testProducer() {
        Supplier<String> stringSupplier = () -> {
            return "everybody loves string";
        };
        String s = stringSupplier.get();
    }

    public void testPredicate() {
        Predicate<String> isEven = (String s) -> s.length() % 2 == 0;
        System.out.println(isEven.test("eleven"));
    }
    
}
