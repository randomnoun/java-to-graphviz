package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;

import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
import com.randomnoun.build.javaToGraphviz.dag.ExitEdge;

// class that adds edges to the dag
public class ControlFlowEdger {
    Logger logger = Logger.getLogger(ControlFlowEdger.class);
    Dag dag;
    boolean includeThrowEdges;
    
    public ControlFlowEdger(Dag dag) {
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
     * @param breakEdges if a 'break' is encountered, a collection to add the outgoing edge to
     * @param continueNode if a 'continue' is encountered, the node to continue to
     * @return
     */
	public List<ExitEdge> addEdges(Dag dag, DagNode node, LexicalScope scope) {
	    if (node.type.equals("MethodDeclaration")) {
	        return addMethodDeclarationEdges(dag, node, scope);
	    } else if (node.type.equals("Block")) {
	        return addBlockEdges(dag, node, scope);
	    } else if (node.type.equals("Synchronized")) {
	        return addSynchronizedEdges(dag, node, scope);
	    } else if (node.type.equals("If")) {
	        return addIfEdges(dag, node, scope);
        } else if (node.type.equals("Try")) {
            return addTryEdges(dag, node, scope);
        } else if (node.type.equals("For")) {
            return addForEdges(dag, node, scope);
        } else if (node.type.equals("EnhancedFor")) {
            return addEnhancedForEdges(dag, node, scope);
        } else if (node.type.equals("Switch")) {
            return addSwitchEdges(dag, node, scope);
        } else if (node.type.equals("SwitchCase")) {
            // these should be handled in addSwitchEdges
            throw new IllegalStateException("SwitchCase encountered outside Switch");
        } else if (node.type.equals("Break")) {
            return addBreakEdges(dag, node, scope);
        } else if (node.type.equals("While")) {
            return addWhileEdges(dag, node, scope);
        } else if (node.type.equals("Continue")) {
            return addContinueEdges(dag, node, scope);
        } else if (node.type.equals("Do")) {
            return addDoEdges(dag, node, scope);
        } else if (node.type.equals("Return")) {
            return addReturnEdges(dag, node, scope);
        } else if (node.type.equals("Throw")) {
            return addThrowEdges(dag, node, scope);
        } else if (node.type.equals("Labeled")) {
            return addLabeledStatementEdges(dag, node, scope);
            
        // goto will be considered tedious if I have to do that. ho ho ho.
            
        } else if (node.type.equals("Assert") ||
          node.type.equals("ConstructorInvocation") ||
          node.type.equals("EmptyStatement") ||
          node.type.equals("Expression") ||
          node.type.equals("SuperConstructorInvocation") ||
          node.type.equals("TypeDeclaration") ||
          node.type.equals("VariableDeclaration") ||
          node.type.equals("comment")) { // lower-case c for nodes created from gv comments
            // non-control flow statement
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            return Collections.singletonList(e);
            
	    } else {
	        logger.warn("non-implemented control flow statement " + node.type);
	        ExitEdge e = new ExitEdge();
	        e.n1 = node;
	        return Collections.singletonList(e);
	    }
	    
	}
	
	// draw lines from each statement to each other
	// exit node is the last statement
	
	private List<ExitEdge> addBlockEdges(Dag dag, DagNode block, LexicalScope scope) {
        // draw the edges from the block
	    ExitEdge start = new ExitEdge();
        start.n1 = block;
	    List<ExitEdge> prevNodes = Collections.singletonList(start);
	    
	    for (DagNode c : block.children) {
	        if (!c.skipNode) {
                for (ExitEdge e : prevNodes) {
                    e.n2 = c;
                    dag.addEdge(e);
                }
                prevNodes = addEdges(dag, c, scope);
	        }
	    }
	    return prevNodes;
    }

    private List<ExitEdge> addSynchronizedEdges(Dag dag, DagNode synchronizedNode, LexicalScope scope) {
        // there's an expression and a block but we don't draw expressions yet
        if (synchronizedNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + synchronizedNode.children.size());
        }

        // draw an edge to the sync node to the block so that we can put a subgraph around the sync node
        DagNode synchronizedBlock = synchronizedNode.children.get(0);
        dag.addEdge(synchronizedNode, synchronizedBlock);
        List<ExitEdge> prevNodes = addBlockEdges(dag, synchronizedBlock, scope);  
        return prevNodes;
    }

    
	// draw lines from each statement to each other
    // returns an empty list
    private List<ExitEdge> addMethodDeclarationEdges(Dag dag, DagNode method, LexicalScope scope) {
        if (method.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + method.children.size());
        }
        MethodDeclaration md = (MethodDeclaration) method.astNode;
        // method.label = "method " + md.getName();
        method.gvAttributes.put("methodName",  md.getName().toString());
        
        DagNode methodBlock = method.children.get(0);
        dag.addEdge(method, methodBlock);
        
        LexicalScope lexicalScope = new LexicalScope(); 
        List<ExitEdge> ee = addBlockEdges(dag, methodBlock, lexicalScope);
        
        // add a node which all the return edges return to
        // this is an artificial node so maybe only construct it based on some gv declaration earlier on ?
        // (whereas all the other nodes are about as concrete as anything else in IT)
        
        // CompilationUnit cu = methodBlock.astNode.getParent();
        CompilationUnit cu = ASTResolving.findParentCompilationUnit(methodBlock.astNode);
        int endOfMethodLine = cu.getLineNumber(methodBlock.astNode.getStartPosition() + methodBlock.astNode.getLength());
        DagNode rn = new DagNode();
        rn.keepNode = true; // always keep comments
        rn.type = "return"; // label this 'end' if it's a void method ?
        rn.lineNumber = endOfMethodLine;
        // rn.name = dag.getUniqueName("m_" + endOfMethodLine);
        rn.classes.add("method");
        rn.classes.add("end");
        // rn.label = "return";
        rn.astNode = null;
        
        DagSubgraph sg = dag.dagNodeToSubgraph.get(method);
        dag.addNode(sg, rn);
        
        for (ExitEdge e : lexicalScope.returnEdges) {
            e.n2 = rn;
            dag.addEdge(e);
        }
        
        // and everything that was thrown connects to this node as well
        if (includeThrowEdges) { 
            for (ExitEdge e : lexicalScope.throwEdges) {
                e.n2 = rn;
                dag.addEdge(e);
            }
        }
        
        // could possibly draw a line from every single node to this node for OOMs etc
        
        // and everything flowing out of the first block connects to this node as well
        for (ExitEdge e : ee) {
            e.n2 = rn;
            dag.addEdge(e);
        }
        
        // there's no exit edges out of a method
        // ExitEdge e = new ExitEdge();
        // e.label = "afterTheReturn";
        // e.n1 = rn;
        return Collections.emptyList();
        
    }
    
	// a break will add an edge to breakEdges only 
	// (and returns an empty list as we won't have a normal exit edge)
	private List<ExitEdge> addBreakEdges(Dag dag, DagNode breakNode, LexicalScope scope) {
	    // BreakStatement b;
	    if (scope.breakEdges == null) { 
	        throw new IllegalStateException("break encountered outside of breakable section");
	    }
        BreakStatement bs = (BreakStatement) breakNode.astNode;
        String label = bs.getLabel() == null ? null : bs.getLabel().toString(); 

        LexicalScope namedScope = scope;
        if (label!=null) {
            namedScope = scope.labeledScopes.get(label);
            if (namedScope == null) { 
                throw new IllegalStateException("no label '" + label + "' in lexical scope");
            }
        }
        
	    ExitEdge e = new ExitEdge();
        e.n1 = breakNode;
        // e.label = "break" + (label==null ? "" : " " + label);
        // e.gvAttributes.put("color", "red"); // @TODO replace all these with classes
        e.classes.add("break");
        e.gvAttributes.put("breakLabel", label == null ? "" : label);
        namedScope.breakEdges.add(e);
        
        return Collections.emptyList();
    }

	// a continue will add an edge back to the continueNode only 
    // (and returns an empty list as we won't have a normal exit edge)
    private List<ExitEdge> addContinueEdges(Dag dag, DagNode continueStatementNode, LexicalScope scope) {
        if (scope.continueNode == null) { 
            throw new IllegalStateException("continue encountered outside of continuable section");
        }
        ContinueStatement cs = (ContinueStatement) continueStatementNode.astNode;
        String label = cs.getLabel() == null ? null : cs.getLabel().toString(); 

        LexicalScope namedScope = scope;
        if (label!=null) {
            namedScope = scope.labeledScopes.get(label);
            if (namedScope == null) { 
                throw new IllegalStateException("no label '" + label + "' in lexical scope");
            }
        }
        
        DagEdge e;
        e = dag.addBackEdge(continueStatementNode, namedScope.continueNode, null); // "continue" + (label==null ? "" : " " + label)
        // e.gvAttributes.put("color", "red");
        e.classes.add("continue");
        e.gvAttributes.put("continueLabel", (label==null ? "" : " " + label));
        return Collections.emptyList();
    }

    private List<ExitEdge> addLabeledStatementEdges(Dag dag, DagNode labeledStatementNode, LexicalScope scope) {
        // there's no edges but remember this statement in the lexical scope
        // or maybe this should create a new lexical scope. maybe it should.
        // so we can break/continue to it
        
        if (labeledStatementNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + labeledStatementNode.children.size());
        }

        DagNode c = labeledStatementNode.children.get(0);
        dag.addEdge(labeledStatementNode, c);

        LabeledStatement ls = (LabeledStatement) labeledStatementNode.astNode;
        String label = ls.getLabel() == null ? null : ls.getLabel().toString();
        c.javaLabel = label;
        
        List<ExitEdge> lsPrevNodes = addEdges(dag, c, scope);
        
        List<ExitEdge> prevNodes = new ArrayList<ExitEdge>();
        prevNodes.addAll(lsPrevNodes); // Y branch
        
        // break/continue edges here ?
        return prevNodes;
    }

    
    
    // a return will add an edge to returnEdges only 
    // (and returns an empty list as we won't have a normal exit edge)
    private List<ExitEdge> addReturnEdges(Dag dag, DagNode breakNode, LexicalScope scope) {
        ExitEdge e = new ExitEdge();
        // e.label = "return";
        e.n1 = breakNode;
        // e.gvAttributes.put("color", "blue");
        e.classes.add("return");
        scope.returnEdges.add(e);
        return Collections.emptyList();
    }

    // a throw will add an edge to throwEdges only 
    // (and returns an empty list as we won't have a normal exit edge)
    private List<ExitEdge> addThrowEdges(Dag dag, DagNode breakNode, LexicalScope scope) {
        ExitEdge e = new ExitEdge();
        // e.label = "throw";
        e.n1 = breakNode;
        // e.gvAttributes.put("color", "purple");
        e.classes.add("throw");
        scope.throwEdges.add(e);
        return Collections.emptyList();
    }


    
	// draw branches from this block to then/else blocks
	// exit edges are combined exit edges of both branches
	private List<ExitEdge> addIfEdges(Dag dag, DagNode block, LexicalScope scope) {
        // draw the edges
	    List<ExitEdge> prevNodes = new ArrayList<>();
	    if (block.children.size() == 1) {
	        DagNode c = block.children.get(0);
            DagEdge trueEdge = dag.addEdge(block, c, null);
            trueEdge.classes.add("if");
            trueEdge.classes.add("true");
            
            List<ExitEdge> branchPrevNodes = addEdges(dag, c, scope);
            ExitEdge e = new ExitEdge();
            e.n1 = block;
            // e.label = "N";
            e.classes.add("if");
            e.classes.add("false");
            
            prevNodes.addAll(branchPrevNodes); // Y branch
            prevNodes.add(e); // N branch
	       
	    } else if (block.children.size() == 2) {
	        DagNode c1 = block.children.get(0);
	        DagNode c2 = block.children.get(1);
            DagEdge trueEdge = dag.addEdge(block, c1, null); trueEdge.classes.add("if"); trueEdge.classes.add("true");
            DagEdge falseEdge = dag.addEdge(block, c2, null); falseEdge.classes.add("if"); falseEdge.classes.add("false");
            List<ExitEdge> branch1PrevNodes = addEdges(dag, c1, scope);
            List<ExitEdge> branch2PrevNodes = addEdges(dag, c2, scope);
            prevNodes.addAll(branch1PrevNodes);
            prevNodes.addAll(branch2PrevNodes);
            
	    } else {
	        throw new IllegalStateException("expected 2 children, found " + block.children.size());
	    }
           
        return prevNodes;
    }
	
	// draw branches into and out of try body
    private List<ExitEdge> addTryEdges(Dag dag, DagNode tryNode, LexicalScope scope) {
        // draw the edges
        TryStatement ts = (TryStatement) tryNode.astNode;
        
        // children of TryStatement are
        /*
            // visit children in normal left to right reading order
            if (this.ast.apiLevel >= AST.JLS4_INTERNAL) {
                acceptChildren(visitor, this.resources);
            }
            acceptChild(visitor, getBody());
            acceptChildren(visitor, this.catchClauses);
            acceptChild(visitor, getFinally());
        */
        @SuppressWarnings("unchecked")
        List<ASTNode> resourcesNodes = ts.resources();
        ASTNode bodyNode = ts.getBody();
        @SuppressWarnings("unchecked")
        List<ASTNode> catchClauseNodes = ts.catchClauses();
        ASTNode finallyNode = ts.getFinally();
        
        List<DagNode> resourceDags = new ArrayList<>();
        DagNode bodyDag = null;
        List<DagNode> catchClauseDags = new ArrayList<>();
        DagNode finallyDag = null;
        
        for (DagNode c : tryNode.children) {
            if (resourcesNodes.contains(c.astNode)) {
                resourceDags.add(c);
                c.classes.add("tryResource");
            } else if (bodyNode == c.astNode) {
                bodyDag = c;
                c.classes.add("tryBody");
            } else if (catchClauseNodes.contains(c.astNode)) {
                catchClauseDags.add(c);
                c.classes.add("catchClause");
            } else if (finallyNode == c.astNode) {
                finallyDag = c;
                c.classes.add("finally");
            } else {
                throw new IllegalStateException("child of TryStatement was not a resource, body, catchClause or finally node");
            }
        }
        if (bodyDag == null) {
            throw new IllegalStateException("try with no body");
        }

        // add clusters for try, catch and finally blocks
        // which we're doing in CSS now instead
        
        /*
        // this occurs before CSS subgraph creation so they're all children of the Dag
        DagSubgraph bodySg = new DagSubgraph(dag, dag);
        bodySg.classes.add("tryBody");
        dag.subgraphs.add(bodySg);
        bodySg.nodes.add(bodyDag);
        if (catchClauseDags.size() > 0) {
            // List<DagSubgraph> catchClauseSgs = new ArrayList<>();
            for (DagNode ccDag : catchClauseDags) {
                DagSubgraph ccSg = new DagSubgraph(dag, dag);
                ccSg.classes.add("catchClause");
                dag.subgraphs.add(ccSg);
                ccSg.nodes.add(ccDag);
            }
        }
        if (finallyDag != null) {
            DagSubgraph finallySg = new DagSubgraph(dag, dag);
            finallySg.classes.add("finally");
            dag.subgraphs.add(finallySg);
            finallySg.nodes.add(finallyDag);
        }
        */
        
        dag.addEdge(tryNode,  bodyDag);
        
        LexicalScope throwScope = scope.newThrowScope();
        List<ExitEdge> tryPrevNodes = new ArrayList<>(addEdges(dag, bodyDag, throwScope));

        for (DagNode ccDag : catchClauseDags) {
            // dag.rootNodes.add(ccDag);
            for (ExitEdge ee : throwScope.throwEdges) {
                // don't know which catch without a type hierarchy so create an edge for each one
                DagEdge de = new DagEdge();
                de.n1 = ee.n1;
                de.n2 = ccDag;
                de.classes.add("throw");
                dag.addEdge(de);
            }
            
            List<ExitEdge> ccPrevNodes = addEdges(dag, ccDag, scope);
            tryPrevNodes.addAll(ccPrevNodes);
        }
        
        if (finallyDag != null) {
            for (ExitEdge ee : tryPrevNodes) {
                ee.n2 = finallyDag;
                dag.addEdge(ee);
            }
            tryPrevNodes = addEdges(dag, finallyDag, scope);
        }
        
        
        return tryPrevNodes;
        // return prevNodes;
    }
    
    // draw branches into and out of for body
    private List<ExitEdge> addForEdges(Dag dag, DagNode forNode, LexicalScope scope) {
        // draw the edges
        ForStatement fs;
        // so could draw this with branches leading back up to the for node from each exit node of repeating block
        // or branches down from each exit node to an artifical block at the bottom, with a branch up from that.
        // which might be a bit cleaner if the for block has a lot of exit edges 
        if (forNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + forNode.children.size());
        }
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = forNode.children.get(0);
        dag.addEdge(forNode, repeatingBlock);
        LexicalScope newScope = scope.newBreakContinueScope(forNode);
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, newScope);
        
        /*
        if (repeatBlockPrevNodes.size() > bunchUpTheEdgesThreshold) {
            // add an artificial node to bunch up the edges
        }
        */
        
        for (ExitEdge e : repeatingBlockPrevNodes) {
            DagEdge backEdge = dag.addBackEdge(e.n1, forNode, null);
            backEdge.classes.add("for");
        }
        
        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire for
        prevNodes.addAll(repeatingBlockPrevNodes);
        prevNodes.addAll(newScope.breakEdges); // forward edges for any breaks inside the for scoped to this for
        return prevNodes;
    }

    // draw branches into and out of extended for body
    private List<ExitEdge> addEnhancedForEdges(Dag dag, DagNode forNode, LexicalScope scope) {
        // draw the edges
        EnhancedForStatement fs;
        // so could draw this with branches leading back up to the for node from each exit node of repeating block
        // or branches down from each exit node to an artifical block at the bottom, with a branch up from that.
        if (forNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + forNode.children.size());
        }
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = forNode.children.get(0);
        dag.addEdge(forNode, repeatingBlock);
        LexicalScope newScope = scope.newBreakContinueScope(forNode);
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, newScope);
        for (ExitEdge e : repeatingBlockPrevNodes) {
            DagEdge backEdge = dag.addBackEdge(e.n1, forNode, null);
            backEdge.classes.add("enhancedFor");
        }

        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire for
        prevNodes.addAll(repeatingBlockPrevNodes);
        prevNodes.addAll(newScope.breakEdges); // forward edges for any breaks inside the for scoped to this for

        return repeatingBlockPrevNodes;
    }
    
    private List<ExitEdge> addWhileEdges(Dag dag, DagNode whileNode, LexicalScope scope) {
        // draw the edges
        WhileStatement ws;
        
        if (whileNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + whileNode.children.size());
        }
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = whileNode.children.get(0);
        DagEdge whileTrue = dag.addEdge(whileNode, repeatingBlock);
        // whileTrue.label = "Y";
        whileTrue.classes.add("while");
        whileTrue.classes.add("true");
        
        // List<ExitEdge> whileBreakEdges = new ArrayList<>();
        LexicalScope newScope = scope.newBreakContinueScope(whileNode);
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, newScope); // new continue node
        for (ExitEdge e : repeatingBlockPrevNodes) {
            DagEdge backEdge = dag.addBackEdge(e.n1, whileNode, null);
            backEdge.classes.add("while");
        }
        
        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire while
        prevNodes.addAll(repeatingBlockPrevNodes);
        prevNodes.addAll(newScope.breakEdges);
        
        // add an edge from the top of the while as well, as it's possible the repeatingBlock may never execute
        ExitEdge e = new ExitEdge();
        e.n1 = whileNode;
        // e.label = "N";
        e.classes.add("while");
        e.classes.add("false");
        
        return prevNodes;
           
    }
    
    private List<ExitEdge> addDoEdges(Dag dag, DagNode doNode, LexicalScope scope) {
        // draw the edges
        // DoStatement ds;
        
        if (doNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + doNode.children.size());
        }
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = doNode.children.get(0);
        DagEdge doTrue = dag.addEdge(doNode, repeatingBlock);
        doTrue.classes.add("do");
        doTrue.classes.add("start");
        
        // List<ExitEdge> doBreakEdges = new ArrayList<>();
        LexicalScope newScope = scope.newBreakContinueScope(doNode); // @XXX continueNode here should be the expression at the end 
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, newScope); // new continue node
        for (ExitEdge e : repeatingBlockPrevNodes) {
            DagEdge backEdge = dag.addBackEdge(e.n1, doNode, null);
            backEdge.classes.add("do");
        }
        
        // @TODO while condition is at the end so could create a node for the evaluation of that as well
        
        
        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire switch
        prevNodes.addAll(repeatingBlockPrevNodes);
        prevNodes.addAll(newScope.breakEdges);
        return prevNodes;
           
    }
    

           
    
    // ye olde switch, not whatever they're doing in java 16 these days
    // maybe I'm thinking of javascript.
    // List<ExitEdge> _breakEdges, DagNode continueNode
    private List<ExitEdge> addSwitchEdges(Dag dag, DagNode switchNode, LexicalScope scope) {
     // draw the edges
        // SwitchStatement ss;
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : switchNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        /*
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.IfStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.BreakStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
         */
        // ^ the children of a switchNode interleaves SwitchCases, statements, and break statements
        
        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire switch
        
        List<ExitEdge> casePrevNodes = new ArrayList<>(); // a single case in the switch
        // List<ExitEdge> caseBreakEdges = new ArrayList<>();
        
        // this starts a new 'break' scope but 'continue's continue to do whatever continues did before.
        // OR DO THEY.
        LexicalScope newScope = null; // scope.newBreakScope();
        
        boolean hasDefaultCase = false;
        for (DagNode c : switchNode.children) {
            if (c.type.equals("SwitchCase")) {
                // close off last case
                if (newScope != null) {
                    prevNodes.addAll(newScope.breakEdges);
                }

                // pretty sure default needs to be the last case but maybe not
                boolean isDefaultCase = ((SwitchCase) c.astNode).getExpression() == null;               

                // start a new one
                DagEdge caseEdge = dag.addEdge(switchNode, c, null); // case expression
                caseEdge.classes.add("case");
                if (isDefaultCase) {
                    caseEdge.classes.add("default");
                }
                
                for (ExitEdge e : casePrevNodes) { // fall-through edges. maybe these should be red instead of the break edges
                    e.n2 = c;
                    dag.addEdge(e);
                    e.classes.add("switch");
                    e.classes.add("fallthrough");
                }
                casePrevNodes = new ArrayList<>();
                // caseBreakEdges = new ArrayList<>();
                // scope.breakEdges.clear();
                newScope = scope.newBreakScope();
                
                hasDefaultCase = hasDefaultCase || isDefaultCase; 
                
                
                ExitEdge e = new ExitEdge();
                e.n1 = c;
                casePrevNodes.add(e);
                
            } else {
                // any other statement is linked to the previous one, similar to a BlockStatement
                if (casePrevNodes.size() == 0) {
                    logger.warn("no edges leading to statement in case. Maybe statement after a breakStatement ? ");
                }
                for (ExitEdge e : casePrevNodes) {
                    e.n2 = c;
                    dag.addEdge(e);
                }
                casePrevNodes = addEdges(dag, c, newScope);
            }
        }
        
        // edges out of the final case are also edges out of the switch
        prevNodes.addAll(newScope.breakEdges);
        prevNodes.addAll(casePrevNodes);
        
        // if there was no 'default' case, then also include an edge from the switchNode
        if (!hasDefaultCase) {
            ExitEdge e = new ExitEdge();
            e.n1 = switchNode;
            e.classes.add("switch");
            e.classes.add("false");
            prevNodes.add(e);
        }

        return prevNodes;
    }    
    
}