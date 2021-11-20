package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;

import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
import com.randomnoun.build.javaToGraphviz.dag.EntryEdge;
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
        } else if (node.type.equals("ExpressionStatement")) {
            return addExpressionStatementEdges(dag, node, scope);
            
            
        // goto will be considered tedious if I have to do that. ho ho ho.
            
        } else if (node.type.equals("Assert") ||
          node.type.equals("ConstructorInvocation") ||
          node.type.equals("EmptyStatement") ||
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
        // there's an expression and a block but we don't draw expressions yet. OH YES WE DO.t
        SynchronizedStatement ss = (SynchronizedStatement) synchronizedNode.astNode;
        

        // draw an edge to the sync node to the block so that we can put a subgraph around the sync node
        // DagNode synchronizedBlock = synchronizedNode.children.get(0);
        DagNode synchronizedBlock = getDagChild(synchronizedNode.children, ss.getBody(), null);
        dag.addEdge(synchronizedNode, synchronizedBlock);
        List<ExitEdge> prevNodes = addBlockEdges(dag, synchronizedBlock, scope);  
        return prevNodes;
    }

    
	// draw lines from each statement to each other
    // returns an empty list
    private List<ExitEdge> addMethodDeclarationEdges(Dag dag, DagNode method, LexicalScope scope) {
        MethodDeclaration md = (MethodDeclaration) method.astNode;
        // method.label = "method " + md.getName();
        method.gvAttributes.put("methodName",  md.getName().toString());
        
        // last child is the block
        DagNode methodBlock = method.children.get(method.children.size() - 1);
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
        
        // label, body
        if (labeledStatementNode.children.size() != 2) {
            throw new IllegalStateException("expected 2 children; found " + labeledStatementNode.children.size());
        }

        DagNode c = labeledStatementNode.children.get(1);
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
	private List<ExitEdge> addIfEdges(Dag dag, DagNode ifNode, LexicalScope scope) {
        // draw the edges
	    // now have a node for the condition
	    
	    List<ExitEdge> prevNodes = new ArrayList<>();
	    if (ifNode.children.size() == 2) {
	        DagNode c = ifNode.children.get(1);
            DagEdge trueEdge = dag.addEdge(ifNode, c, null);
            trueEdge.classes.add("if");
            trueEdge.classes.add("true");
            
            List<ExitEdge> branchPrevNodes = addEdges(dag, c, scope);
            ExitEdge e = new ExitEdge();
            e.n1 = ifNode;
            // e.label = "N";
            e.classes.add("if");
            e.classes.add("false");
            
            prevNodes.addAll(branchPrevNodes); // Y branch
            prevNodes.add(e); // N branch
	       
	    } else if (ifNode.children.size() == 3) {
	        DagNode c1 = ifNode.children.get(1);
	        DagNode c2 = ifNode.children.get(2);
            DagEdge trueEdge = dag.addEdge(ifNode, c1, null); trueEdge.classes.add("if"); trueEdge.classes.add("true");
            DagEdge falseEdge = dag.addEdge(ifNode, c2, null); falseEdge.classes.add("if"); falseEdge.classes.add("false");
            List<ExitEdge> branch1PrevNodes = addEdges(dag, c1, scope);
            List<ExitEdge> branch2PrevNodes = addEdges(dag, c2, scope);
            prevNodes.addAll(branch1PrevNodes);
            prevNodes.addAll(branch2PrevNodes);
            
	    } else {
	        throw new IllegalStateException("expected 2 or 3 children, found " + ifNode.children.size());
	    }
           
        return prevNodes;
    }
	
	// draw branches into and out of try body
    private List<ExitEdge> addTryEdges(Dag dag, DagNode tryNode, LexicalScope scope) {
        // draw the edges
        TryStatement ts = (TryStatement) tryNode.astNode;
        
        List<DagNode> resourceDags = getDagChildren(tryNode.children, ts.resources(), "tryResource");
        DagNode bodyDag = getDagChild(tryNode.children, ts.getBody(), "tryBody");
        List<DagNode> catchClauseDags = getDagChildren(tryNode.children, ts.catchClauses(), "finally");
        DagNode finallyDag = getDagChild(tryNode.children, ts.getFinally(), "finally");
        
        if (bodyDag == null) {
            throw new IllegalStateException("try with no body");
        }
        
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
    
    private DagNode getDagChild(List<DagNode> children, ASTNode astNode, String className) {
        for (DagNode c : children) {
            if (astNode == c.astNode) {
                if (className != null) { 
                    c.classes.add(className);
                }
                return c;
            }
        }
        return null;
    }
    private List<DagNode> getDagChildren(List<DagNode> children, List<?> astNodes, String className) {
        List<DagNode> result = new ArrayList<>();
        for (DagNode c : children) {
            if (astNodes.contains(c.astNode)) {
                result.add(c);
                if (className != null) { 
                    c.classes.add(className);
                }
            }
        }
        return result;
    }
    // draw branches into and out of for body
    private List<ExitEdge> addForEdges(Dag dag, DagNode forNode, LexicalScope scope) {
        // draw the edges
        ForStatement fs = (ForStatement) forNode.astNode;
        // so could draw this with branches leading back up to the for node from each exit node of repeating block
        // or branches down from each exit node to an artifical block at the bottom, with a branch up from that.
        // which might be a bit cleaner if the for block has a lot of exit edges 

        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        
        DagNode repeatingBlock = getDagChild(forNode.children, fs.getBody(), null);
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
        EnhancedForStatement fs = (EnhancedForStatement) forNode.astNode;
        // so could draw this with branches leading back up to the for node from each exit node of repeating block
        // or branches down from each exit node to an artifical block at the bottom, with a branch up from that.
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        // DagNode repeatingBlock = forNode.children.get(0);
        DagNode repeatingBlock = getDagChild(forNode.children, fs.getBody(), null);
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
        WhileStatement ws = (WhileStatement) whileNode.astNode;
        
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = getDagChild(whileNode.children, ws.getBody(), null);
        // DagNode repeatingBlock = whileNode.children.get(0);
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
        DoStatement ds = (DoStatement) doNode.astNode;
        
        
        
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = getDagChild(doNode.children, ds.getBody(), null);
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
 
    /************************************************************************************************
     ************************************************************************************************ 
     ***
     *** Expression nodes
     ***
     ***
     */
    
    private List<ExitEdge> addExpressionStatementEdges(Dag dag, DagNode expressionStatementNode, LexicalScope scope) {
        ExpressionStatement es = (ExpressionStatement) expressionStatementNode.astNode;
        Expression e = es.getExpression();
        if (expressionStatementNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + expressionStatementNode.children.size());
        }
        DagNode eNode = expressionStatementNode.children.get(0);
        dag.addEdge(expressionStatementNode, eNode);
        
        return addExpressionEdges(dag, eNode, scope);
    }

    // this will probably go into the ExpressionEdger later
    // so what we should probably be doing is converting this into the RPN sequence of evaluations
    // or maybe that's RRPN.
    // so c = a + b becomes
    // evaluate a -> evaluate b -> addition -> assignment
    
    
    public List<ExitEdge> addExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        if (node.type.equals("MethodInvocation")) {
            return addMethodInvocationEdges(dag, node, scope);

        } else if (node.type.equals("ConditionalExpression")) {
            return addConditionalExpressionEdges(dag, node, scope);

        } else if (node.type.equals("SimpleName") ||
            node.type.equals("QualifiedName")) {
            return addNameEdges(dag, node, scope);
            
        } else if (node.type.equals("BooleanLiteral") ||
            node.type.equals("CharacterLiteral") || 
            node.type.equals("NumberLiteral") ||
            node.type.equals("NullLiteral") ||
            node.type.equals("TypeLiteral")) {
            
            // @TODO mark as ignored, move comments up the tree
            // return Collections.emptyList();
            
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            return Collections.singletonList(e);

        } else if (node.type.equals("InfixExpression")) {
            return addInfixExpressionEdges(dag, node, scope);

        } else {
            logger.warn("non-implemented expression " + node.type);

            // edge the AST so we've got something for now
            node.classes.add("ast");
            addAstEdges(dag, node, scope);
            
            // non-control flow statement
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            return Collections.singletonList(e);
        }
        
    }

    private void addAstEdges(Dag dag, DagNode node, LexicalScope scope) {
        
        if (node.children != null && node.children.size() > 0) {
            for (DagNode c : node.children) {
                DagEdge ce = dag.addEdge(node, c);
                ce.classes.add("ast");
                addExpressionEdges(dag, c, scope);
            }
        }
    }

    private List<ExitEdge> addMethodInvocationEdges(Dag dag, DagNode methodInvocationNode, LexicalScope scope) {
        MethodInvocation mi = (MethodInvocation) methodInvocationNode.astNode;
        DagNode expressionDag = getDagChild(methodInvocationNode.children, mi.getExpression(), null);
        // DagNode name = getDagChild(methodInvocationNode.children, mi.getName(), null);
        methodInvocationNode.gvAttributes.put("methodName", mi.getName().toString());
        List<DagNode> argumentDags = getDagChildren(methodInvocationNode.children, mi.arguments(), null);

        // move methodInvocation node after the expression & argument nodes
        List<DagEdge> inEdges = new ArrayList<>();
        for (DagEdge e : dag.edges) { if ( e.n2 == methodInvocationNode ) { inEdges.add(e); } }
        if (inEdges.size() == 0) { throw new IllegalStateException("no inEdges"); }

        // first node is inEdge.n2
        EntryEdge inEdge = new EntryEdge();
        
        // expression is null for method calls within the same object
        List<ExitEdge> prevNodes = null;
        if (expressionDag != null) {
            inEdge.n2 = expressionDag;
            dag.edges.add(inEdge);
            prevNodes = addExpressionEdges(dag, expressionDag, scope);
        }
        for (DagNode a : argumentDags) {
            if (prevNodes != null) {
                for (ExitEdge e : prevNodes) {
                    e.n2 = a;
                    e.classes.add("invocationArgument");
                    dag.addEdge(e);
                }
            } else {
                inEdge.n2 = a;
                dag.edges.add(inEdge);
            }
            prevNodes = addExpressionEdges(dag, a, scope );
        }
        
        // move the top node here instead
        if (prevNodes != null) {
            // inEdge may have been rejiggered as well though, so the firstNode may no longer be the firstNode
            for (DagEdge e : inEdges) {
                e.n2 = inEdge.n2;  // this is so going to work.  t// firstNode; // @TODO probably some other structures to change here
            }
            for (DagEdge e : prevNodes) {
                e.n2 = methodInvocationNode;
                dag.edges.add(e);
            }
            dag.edges.remove(inEdge);
        }
        ExitEdge e = new ExitEdge();
        e.n1 = methodInvocationNode;
        prevNodes = Collections.singletonList(e);
        return prevNodes;
    }   
    

    private List<ExitEdge> addInfixExpressionEdges(Dag dag, DagNode ieNode, LexicalScope scope) {
        InfixExpression ie = (InfixExpression) ieNode.astNode;
        DagNode leftDag = getDagChild(ieNode.children, ie.getLeftOperand(), null);
        DagNode rightDag = getDagChild(ieNode.children, ie.getRightOperand(), null);
        List<DagNode> extendedDags = getDagChildren(ieNode.children, ie.extendedOperands(), null);
        
        ieNode.gvAttributes.put("operator", ie.getOperator().toString());
        
        // a + b      becomes a -> b -> +
        // a + b + c  should becomes a -> b -> + -> c -> + , which has two + nodes, even though there's only one in the AST. because you know. eclipse.
        
        // InfixExpressions also include shortcut || which doesn't evaluate the second parameter if the first is true
        // so it should be a something a bit more complicated, control-flow wise
        
        // a 
        // true? -N->  b
        //  :Y         :   
        //  v          v          
        
        // move infixExpression node after the a and b nodes
        List<DagEdge> inEdges = new ArrayList<>();
        for (DagEdge e : dag.edges) { if ( e.n2 == ieNode ) { inEdges.add(e); } }
        if (inEdges.size() == 0) { throw new IllegalStateException("no inEdges"); }

        // first node is inEdge.n2
        EntryEdge inEdge = new EntryEdge();
        
        // not entirely sure why I deconstructed the list in order to reconstruct it here,
        // but hey 
        List<DagNode> allDags = new ArrayList<DagNode>();
        allDags.add(leftDag);
        allDags.add(rightDag);
        allDags.addAll(extendedDags);
        
        // expression is null for method calls within the same object
        List<ExitEdge> prevNodes = null;
        for (DagNode a : allDags) {
            if (prevNodes != null) {
                for (ExitEdge e : prevNodes) {
                    e.n2 = a;
                    // e.classes.add("invocationArgument");
                    dag.addEdge(e);
                }
            } else {
                inEdge.n2 = a;
                dag.edges.add(inEdge);
            }
            prevNodes = addExpressionEdges(dag, a, scope );
        }
        
        // move the top node here instead
        if (prevNodes != null) {
            // inEdge may have been rejiggered as well though, so the firstNode may no longer be the firstNode
            for (DagEdge e : inEdges) {
                e.n2 = inEdge.n2;  // this is so going to work.  t// firstNode; // @TODO probably some other structures to change here
            }
            for (DagEdge e : prevNodes) {
                e.n2 = ieNode;
                dag.edges.add(e);
            }
            dag.edges.remove(inEdge);
        }
        ExitEdge e = new ExitEdge();
        e.n1 = ieNode;
        prevNodes = Collections.singletonList(e);
        return prevNodes;
        
        
        
    }

        
    private List<ExitEdge> addConditionalExpressionEdges(Dag dag, DagNode ceNode, LexicalScope scope) {
        ConditionalExpression ce = (ConditionalExpression) ceNode.astNode;
        
        DagNode expressionDag = getDagChild(ceNode.children, ce.getExpression(), null);
        DagNode thenDag = getDagChild(ceNode.children, ce.getThenExpression(), null);
        DagNode elseDag = getDagChild(ceNode.children, ce.getElseExpression(), null);
        
        List<ExitEdge> ee = addExpressionEdges(dag, expressionDag, scope);
        for (DagEdge e : ee) {
            e.n2 = ceNode;
        }
                
        List<ExitEdge> prevNodes = new ArrayList<>();
        DagEdge trueEdge = dag.addEdge(ceNode, thenDag, null); trueEdge.classes.add("conditionalExpression"); trueEdge.classes.add("true");
        DagEdge falseEdge = dag.addEdge(ceNode, elseDag, null); falseEdge.classes.add("conditionalExpression"); falseEdge.classes.add("false");
        List<ExitEdge> branch1PrevNodes = addExpressionEdges(dag, thenDag, scope);
        List<ExitEdge> branch2PrevNodes = addExpressionEdges(dag, elseDag, scope);
        
        prevNodes.addAll(branch1PrevNodes);
        prevNodes.addAll(branch2PrevNodes);
        return prevNodes;
    }
    
    
    private List<ExitEdge> addNameEdges(Dag dag, DagNode nameNode, LexicalScope scope) {
        Name n = (Name) nameNode.astNode; // SimpleName or QualifiedName
        nameNode.gvAttributes.put("name", n.getFullyQualifiedName());
        
        // names don't have edges
        // return Collections.emptyList();
        ExitEdge e = new ExitEdge();
        e.n1 = nameNode;
        return Collections.singletonList(e);
    }
    
}
