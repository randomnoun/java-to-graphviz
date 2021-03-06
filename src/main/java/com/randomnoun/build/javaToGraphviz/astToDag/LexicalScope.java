package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.ExitEdge;

// a lexical scope corresponds to some boundary in the AST that we want to demarcate somehow.
// Scopes can be nested; a nested scope may inherit a subset of the parent scope's fields depending 
// on the type of scope.
public class LexicalScope {

    // a collection of edges from 'break' statements
    // these are always forward edges
    List<ExitEdge> breakEdges;

    // within this scope, the node that a 'continue' will continue to
    // an edge to a continueNode is always a back edge
    DagNode continueNode;
    // in a 'do' statement, continues jump forward, so we collect them instead
    // (forwardContinue = true)
    boolean continueForward = false;
    List<ExitEdge> continueEdges;
    
    // a collection of edges from 'return' statements
    // these are always forward edges
    List<ExitEdge> returnEdges;


    // a collection of edges from 'throw' statements
    // if we can get the exception type hierarchy out of this thing then
    // we could channel them into the right subgraph
    // these are always forward edges
    List<ExitEdge> throwEdges;

    // break/continue targets
    Map<String, LexicalScope> labeledScopes;
    
    
    // new method scope
    public LexicalScope() {
        breakEdges = new ArrayList<ExitEdge>();
        continueNode = null;
        continueForward = false;        
        returnEdges = new ArrayList<ExitEdge>();
        throwEdges = new ArrayList<ExitEdge>();
        
        // I'm assuming the same label name can never be nested within a method
        // suppose it could if you had anonymous classes in this thing. yeesh. anonymous classes.
        // let's ignore that for now.
        labeledScopes = new HashMap<String, LexicalScope>();
    }
    
    // new lambda scope
    public LexicalScope newLambdaScope() {
        // pretty sure everything's contained in the lambda, control-flow wise
        LexicalScope next = new LexicalScope();
        next.breakEdges = new ArrayList<ExitEdge>();
        next.continueNode = null;
        continueForward = false;
        next.returnEdges = new ArrayList<ExitEdge>();
        next.throwEdges = new ArrayList<ExitEdge>();
        next.labeledScopes = new HashMap<String, LexicalScope>();
        return next;
    }
    
    // scope that bounds statements that can break/continue
    // e.g. for, while
    // the 'while' is labelled, but we continue to the expression in the while
    public LexicalScope newBreakContinueScope(DagNode labelledNode, DagNode continueNode) {
        LexicalScope next = new LexicalScope();
        next.breakEdges = new ArrayList<>();
        next.continueNode = continueNode;
        next.continueForward = false;
        next.returnEdges = this.returnEdges;
        next.throwEdges = this.throwEdges;
        next.labeledScopes = this.labeledScopes;
        
        // if this DagNode is labelled, add this scope to the labeledScopes map
        // @TODO remove these from the scope when the scope closes. If that's the right verb. wraps up.
        // @TODO maybe add these to the scope as we reach them in the Edger class
        if (labelledNode.javaLabel != null) {
            labeledScopes.put(labelledNode.javaLabel, next);
        }
        return next;
    }

    // scope that bounds statements that can break/continue, but where the continue is a forward edge
    // e.g. do
    public LexicalScope newBreakContinueScope(DagNode labelledNode) {
        LexicalScope next = new LexicalScope();
        next.breakEdges = new ArrayList<>();
        next.continueNode = null;
        next.continueForward = true;
        next.continueEdges = new ArrayList<>();
        next.returnEdges = this.returnEdges;
        next.throwEdges = this.throwEdges;
        next.labeledScopes = this.labeledScopes;
        
        // if this DagNode is labelled, add this scope to the labeledScopes map
        // @TODO remove these from the scope when the scope closes. If that's the right verb. wraps up.
        // @TODO maybe add these to the scope as we reach them in the Edger class
        if (labelledNode.javaLabel != null) {
            labeledScopes.put(labelledNode.javaLabel, next);
        }
        return next;
    }

    
    // scope that bounds statements that can break
    // e.g. case within a switch
    public LexicalScope newBreakScope() {
        LexicalScope next = new LexicalScope();
        next.breakEdges = new ArrayList<>();
        next.continueNode = this.continueNode;
        next.continueForward = this.continueForward;
        next.continueEdges = this.continueEdges;
        next.returnEdges = this.returnEdges;
        next.throwEdges = this.throwEdges;
        next.labeledScopes = this.labeledScopes;
        return next;
    }
    
    // scope that bounds statements that can throw
    // return edges are also captured here so they can get routed into the finally
    public LexicalScope newThrowScope() {
        LexicalScope next = new LexicalScope();
        next.breakEdges = this.breakEdges;
        next.continueNode = this.continueNode;
        next.continueForward = this.continueForward;
        next.continueEdges = this.continueEdges;
        next.returnEdges = new ArrayList<>();
        next.throwEdges = new ArrayList<>();
        next.labeledScopes = this.labeledScopes;
        return next;
    }

    // new class, interface, inner class/interface, or anonymous class
    public LexicalScope newTypeScope() {
        // pretty sure everything's contained in the lambda, control-flow wise
        LexicalScope next = new LexicalScope();
        next.breakEdges = new ArrayList<ExitEdge>();
        next.continueNode = null;
        next.continueForward = false;
        next.returnEdges = new ArrayList<ExitEdge>();
        next.throwEdges = new ArrayList<ExitEdge>();
        next.labeledScopes = new HashMap<String, LexicalScope>();
        return next;
   }

    
    
    
    // scope for user-defined subgraphs, does not affect nodes and edges.
    // going to allow these around most grouping nodes, although I was hoping to do that when we apply CSS rules
    // which won't be possible if I'm doing this within blocks as well.
    // OK so maybe I allow subgraphs to be defined both within comments (constructed when the Dag is being constructed)
    // or via classes (constructed when the styles are being applied)
    // easy peasy.
    // am I going to allow unbalanced subgraphs ( subgraph boundaries that don't line up with AST boundaries ) ? probably not.
    /* OK so user-defined subgraphs don't create a scope
    public LexicalScope newSubgraphScope() {
        LexicalScope next = new LexicalScope();
        next.breakEdges = this.breakEdges;
        next.continueNode = this.continueNode;
        next.returnEdges = this.returnEdges;
        next.throwEdges = this.throwEdges;
        next.labeledScopes = this.labeledScopes;
        return next;
    }
    */
    

}