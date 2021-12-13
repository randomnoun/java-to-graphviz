package com.example.input;

/** Multiple graphs 
 * 
 * <p>This file creates the diagrams which are in the blog post
 * 
 */
public class BlogDiagrams {
    
    /* gv-style: {
        // some rules 
        .something { color: red; }
        .special { color: green; }
        #unique { shape: hexagon; }
    } 
    */    
    // keep everything except for the expressionStatements and the blocks
    // gv-keepNode: startNode -expressionStatement -block -switchCase

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
        
        // gv-graph
        // gv-keepNode: -expressionStatement -block -switchCase
        {
            a(); // gv: step 1
            b(); // gv: step 2
            c(); // gv: step 3
        }
        
        // gv-graph
        {
            before();  // gv: step 1
            if (condition) { // gv: make a\ndecision
                truePath();  // gv: option 1
            } else {
                falsePath(); // gv: option 2
            }
            after(); // gv: step 3
        }
        
        // gv-endgraph
        
        // there's more than this of course
        
    }
    
    
}
