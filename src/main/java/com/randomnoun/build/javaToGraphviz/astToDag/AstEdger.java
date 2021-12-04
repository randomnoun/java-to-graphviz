package com.randomnoun.build.javaToGraphviz.astToDag;

import org.apache.log4j.Logger;

import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;

// class that adds edges to every node in the dag, using the AST structure
public class AstEdger {
    Logger logger = Logger.getLogger(AstEdger.class);
    Dag dag;
    boolean includeThrowEdges;
    
    public AstEdger(Dag dag) {
        this.dag = dag;
    }
    public void setIncludeThrowEdges(boolean includeThrowEdges) {
        this.includeThrowEdges = includeThrowEdges;
    }


    /** Adds the edges for a DagNode into the Dag, and returns the edges leading out of that DagNode
     * (which may now be labelled)
     * 
     * @param dag
     * @param node
     * @param scope a lexical scope, which define the boundary for 'break', 'continue', 'return' and 'throws' statements
     * @return
     */
    public void addEdges(Dag dag, DagNode node, LexicalScope scope) {
        
        if (node.children != null && node.children.size() > 0) {
            for (DagNode c : node.children) {
                DagEdge ce = dag.addEdge(node, c);
                ce.classes.add("ast");
                addEdges(dag, c, scope);
            }
        }
    }
  
}