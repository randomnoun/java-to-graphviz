package com.example.input;

/** Multiple graphs */
public class AllTheControlFlowNodes {
    
    // gv-style: { @import "JavaToGraphviz.css"; }
    
    // keep everything except for the expressionStatements and the blocks
    // gv-keepNode: -expressionStatement -block -switchCase

    public void a() { };
    public void b() { };
    public void c() { };
    public void truePath() { };
    public void falsePath() { };
    public void println() { };
    public void println(int i) { };
    public void before() { };
    public void after() { };
    public void orderDonuts() { };
    
    
    public void testAllTheControlFlowNodes() {
        // whole raft of variables
        boolean condition = true;
        int i, j = 0;
        int[] elements = new int[5];
        
        // gv-graph
        { // gv: Block edges
            a();
            b();
            c();
        }
        
        // gv-graph
        { // gv: If edges
            before();
            if (condition) {
                truePath();
            } else {
                falsePath();
            }
            after();
        }
        
        // gv-graph
        { // gv: For edges
            before();
            for (i = 0; i < 10; i++) {
                println(i);
            }
            after();
        }
        
        // gv-graph
        { // gv: EnhancedFor edges
            before();
            for (int e : elements) {
                println(e);
            }
            after();
        }
        
        // gv-graph
        { // gv: While Edges
            before();
            while (condition) {
                println(i);
            }
            after();
        }

        // gv-graph
        { // gv: Do Edges
            before();
            do {
                println(i);
            } while (condition);
            after();
        }

        // gv-graph
        { // gv: Switch Edges
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
        { // gv: Switch Edges (alternate)
            before();
            switch(i) {
                case 0: println(); // fallthrough
                case 1: println(); break; 
                default: println();
            }
            after();
        }

        
        // gv-graph
        { // gv: InfixExpression edges
            println(1 + 2 / 3);
        }

        // gv-graph
        { // gv: UnaryExpression edges
            i++;
        }

        // gv-graph
        { // gv: TernaryExpression edges
            println(condition ? i : j);
        }
        
        // gv-endgraph
        
        // there's more than this of course
        
    }
    
    // gv-graph
    // gv-keepNode: -methodDeclaration -expressionStatement -block -switchCase
    public void example() {
        orderDonuts();  // gv: order some donuts
    }


}
