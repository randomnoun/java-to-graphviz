package com.example.input;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Predicate;

/** Multiple graphs 
 * 
 * <p>This file creates the diagrams which are in the README.md
 * 
 */
public class AllTheControlFlowNodes {
    
    /* gv-style: {
        // some rules 
        .something { color: red; }
        .special { color: green; }
        #unique { shape: hexagon; }
    } 
    */    
    // keep everything except for the expressionStatements and the blocks
    // gv-keepNode: -expressionStatement -block -switchCase +methodInvocation

    public void a() { };
    public void b() { };
    public void c() { };
    public void truePath() { };
    public void falsePath() { };
    public void println() { };
    public void println(int i) { };
    public void println(boolean b) { };
    public void println(String s) { };
    public void before() { };
    public void after() { };
    public void orderDonuts() { };
    public void someCode() { };
    public void someOtherCode(boolean x) { };
    
    
    public void testAllTheControlFlowNodes() {
        // whole raft of variables
        boolean condition = true;
        boolean a = true, b = false, c = true;
        int i, j = 0;
        int[] elements = new int[5];
        
        // gv-graph
        // gv-keepNode: -expressionStatement -block -switchCase +methodInvocation
        {
            a();
            b();
            c();
        }
        
        // gv-graph
        {
            before();
            if (condition) {
                truePath();
            } else {
                falsePath();
            }
            after();
        }
        
        // gv-graph
        {
            before();
            for (i = 0; i < 10; i++) {
                println(i);
            }
            after();
        }
        
        // gv-graph
        {
            before();
            for (int e : elements) {
                println(e);
            }
            after();
        }
        
        // gv-graph
        {
            before();
            while (condition) {
                println(i);
            }
            after();
        }

        // gv-graph
        {
            before();
            do {
                println(i);
            } while (condition);
            after();
        }

        // gv-graph
        {
            before();
            switch(i) {
                case 0: println(); // fallthrough
                case 1: println(); break; 
                default: println();
            }
            after();
        }

        // gv-graph
        // gv-option: centralSwitch=true
        {
            before();
            switch(i) {
                case 0: println(); // fallthrough
                case 1: println(); break; 
                default: println();
            }
            after();
        }

        
        // gv-graph
        {
            println(1 + 2 / 3);
        }

        // gv-graph
        {
            i++;
        }

        // gv-graph
        {
            println(condition ? i : j);
        }

        // gv-graph
        {
            println(a || (b && c));
        }

        // gv-graph
        {
            println(new Predicate<Integer>() {
                @Override
                public boolean test(Integer n) {
                    return n % 2 == 0;
                }
                
            }.test(99));
        }

        // gv-graph
        {
            Predicate<Integer> isEven = (Integer n) -> n % 2 == 0;
            println(isEven.test(99));
        }


        // gv-graph
        {
            byte[] buf = new byte[0];
            try {
                buf = new byte[1000000];
            } catch (OutOfMemoryError oome) {
                buf = new byte[1024];
            } finally {
                println(buf.length);
            }
        }

        // gv-graph
        {
            try (InputStream is = new FileInputStream("test-in.txt")) {
                OutputStream os = new FileOutputStream("test-out.txt");
            } catch (IOException ioe) {
                println("ioexception");
            } finally {
                println("finally");
            }
        }

        
        
        // gv-endgraph
        
        // there's more than this of course
        
    }
    
    // gv-graph
    // gv-option: defaultKeepNode=false
    // gv-keepNode: -startNode -methodDeclaration -methodDeclarationEnd -expressionStatement -block
    public void example() {
        before();  // gv: { color: transparent; fontcolor: transparent; style: unset; }
        orderDonuts();  // gv: order some donuts
        after();   // gv: { color: transparent; fontcolor: transparent; style: unset; }
    }


    // gv-graph
    // gv-option: defaultKeepNode=false
    // gv-keepNode: -startNode -methodDeclaration -methodDeclarationEnd -expressionStatement -block
    public void example2() {
        before();  // gv: { color: transparent; fontcolor: transparent; style: unset; }
        orderDonuts();  // gv: order some donuts { shape: oval; color: blue; fontcolor: blue; }
        after();   // gv: { color: transparent; fontcolor: transparent; style: unset; }
    }

    // gv-graph
    // gv-option: defaultKeepNode=false
    // gv-keepNode: -startNode -methodDeclaration -methodDeclarationEnd -expressionStatement -block
    public void example3() {
        before(); // gv: the beginning { fillcolor: grey; } 
        someCode(); // gv.something: well hello there
        someOtherCode(true); // gv.something.special: well hello there again
        someOtherCode(false); // gv#unique.something: righteo then
    }
    

    
}

