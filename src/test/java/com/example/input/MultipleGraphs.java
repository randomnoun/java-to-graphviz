package com.example.input;

/** Multiple graphs */
public class MultipleGraphs {
    
    // gv-style: { @import "JavaToGraphviz.css"; }
    /* gv-style: {
           graph#first {
              e: f;
           }
           
           // if this was just .second
           // the rule would apply to graph, graphNode and graphEdge elements, as they all have the same id
           // which they probably shouldn't.
           
           graph.second {
              g: h;
           }

           #thisOne { i : j; }
           #noThisOne { k: l; }
           
           .green { color: green; }
           
       }
     */
    
    // gv-graph#first: a whole new graph { a: b; c: d; }
    
    public void testTheOnlyWayIsUp() {
        System.out.println("baby");
    }

    // gv-graph.second: let me share a whole new graph with you
    
    public void testDownDown() {
        System.out.println("prices are down");
    }

    public void testJitterbug() {
        
        System.out.println("jitterbug");
        
        // gv: a comment
        
        // gv#thisOne: comment with id
        
        // gv.green: comment with class
        
        // gv#noThisOne.green: comment id and a class
        
        // gv#howAboutThisOne.green: comment id and a class and a style { color: blue; }

        

    }

    

}
