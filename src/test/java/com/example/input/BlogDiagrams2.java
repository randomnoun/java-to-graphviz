package com.example.input;

/** Multiple graphs 
 * 
 * <p>This file creates the diagrams which are in the blog post
 * 
 */
public class BlogDiagrams2 {
    
    /* gv-style: {
        // some rules 
        .something { color: red; }
        .special { color: green; }
        #unique { shape: hexagon; }
    } 
    */    

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
    public void someCode() { };
    public void someOtherCode(boolean x) { };
    
    
    public void testAllTheControlFlowNodes() {
        // whole raft of variables
        boolean condition = true;
        int i, j = 0;
        int[] elements = new int[5];

        // keep everything except for the expressionStatements and the blocks

        // gv-keepNode: startNode -expressionStatement -block -switchCase    
        // gv-option: removeNodes=false
        // gv-graph
        
        {
            before();
            for (i = 0; i < 10; i++) {
                println(i);
            }
            after();
        }

        
        // gv-endGraph
        
        // and again with the filters enabled
        
        // gv-keepNode: -expressionStatement -block -switchCase    
        // gv-option: removeNodes=true
        // gv-graph
        
        {
            before();
            for (i = 0; i < 10; i++ /* gv:<: increment things */ ) { // gv: count things
                println(i); // gv: print things
            }
            after();
        }

        
        // gv-endGraph
        
        
    }
    
    
}
