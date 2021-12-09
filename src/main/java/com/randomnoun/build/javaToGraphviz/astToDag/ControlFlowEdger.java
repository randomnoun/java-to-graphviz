package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;

import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
import com.randomnoun.build.javaToGraphviz.dag.EntryEdge;
import com.randomnoun.build.javaToGraphviz.dag.ExitEdge;
import com.randomnoun.common.Text;

/** Class that adds control-flow edges to the dag.
 * 
 * @author knoxg
 */
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
	    
	    // guess most of these can contain expressions as well. argh.
	    if (node.type.equals("TypeDeclaration")) {
	        return addTypeDeclarationEdges(dag, node, scope);
	        
	    } else if (node.type.equals("MethodDeclaration")) {
	        return addMethodDeclarationEdges(dag, node, scope);
	    } else if (node.type.equals("VariableDeclarationFragment")) {
	        // instance variables; ignore for now
	        ExitEdge e = new ExitEdge();
            e.n1 = node;
            return Collections.singletonList(e);
	        
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
        } else if (node.type.equals("CatchClause")) {
            return addCatchClauseEdges(dag, node, scope);
        } else if (node.type.equals("Labeled")) {
            return addLabeledStatementEdges(dag, node, scope);
        } else if (node.type.equals("ExpressionStatement")) {
            return addExpressionStatementEdges(dag, node, scope);
        } else if (node.type.equals("VariableDeclaration")) {
            return addVariableDeclarationStatementEdges(dag, node, scope);
        } else if (node.type.equals("SingleVariableDeclaration")) {
            return addSingleVariableDeclarationEdges(dag, node, scope);
        } else if (node.type.equals("ConstructorInvocation")) {
            return addConstructorInvocationEdges(dag, node, scope);
        } else if (node.type.equals("SuperConstructorInvocation")) {
            return addSuperConstructorInvocationEdges(dag, node, scope);

        // goto will be considered tedious if I have to do that. ho ho ho.
            
        } else if (node.type.equals("Assert") ||
          node.type.equals("Empty") ||
          node.type.equals("comment")) { // lower-case c for nodes created from gv comments
            // non-control flow statement
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            return Collections.singletonList(e);
            
	    } else {
	        // names in the package declaration trigger this
	        logger.warn("non-implemented control flow statement " + node.type);
	        ExitEdge e = new ExitEdge();
	        e.n1 = node;
	        return Collections.singletonList(e);
	    }
	    
	}

	/** Utility method to find a child DagNode corresponding to a particular ASTNode.
	 * 
	 * <p>If className is non-null, that class is added to the DagNode before it is returned.
	 *  
	 * @param children the children of a DagNode
	 * @param astNode an ASTNode
	 * @param className an optional className to add to the returned DagNode
	 * 
	 * @return the DagNode that matches the astNode, or null if it does not exist
	 */
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

    /** Utility method to find child DagNodes corresponding to a list of ASTNodes.
     * 
     * <p>If className is non-null, that class is added to each DagNode in the returned list.
     *  
     * @param children the children of a DagNode
     * @param astNode a collection of ASTNodes
     * @param className an optional className to add to each returned DagNode
     * 
     * @return a list of DagNodes that match the astNodes, or an empty list if there are no matching nodes
     */
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

	// in the css you want to put a subgraph around the synchronizedBlock ( the expression is hoisted above this node )
    private List<ExitEdge> addSynchronizedEdges(Dag dag, DagNode synchronizedNode, LexicalScope scope) {
        SynchronizedStatement ss = (SynchronizedStatement) synchronizedNode.astNode;
        
        DagNode expressionDag = getDagChild(synchronizedNode.children, ss.getExpression(), null);
        DagNode synchronizedBlock = getDagChild(synchronizedNode.children, ss.getBody(), null);
        
        Rejigger rejigger = hoistNode(dag, synchronizedNode, expressionDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, expressionDag, scope);
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        
        for (ExitEdge e : prevNodes) {
            e.n2 = synchronizedBlock;
            dag.addEdge(e);
        }
        
        prevNodes = addBlockEdges(dag, synchronizedBlock, scope);  
        return prevNodes;
    }

    
    
    private List<ExitEdge> addTypeDeclarationEdges(Dag dag, DagNode typeNode, LexicalScope scope) {
        TypeDeclaration td = (TypeDeclaration) typeNode.astNode;
        // method.label = "method " + md.getName();
        if (td.isInterface()) {
            typeNode.classes.add("interface");
            typeNode.gvAttributes.put("interfaceName", td.getName().toString());
        } else {
            typeNode.classes.add("class");
            typeNode.gvAttributes.put("className", td.getName().toString());
        }
        
        LexicalScope lexicalScope = scope.newTypeScope();
        List<DagNode> bodyDeclarationDags = getDagChildren(typeNode.children, td.bodyDeclarations(), null);

        // add edges for everything in the class
        // @TODO check fields don't appear though. unless they initalise things, then maybe. Or maybe that goes into the constructors. 
        // or not.
        // yeesh what about the static initializers. what about them indeed.
        for (DagNode n : bodyDeclarationDags) {
            // System.out.println(n.type);
            // other type declarations and method declarations, it looks like
            addEdges(dag, n, lexicalScope);
        }
        
        return Collections.emptyList();
        
    }
    
	// draw lines from each statement to each other, with an artifical node at the end
    // that all the thrown exceptions throw to, and that all the return statements return to.
    // 
    // methods don't have any edges leaving them, but we add one anyway so that we can layout
    // methods inside anonymous classes better in graphviz. ( this edge will be transparent )
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
        DagNode returnNode = new DagNode();
        returnNode.keepNode = method.keepNodeMatcher.matches("methodDeclarationEnd"); 
        returnNode.type = "methodDeclarationEnd"; // label this 'end' if it's a void method ?
        returnNode.lineNumber = endOfMethodLine;
        // rn.name = dag.getUniqueName("m_" + endOfMethodLine);
        returnNode.classes.add("method");
        returnNode.classes.add("end");
        // rn.label = "return";
        returnNode.astNode = null;
        method.children.add(returnNode); // keep the return node in the method grouping
        
        DagSubgraph sg = dag.dagNodeToSubgraph.get(method);
        dag.addNode(sg, returnNode);
        
        for (ExitEdge e : lexicalScope.returnEdges) {
            e.n2 = returnNode;
            dag.addEdge(e);
        }
        
        // and everything that was thrown connects to this node as well
        if (includeThrowEdges) { 
            for (ExitEdge e : lexicalScope.throwEdges) {
                e.n2 = returnNode;
                dag.addEdge(e);
            }
        }
        
        // could possibly draw a line from every single node to this node for OOMs etc
        
        // and everything flowing out of the first block connects to this node as well
        for (ExitEdge e : ee) {
            e.n2 = returnNode;
            dag.addEdge(e);
        }
        
        // there's no exit edges out of a method
        // return Collections.emptyList();
        
        // maybe there is now so we can draw an edge out of anonymous classes
        ExitEdge e = new ExitEdge();
        e.n1 = returnNode;
        return Collections.singletonList(e);
        
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

	// a continue will add an edge back to the continueNode if we have one
	// or added to the scope's continueEdges collection if we don't
    // (and returns an empty list as we won't have a normal exit edge)
    private List<ExitEdge> addContinueEdges(Dag dag, DagNode continueStatementNode, LexicalScope scope) {
        if (scope.continueNode == null && !scope.continueForward) { 
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
        
        if (namedScope.continueNode != null) { 
            DagEdge e;
            e = dag.addBackEdge(continueStatementNode, namedScope.continueNode, null);
            e.classes.add("continue");
            e.gvAttributes.put("continueLabel", label == null ? "" : " " + label);
        } else if (namedScope.continueForward) {
            ExitEdge e = new ExitEdge();
            e.n1 = continueStatementNode;
            e.classes.add("continue");
            e.gvAttributes.put("continueLabel", label == null ? "" : label);
            namedScope.continueEdges.add(e);
        } else {
            // this seems like a bit of an edge-case. ah boom tish.
            throw new IllegalStateException("named scope is not continuable");
        }
        
        return Collections.emptyList();
    }

    private List<ExitEdge> addLabeledStatementEdges(Dag dag, DagNode labeledStatementNode, LexicalScope scope) {
        // remember this statement in the lexical scope so we can break/continue to it
        
        LabeledStatement ls = (LabeledStatement) labeledStatementNode.astNode;
        DagNode c = getDagChild(labeledStatementNode.children, ls.getBody(), null);
        String label = ls.getLabel() == null ? null : ls.getLabel().toString();
        
        dag.addEdge(labeledStatementNode, c);

        c.javaLabel = label;
        
        List<ExitEdge> lsPrevNodes = addEdges(dag, c, scope);
        
        List<ExitEdge> prevNodes = new ArrayList<ExitEdge>();
        prevNodes.addAll(lsPrevNodes); // Y branch
        
        return prevNodes;
    }

    
    
    // a return will add an edge to returnEdges only 
    // (and returns an empty list as we won't have a normal exit edge)
    private List<ExitEdge> addReturnEdges(Dag dag, DagNode node, LexicalScope scope) {
        ReturnStatement rs = (ReturnStatement) node.astNode;
        DagNode expressionDag = getDagChild(node.children, rs.getExpression(), null);

        // the expressionDag is after the return in the AST, but we want to evaluate it before the 
        // return statement (which already has edges leading to it), so we 'hoist' it above the 
        // return node in the DAG. This process is one of the 'rejiggering' operations. 
        // The opposite process when we add the return node back in is called 'unhoisting'. 
        
        ExitEdge e;
        if (expressionDag != null) {
            Rejigger rejigger = hoistNode(dag, node, expressionDag);
            List<ExitEdge> prevNodes = addExpressionEdges(dag, expressionDag, scope);
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
            if (prevNodes.size() != 1) { throw new IllegalStateException("expected 1 exitEdge"); }
            e = prevNodes.get(0);
        } else {
            e = new ExitEdge();
            e.n1 = node;    
        }
        
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
        IfStatement is = (IfStatement) ifNode.astNode;
        DagNode exprDag = getDagChild(ifNode.children, is.getExpression(), null);

        Rejigger rejigger = hoistNode(dag, ifNode, exprDag);
        List<ExitEdge> ee = addExpressionEdges(dag, exprDag,  scope);
        /*List<ExitEdge> prevNodes =*/ rejigger.unhoistNode(dag,  ee); // create exit edges below

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
	
	// draw branches into and out of try statements
	// try/catch/finally's are weird things to draw; what I've done is:
	// * try resources link to try body
	// * try body links to finally body
	// * any throw edge in the try body links to all catch bodies 
	//   ( ideally it'd just link to the correct catch body, but that requires knowing the class hierarchies, which we don't know yet )
	// * return edges from the try body lead into the finally
	// * exit edges are from the finally body, or from the try & catch bodies if there's no finally
	// in the css, it helps if you put subgraphs with borders around the try, catch and finally bodies.
    private List<ExitEdge> addTryEdges(Dag dag, DagNode tryNode, LexicalScope scope) {
        // draw the edges
        TryStatement ts = (TryStatement) tryNode.astNode;
        
        List<DagNode> resourceDags = getDagChildren(tryNode.children, ts.resources(), "tryResource");
        DagNode bodyDag = getDagChild(tryNode.children, ts.getBody(), "tryBody");
        List<DagNode> catchClauseDags = getDagChildren(tryNode.children, ts.catchClauses(), "catch"); // tryCatch ?
        DagNode finallyDag = getDagChild(tryNode.children, ts.getFinally(), "finally"); // tryFinally ?
        boolean hasResource = false;
        
        if (bodyDag == null) {
            throw new IllegalStateException("try with no body");
        }
        LexicalScope throwScope = scope.newThrowScope();
        
        ExitEdge ee = new ExitEdge();
        ee.n1 = tryNode;
        List<ExitEdge> prevNodes = Collections.singletonList(ee);
        for (DagNode rd : resourceDags) {
            if (prevNodes != null) {
                for (ExitEdge e : prevNodes) {
                    e.n2 = rd;
                    dag.addEdge(e);
                }
            }
            // if exceptions occur here they pass to the exception handler defined here 
            prevNodes = addExpressionEdges(dag, rd, throwScope);
            hasResource = true;
        }

        for (ExitEdge e : prevNodes) {
            e.n2 = bodyDag;
            dag.addEdge(e);
        }
        // dag.addEdge(tryNode,  bodyDag);
        
        List<ExitEdge> tryPrevNodes = new ArrayList<>(addEdges(dag, bodyDag, throwScope));

        for (DagNode ccDag : catchClauseDags) {
            // if hasResource == true, we could create an artificial node at the top of each catch clause 
            // that closes the Closable resources
            
            for (ExitEdge e : throwScope.throwEdges) {
                // don't know which catch without a type hierarchy so create an edge for each one
                DagEdge de = new DagEdge();
                de.n1 = e.n1;
                de.n2 = ccDag;
                de.classes.add("throw");
                dag.addEdge(de);
            }
            
            // original scope for exceptions thrown in exception handlers
            // @TODO not sure about 'return's here though. feel they should still go through the finally.
            List<ExitEdge> ccPrevNodes = addEdges(dag, ccDag, scope); 
            tryPrevNodes.addAll(ccPrevNodes);
        }
        
        if (finallyDag != null) {
            // if hasResource == true, we could create an artificial node at the top of the finally clause 
            // that closes the Closable resources

            // returns within the try{} finally{} go through the finally
            boolean returnIntoFinally = false, otherIntoFinally = false;;
            for (ExitEdge e : throwScope.returnEdges) {
                e.n2 = finallyDag;
                dag.addEdge(e);
                returnIntoFinally = true;
            }
            for (ExitEdge e : tryPrevNodes) {
                e.n2 = finallyDag;
                dag.addEdge(e);
                otherIntoFinally = true;
            }
            tryPrevNodes = addEdges(dag, finallyDag, scope);
            
            // if there was a return edge leading into the finally, 
            // then this finally can return as well
            if (returnIntoFinally) {
                // could add an artifical return node rather than an edge from each prevNode
                // (as we do for methods)
                for (ExitEdge e : tryPrevNodes) {
                    ee = new ExitEdge();
                    ee.n1 = e.n1;
                    ee.classes.add("return");
                    scope.returnEdges.add(ee);
                }
                if (!otherIntoFinally) {
                    // everything returns
                    tryPrevNodes = Collections.emptyList();
                }
            }
            
        
        } else {
            // no finally block, returns are handled as before 
            scope.returnEdges.addAll(throwScope.returnEdges);
        }
        
        return tryPrevNodes;

    }
    
    private List<ExitEdge> addCatchClauseEdges(Dag dag, DagNode catchNode, LexicalScope scope) {
        CatchClause cc = (CatchClause) catchNode.astNode;
        catchNode.gvAttributes.put("exceptionSpec", cc.getException().toString());
        
        DagNode bodyDag = getDagChild(catchNode.children, cc.getBody(), null);
        dag.addEdge(catchNode, bodyDag);
        
        List<ExitEdge> prevNodes = addEdges(dag, bodyDag, scope);
        return prevNodes;
    }

    
    
    // draw branches into and out of for body
    private List<ExitEdge> addForEdges(Dag dag, DagNode forNode, LexicalScope scope) {
        // draw the edges
        ForStatement fs = (ForStatement) forNode.astNode;
        
        List<DagNode> initialiserDags = getDagChildren(forNode.children, fs.initializers(), "initialiser");
        DagNode exprDag = getDagChild(forNode.children, fs.getExpression(), "expression");
        List<DagNode> updaterDags = getDagChildren(forNode.children, fs.updaters(), "updater");
        DagNode repeatingBlock = getDagChild(forNode.children, fs.getBody(), null);

        // move forStatement node after the initialisation & expression nodes
        Rejigger rejigger = hoistNode(dag, forNode, initialiserDags.size() == 0 ? exprDag : initialiserDags.get(0));
        
        // expression is null for method calls within the same object
        List<ExitEdge> prevNodes = null;
        for (DagNode i : initialiserDags) {
            if (prevNodes != null) {
                for (ExitEdge e : prevNodes) {
                    e.n2 = i;
                    dag.addEdge(e);
                }
            }
            prevNodes = addExpressionEdges(dag, i, scope );
        }

        DagEdge firstExpressionEdge = null;
        if (prevNodes != null) {
            for (ExitEdge e : prevNodes) {
                e.n2 = exprDag;
                dag.addEdge(e);
                firstExpressionEdge = e;
            }
        }
        prevNodes = addExpressionEdges(dag, exprDag, scope);
        
        // this is the node we loop back to
        DagNode firstExpressionNode = firstExpressionEdge == null ? rejigger.inEdge.n2 : firstExpressionEdge.n2;
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        
        
        
        DagEdge forTrue = prevNodes.get(0);
        forTrue.classes.add("for");
        forTrue.classes.add("true");
        forTrue.n2 = repeatingBlock;
        dag.addEdge(forTrue);
        
        LexicalScope newScope = scope.newBreakContinueScope(forNode, firstExpressionNode);
        prevNodes = addEdges(dag, repeatingBlock, newScope);
        
        for (DagNode u : updaterDags) {
            for (ExitEdge e : prevNodes) {
                e.n2 = u;
                dag.addEdge(e);
            }
            prevNodes = addExpressionEdges(dag, u, scope );
        }
        
        for (ExitEdge e : prevNodes) {
            DagEdge backEdge = dag.addBackEdge(e.n1, firstExpressionNode, null); // exprDag
            backEdge.classes.add("for");
        }
        
        prevNodes = new ArrayList<>(); // the entire for
        // prevNodes.addAll(repeatingBlockPrevNodes);  // could add hidden edges here to force the 'after loop' nodes to appear under the for
        prevNodes.addAll(newScope.breakEdges); // forward edges for any breaks inside the for scoped to this for
        
        ExitEdge forFalse = new ExitEdge();
        forFalse.n1 = forNode;
        forFalse.classes.add("for");
        forFalse.classes.add("false");
        prevNodes.add(forFalse);
        
        return prevNodes;
    }

    // draw branches into and out of extended for body
    private List<ExitEdge> addEnhancedForEdges(Dag dag, DagNode forNode, LexicalScope scope) {
        // draw the edges
        EnhancedForStatement fs = (EnhancedForStatement) forNode.astNode;
        
        DagNode initialiserDag = getDagChild(forNode.children, fs.getParameter(), "initialiser");
        DagNode exprDag = getDagChild(forNode.children, fs.getExpression(), "expression");
        DagNode repeatingBlock = getDagChild(forNode.children, fs.getBody(), null);

        // move forStatement node after the initialisation & expression nodes
        Rejigger rejigger = hoistNode(dag, forNode, initialiserDag);
        
        // expression is null for method calls within the same object
        List<ExitEdge> prevNodes = addEdges(dag, initialiserDag, scope );

        DagEdge firstExpressionEdge = null;
        if (prevNodes != null) {
            for (ExitEdge e : prevNodes) {
                e.n2 = exprDag;
                dag.addEdge(e);
                firstExpressionEdge = e;
            }
        }
        prevNodes = addExpressionEdges(dag, exprDag, scope);
        
        // this is the node we loop back to
        DagNode firstExpressionNode = firstExpressionEdge == null ? rejigger.inEdge.n2 : firstExpressionEdge.n2;
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        
        DagEdge forTrue = prevNodes.get(0);
        forTrue.classes.add("enhancedFor");
        forTrue.classes.add("true");
        forTrue.n2 = repeatingBlock;
        dag.addEdge(forTrue);
        
        LexicalScope newScope = scope.newBreakContinueScope(forNode, firstExpressionNode);
        prevNodes = addEdges(dag, repeatingBlock, newScope);
        
        for (ExitEdge e : prevNodes) {
            DagEdge backEdge = dag.addBackEdge(e.n1, firstExpressionNode, null); // exprDag
            backEdge.classes.add("enhancedFor");
        }
        
        prevNodes = new ArrayList<>(); // the entire for
        // prevNodes.addAll(repeatingBlockPrevNodes);  // could add hidden edges here to force the 'after loop' nodes to appear under the for
        prevNodes.addAll(newScope.breakEdges); // forward edges for any breaks inside the for scoped to this for
        
        ExitEdge forFalse = new ExitEdge();
        forFalse.n1 = forNode;
        forFalse.classes.add("enhancedFor");
        forFalse.classes.add("false");
        prevNodes.add(forFalse);
        
        return prevNodes;
        
    }
    
    
    
    
    private List<ExitEdge> addWhileEdges(Dag dag, DagNode whileNode, LexicalScope scope) {
        // draw the edges
        WhileStatement ws = (WhileStatement) whileNode.astNode;
        
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode exprDag = getDagChild(whileNode.children, ws.getExpression(), null);
        DagNode repeatingBlock = getDagChild(whileNode.children, ws.getBody(), null);

        
        // move forStatement node after the initialisation & expression nodes
        Rejigger rejigger = hoistNode(dag, whileNode, exprDag);

        List<ExitEdge> prevNodes = addExpressionEdges(dag, exprDag, scope);
        
        // this is the node we loop back to
        DagNode firstExpressionNode = rejigger.inEdge.n2;
        prevNodes = rejigger.unhoistNode(dag, prevNodes);

        // DagNode repeatingBlock = whileNode.children.get(0);
        DagEdge whileTrue = dag.addEdge(whileNode, repeatingBlock);
        // whileTrue.label = "Y";
        whileTrue.classes.add("while");
        whileTrue.classes.add("true");
        
        // List<ExitEdge> whileBreakEdges = new ArrayList<>();
        LexicalScope newScope = scope.newBreakContinueScope(whileNode, firstExpressionNode);
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, newScope); // new continue node
        for (ExitEdge e : repeatingBlockPrevNodes) {
            DagEdge backEdge = dag.addBackEdge(e.n1, firstExpressionNode, null);
            backEdge.classes.add("while");
        }
        
        prevNodes = new ArrayList<>(); // the entire while
        prevNodes.addAll(repeatingBlockPrevNodes);
        prevNodes.addAll(newScope.breakEdges);
        
        // add an edge from the top of the while as well, as it's possible the repeatingBlock may never execute
        ExitEdge e = new ExitEdge();
        e.n1 = whileNode;
        // e.label = "N";
        e.classes.add("while");
        e.classes.add("false");
        prevNodes.add(e);
        
        return prevNodes;
           
    }
    
    private List<ExitEdge> addDoEdges(Dag dag, DagNode doNode, LexicalScope scope) {
        DoStatement ds = (DoStatement) doNode.astNode;
        DagNode repeatingBlock = getDagChild(doNode.children, ds.getBody(), null);
        DagNode exprDag = getDagChild(doNode.children, ds.getExpression(), null);
        
        // move doStatement node after the repeatingblock & expression nodes
        Rejigger rejigger = hoistNode(dag, doNode, repeatingBlock);

        // continue is now a forward edge, and it's a forward edge to something we haven't
        // edged yet (the exprDag). so we should probably collect these edges in the scope like we do for 
        // break and throw edges.
        LexicalScope newScope = scope.newBreakContinueScope(doNode); 
        
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, newScope); // new continue node
        for (ExitEdge e : repeatingBlockPrevNodes) {
            // DagEdge backEdge = dag.addBackEdge(e.n1, doNode, null);
            // backEdge.classes.add("do");
            dag.addEdge(e.n1, exprDag, null);
        }

        Rejigger markedExpression = markNode(dag, exprDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, exprDag, scope);
        markedExpression.unmarkNode(dag);

        DagNode firstExpressionNode = markedExpression.inEdge.n2;
        
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        DagEdge doFalse = prevNodes.get(0);
        doFalse.classes.add("do");
        doFalse.classes.add("false");
        
        DagEdge doTrue = dag.addBackEdge(doNode, repeatingBlock, null);
        doTrue.classes.add("do");
        doTrue.classes.add("true");
        
        for (DagEdge ce : newScope.continueEdges) {
            ce.n2 = firstExpressionNode;
            dag.addEdge(ce);
        }
        
        // prevNodes.addAll(repeatingBlockPrevNodes);
        prevNodes.addAll(newScope.breakEdges);
        return prevNodes;
           
    }
    

           
    
    // ye olde switch, not whatever they're doing in java 16 these days
    // actually those might be here as well, just with switchLabeledRules and multiple expressions
    private List<ExitEdge> addSwitchEdges(Dag dag, DagNode switchNode, LexicalScope scope) {
     // draw the edges
        SwitchStatement ss = (SwitchStatement) switchNode.astNode;
        DagNode exprDag = getDagChild(switchNode.children, ss.getExpression(), null);
        
        // the statements of a switchNode interleaves SwitchCases, statements, and break statements
        List<DagNode> statementDags = getDagChildren(switchNode.children, ss.statements(), null);

        boolean centralSwitch = "true".equals(switchNode.options.get("centralSwitch"));
        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire switch
        List<ExitEdge> casePrevNodes = new ArrayList<>(); // a single case in the switch
        boolean hasDefaultCase = false;
        LexicalScope newScope = null;
        
        if (centralSwitch) {
            // hoist switchNode after the expression nodes
            Rejigger rejigger = hoistNode(dag, switchNode, exprDag);
            List<ExitEdge> ee = addExpressionEdges(dag, exprDag,  scope);
            rejigger.unhoistNode(dag,  ee); // create exit edges below
            switchNode.classes.add("centralSwitch");
            
            for (DagNode c : statementDags) {
                if (c.type.equals("SwitchCase")) {
                    SwitchCase sc = (SwitchCase) c.astNode;
                    // if apiLevel >= AST.JLS14_INTERNAL
                    // List<DagNode> exprDags = getDagChildren(c.children, sc.expressions(), null);
                    List<DagNode> exprDags = new ArrayList<>();
                    if (sc.getExpression() != null) {
                        exprDags = Collections.singletonList(getDagChild(c.children, sc.getExpression(), null));
                    };
                    boolean isDefaultCase = ((SwitchCase) c.astNode).getExpression() == null;
                    c.classes.add("centralSwitch");
                    
                    // @TODO switchLabeledRule
                    
                    // close off last case
                    if (newScope != null) {
                        prevNodes.addAll(newScope.breakEdges);
                    }
                    // this starts a new 'break' scope but 'continue's continue to do whatever continues did before.
                    newScope = scope.newBreakScope();
                    
                    List<ExitEdge> exprPrevNodes = new ArrayList<>();
                    
                    if (exprDags.size() > 0) {
                        for (DagNode caseExprDag : exprDags) {
                            DagEdge exprEdge = dag.addEdge(switchNode, caseExprDag);
                            exprEdge.classes.add("switchCase");
                            exprPrevNodes = new ArrayList<>(addExpressionEdges(dag, caseExprDag, newScope));
                            // @TODO link these up with || nodes
                        }
                    }
                    if (isDefaultCase) {
                        ExitEdge e = new ExitEdge();
                        e.n1 = switchNode;
                        e.classes.add("switchCase");
                        e.classes.add("default");
                        hasDefaultCase = true;
                        exprPrevNodes.add(e);
                    }
                    
                    for (ExitEdge e : exprPrevNodes) {
                        e.n2 = c;
                        dag.addEdge(e);
                    }
                    
                    for (ExitEdge e : casePrevNodes) { // fall-through edges. maybe these should be red instead of the break edges
                        e.n2 = c;
                        dag.addEdge(e);
                        e.classes.add("switch");
                        e.classes.add("fallthrough");
                    }
                    casePrevNodes = new ArrayList<>();
                    
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
            
        } else {
                
            // hoist switchNode after the expression nodes
            Rejigger rejigger = hoistNode(dag, switchNode, exprDag);
            List<ExitEdge> ee = addExpressionEdges(dag, exprDag,  scope);
            List<ExitEdge> noCasePrevNodes = rejigger.unhoistNode(dag,  ee); // create exit edges below
            
            
            for (DagNode c : statementDags) {
                if (c.type.equals("SwitchCase")) {
                    SwitchCase sc = (SwitchCase) c.astNode;
                    // if apiLevel >= AST.JLS14_INTERNAL
                    // List<DagNode> exprDags = getDagChildren(c.children, sc.expressions(), null);
                    List<DagNode> exprDags = new ArrayList<>();
                    if (sc.getExpression() != null) {
                        exprDags = Collections.singletonList(getDagChild(c.children, sc.getExpression(), null));
                    };
                    boolean isDefaultCase = ((SwitchCase) c.astNode).getExpression() == null;
                    // @TODO switchLabeledRule
                    
                    // close off last case
                    if (newScope != null) {
                        prevNodes.addAll(newScope.breakEdges);
                    }
                    // this starts a new 'break' scope but 'continue's continue to do whatever continues did before.
                    newScope = scope.newBreakScope();
                    if (exprDags.size() > 0) {
                        for (DagNode caseExprDag : exprDags) {
                            for (ExitEdge e : noCasePrevNodes) {
                                e.n2 = caseExprDag;
                                dag.addEdge(e);
                            }
                            noCasePrevNodes = new ArrayList<>(addExpressionEdges(dag, caseExprDag, newScope));
                            // @TODO link these up with || nodes
                        }
                    }
                    for (DagEdge e : noCasePrevNodes) {
                        e.n2 = c;
                        dag.addEdge(e);
                    }
                    for (ExitEdge e : casePrevNodes) { // fall-through edges from previous case
                        e.classes.add("switch");
                        e.classes.add("fallthrough");
                    }
                    ExitEdge ce = new ExitEdge();
                    ce.n1 = c;
                    ce.classes.add("switchCase");
                    ce.classes.add("true");
                    casePrevNodes.add(ce);
                    
                    
                    ExitEdge noCe = new ExitEdge();
                    noCe.n1 = c;
                    noCe.classes.add("switchCase");
                    noCe.classes.add("false");
                    noCasePrevNodes = new ArrayList<>();
                    noCasePrevNodes.add(noCe);
                    
                } else {
                    // any other statement is linked to the previous one, similar to a BlockStatement
                    if (casePrevNodes.size() == 0) {
                        logger.warn("no edges leading to statement in case. Maybe statement after a breakStatement ? ");
                    }
                    for (ExitEdge e : casePrevNodes) {
                        e.n2 = c;
                        dag.addEdge(e);
                    }
                    casePrevNodes = new ArrayList<>(addEdges(dag, c, newScope));
                }
            }
            // @TODO add noCasePrevNodes to casePrevNodes here ?
            
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
    
    private List<ExitEdge> addVariableDeclarationStatementEdges(Dag dag, DagNode node, LexicalScope scope) {
        // modifiers, type, varDecFragments
        VariableDeclarationStatement vs = (VariableDeclarationStatement) node.astNode;
        List<DagNode> fragments = getDagChildren(node.children, vs.fragments(), null);
        
        node.gvAttributes.put("type", vs.getType().toString());
        
        ExitEdge e = new ExitEdge();
        e.n1 = node;
        List<ExitEdge> prevNodes = Collections.singletonList(e);
        for (DagNode f : fragments) {
            for (ExitEdge pe : prevNodes) {
                pe.n2 = f;
                dag.edges.add(pe);
            }
            
            // name, dimension(s), expression
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) f.astNode;
            DagNode expressionDag = getDagChild(f.children, vdf.getInitializer(), null);
            if (expressionDag != null) {
                Rejigger rejigger = hoistNode(dag, f, expressionDag);
                prevNodes = addExpressionEdges(dag, expressionDag, scope);
                prevNodes = rejigger.unhoistNode(dag, prevNodes);
            }
            
            f.gvAttributes.put("type", vs.getType().toString());
            f.gvAttributes.put("variableName", vdf.getName().toString());
        }
        
        return prevNodes;
    }

    private List<ExitEdge> addSingleVariableDeclarationEdges(Dag dag, DagNode node, LexicalScope scope) {
       // modifiers, type, dimensions
        SingleVariableDeclaration svd = (SingleVariableDeclaration) node.astNode;
        
        node.gvAttributes.put("type", svd.getType().toString());
        node.gvAttributes.put("variableName", svd.getName().toString());
        
        ExitEdge e = new ExitEdge();
        e.n1 = node;
        List<ExitEdge> prevNodes = Collections.singletonList(e);
        return prevNodes;
        
    }
    

    // this will probably go into the ExpressionEdger later
    // so what we should probably be doing is converting this into the RPN sequence of evaluations
    // or maybe that's RRPN.
    // so c = a + b becomes
    // evaluate a -> evaluate b -> addition -> assignment
    
    
    public List<ExitEdge> addExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        if (node.type.equals("MethodInvocation")) {
            return addMethodInvocationEdges(dag, node, scope);

        } else if (node.type.equals("SuperMethodInvocation")) {
            return addSuperMethodInvocationEdges(dag, node, scope);

        } else if (node.type.equals("ConditionalExpression")) {
            return addConditionalExpressionEdges(dag, node, scope);

        } else if (node.type.equals("SimpleName") ||
            node.type.equals("QualifiedName")) {
            return addNameEdges(dag, node, scope);

        } else if (node.type.equals("ThisExpression")) {
            return addThisExpressionEdges(dag, node, scope);
            
        } else if (node.type.equals("BooleanLiteral") ||
            node.type.equals("CharacterLiteral") || 
            node.type.equals("NumberLiteral") ||
            node.type.equals("NullLiteral") ||
            node.type.equals("TypeLiteral") || 
            node.type.equals("StringLiteral")) {
            return addLiteralExpressionEdges(dag, node, scope);

        } else if (node.type.equals("ParenthesizedExpression")) {
            return addParenthesizedExpressionEdges(dag, node, scope);

        } else if (node.type.equals("PrefixExpression")) {
            return addPrefixExpressionEdges(dag, node, scope);

        } else if (node.type.equals("PostfixExpression")) {
            return addPostfixExpressionEdges(dag, node, scope);
            
        } else if (node.type.equals("InfixExpression")) {
            return addInfixExpressionEdges(dag, node, scope);

        } else if (node.type.equals("CastExpression")) {
            return addCastExpressionEdges(dag, node, scope);

        } else if (node.type.equals("InstanceofExpression")) {
            return addInstanceofExpressionEdges(dag, node, scope);
            
        } else if (node.type.equals("Assignment")) {
            return addAssignmentEdges(dag, node, scope);

        } else if (node.type.equals("FieldAccess")) {
            return addFieldAccessEdges(dag, node, scope);

        } else if (node.type.equals("ArrayAccess")) {
            return addArrayAccessEdges(dag, node, scope);

        } else if (node.type.equals("SuperFieldAccess")) {
            return addSuperFieldAccessEdges(dag, node, scope);

        } else if (node.type.equals("CreationReference") ||
            node.type.equals("ExpressionMethodReference") ||
            node.type.equals("SuperMethodReference") ||
            node.type.equals("TypeMethodReference")) {
            return addMethodReferenceEdges(dag, node, scope);

        } else if (node.type.equals("ArrayCreation")) {
            return addArrayCreationEdges(dag, node, scope);
        } else if (node.type.equals("ArrayInitializer")) {
            return addArrayInitializerEdges(dag, node, scope);
        } else if (node.type.equals("ClassInstanceCreation")) {
            return addClassInstanceCreationEdges(dag, node, scope);

        } else if (node.type.equals("VariableDeclarationExpression")) {
            return addVariableDeclarationExpressionEdges(dag, node, scope);
            
            
        } else if (node.type.equals("LambdaExpression")) {
            return addLambdaExpressionEdges(dag, node, scope);
            
            
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

    private List<ExitEdge> addLambdaExpressionEdges(Dag dag, DagNode lambdaNode, LexicalScope scope) {
        // lambda expression is going to be a big like a method declaration in the middle of a method.
        // but has an exit edge 
    
        LambdaExpression le = (LambdaExpression) lambdaNode.astNode;
        // method.label = "method " + md.getName();
        // method.gvAttributes.put("methodName",  md.getName().toString());
        
        // we don't create nodes for method parameters yet, so not going to do that for lambdas either
        // (maybe we should ? )
        
        // when a lambda is defined control flow doesn't pass to the block, 
        // so maybe I skip all of that somehow. OK so let's create an edge so it's grouped together but hide it in the diagram.
        
        DagNode blockNode = lambdaNode.children.get(lambdaNode.children.size() - 1);
        DagEdge lambdaEntryEdge = dag.addEdge(lambdaNode, blockNode);
        lambdaEntryEdge.classes.add("lambdaEntry");

        // @TODO these probably need a new lexical scope
        LexicalScope lexicalScope = scope.newLambdaScope(); 
        
        List<ExitEdge> ee;
        if (blockNode.type.equals("Block")) {
            ee = addBlockEdges(dag, blockNode, lexicalScope);
        } else if (blockNode.astNode instanceof Expression) {
            ee = addExpressionEdges(dag, blockNode, lexicalScope);
        } else {
            throw new IllegalStateException("expected Block or Expression in lambda");
        }

        // add a node which all the return edges return to
        // this is an artificial node so maybe only construct it based on some gv declaration earlier on ?
        // (whereas all the other nodes are about as concrete as anything else in IT)
        
        // CompilationUnit cu = methodBlock.astNode.getParent();
        CompilationUnit cu = ASTResolving.findParentCompilationUnit(le);
        int endOfLambdaLine = cu.getLineNumber(le.getStartPosition() + le.getLength());
        DagNode returnNode = new DagNode();
        returnNode.keepNode = lambdaNode.keepNodeMatcher.matches("lambdaExpressionEnd");
        returnNode.type = "lambdaExpressionEnd"; // label this 'end' if it has no return value ?
        returnNode.lineNumber = endOfLambdaLine;
        // rn.name = dag.getUniqueName("m_" + endOfMethodLine);
        returnNode.classes.add("lambdaExpression");
        returnNode.classes.add("end");
        // rn.label = "return";
        returnNode.astNode = null;
        lambdaNode.children.add(returnNode); // include the artificial return inside the lambda grouping
        
        DagSubgraph sg = dag.dagNodeToSubgraph.get(lambdaNode);
        dag.addNode(sg, returnNode);
        
        for (ExitEdge e : lexicalScope.returnEdges) {
            e.n2 = returnNode;
            dag.addEdge(e);
        }
        for (ExitEdge e : ee) {
            e.n2 = returnNode;
            dag.addEdge(e);
        }
        
        // and everything that was thrown connects to this node as well
        if (includeThrowEdges) { 
            for (ExitEdge e : lexicalScope.throwEdges) {
                e.n2 = returnNode;
                dag.addEdge(e);
            }
        }
        
        // there's no exit edges out of a method, but let's say there is from a lambda
        // (it's not a real edge, it gets truncated to the subgraph boundary in some css)
        ExitEdge e = new ExitEdge();
        e.n1 = returnNode; 
        return Collections.singletonList(e);
        
    }
    
    
    private List<ExitEdge> addLiteralExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        String literalValue = null;
        switch (node.type) {
            case "BooleanLiteral": 
                literalValue = String.valueOf( ((BooleanLiteral) node.astNode).booleanValue() );
                break;
            case "NumberLiteral": 
                literalValue = ((NumberLiteral) node.astNode).getToken();
                break;
            case "CharacterLiteral": 
                literalValue = ((CharacterLiteral) node.astNode).getEscapedValue();
                break;
            case "NullLiteral": 
                literalValue = "null";
                break;
            case "TypeLiteral": 
                Type t = ((TypeLiteral) node.astNode).getType();
                literalValue = t == null ? "void" : t.toString() + ".class";
                break;
            case "StringLiteral": 
                literalValue = ((StringLiteral) node.astNode).getEscapedValue();
                break;
            default:
                throw new IllegalStateException("unknown literal " + node.type);
        }
        node.gvAttributes.put("literalValue", literalValue);
        node.classes.add("literal"); // not gv-literal mind you. that's a different kind of literal.
        
        ExitEdge e = new ExitEdge();
        e.n1 = node;
        return Collections.singletonList(e);
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
    
    
    /** A rejigger is a hoisted node, which has been removed from the dag with the intention of adding it back in again
     * a bit further down ( unhoisting ). This class is mostly necessary since I don't trust the edge collections in the DagNodes
     * whilst things are moving around, or even after they've moved around for that matter.
     * 
     */
    public static class Rejigger {
        DagNode hoistedNode;
        List<DagEdge> inEdges;  // old in edges
        EntryEdge inEdge;       // new in edge
        
        public List<ExitEdge> unhoistNode(Dag dag, List<ExitEdge> prevNodes) {
            // inEdge may have been rejiggered as well though, so the firstNode may no longer be the firstNode
            for (DagEdge e : this.inEdges) {
                e.n2 = this.inEdge.n2;  // this is so going to work.  t// firstNode; // @TODO probably some other structures to change here
            }
            dag.edges.remove(this.inEdge);
            if (prevNodes != null) {
                for (DagEdge e : prevNodes) {
                    e.n2 = hoistedNode;
                    dag.edges.add(e);
                }
            }
            
            ExitEdge e = new ExitEdge();
            e.n1 = hoistedNode;
            prevNodes = new ArrayList<>();
            prevNodes.add(e);
            return prevNodes;
        }
        
        
        public void unmarkNode(Dag dag) {
            dag.edges.remove(this.inEdge);
        }
    }
    
    private Rejigger hoistNode(Dag dag, DagNode node, DagNode newNode) {
        // move methodInvocation node after the expression & argument nodes
        List<DagEdge> inEdges = new ArrayList<>();
        for (DagEdge e : dag.edges) { if ( e.n2 == node ) { inEdges.add(e); } }
        if (inEdges.size() == 0) { 
            // throw new IllegalStateException("no inEdges"); // this can happen if a new graph is created inside a method
            
        }

        EntryEdge inEdge = new EntryEdge();
        inEdge.n2 = newNode;
        dag.edges.add(inEdge);
        
        Rejigger rejigger = new Rejigger();
        rejigger.hoistedNode = node;
        rejigger.inEdges = inEdges;
        rejigger.inEdge = inEdge;
        
        // first node is inEdge.n2
        return rejigger;
    }
    
    // mark a node which is not yet in the dag, which may be rejiggered
    // so we can find the first node again later
    private Rejigger markNode(Dag dag, DagNode node) {
        EntryEdge inEdge = new EntryEdge();
        inEdge.n2 = node;
        dag.edges.add(inEdge);
        
        Rejigger rejigger = new Rejigger();
        rejigger.hoistedNode = node;
        rejigger.inEdge = inEdge;
        
        // first node is inEdge.n2
        return rejigger;
    }

    private List<ExitEdge> addConstructorInvocationEdges(Dag dag, DagNode constructorInvocationNode, LexicalScope scope) {
        ConstructorInvocation ci = (ConstructorInvocation) constructorInvocationNode.astNode;
        // type arguments
        constructorInvocationNode.gvAttributes.put("methodName", "this");
        List<DagNode> argumentDags = getDagChildren(constructorInvocationNode.children, ci.arguments(), null);

        List<ExitEdge> prevNodes = null;
        if (argumentDags.size() > 0) {
            // move methodInvocation node after the argument nodes
            Rejigger rejigger = hoistNode(dag, constructorInvocationNode, argumentDags.get(0));
            // expression is null for method calls within the same object
            for (DagNode a : argumentDags) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = a;
                        e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, a, scope );
            }
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
        } else {
            ExitEdge e = new ExitEdge();
            e.n1 = constructorInvocationNode;
            prevNodes = Collections.singletonList(e);
        }
        return prevNodes;
    }
    
    private List<ExitEdge> addSuperConstructorInvocationEdges(Dag dag, DagNode superConstructorInvocationNode, LexicalScope scope) {
        SuperConstructorInvocation mi = (SuperConstructorInvocation) superConstructorInvocationNode.astNode;
        DagNode expressionDag = getDagChild(superConstructorInvocationNode.children, mi.getExpression(), null);
        superConstructorInvocationNode.gvAttributes.put("methodName", "super");
        List<DagNode> argumentDags = getDagChildren(superConstructorInvocationNode.children, mi.arguments(), null);

        List<ExitEdge> prevNodes = null;
        if (expressionDag != null || argumentDags.size() > 0) {
            // move methodInvocation node after the expression & argument nodes
            Rejigger rejigger = hoistNode(dag, superConstructorInvocationNode, expressionDag != null ? expressionDag : argumentDags.get(0));
            // expression is null for method calls within the same object
            if (expressionDag != null) {
                prevNodes = addExpressionEdges(dag, expressionDag, scope);
            }
            for (DagNode a : argumentDags) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = a;
                        e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, a, scope );
            }
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
        } else {
            ExitEdge e = new ExitEdge();
            e.n1 = superConstructorInvocationNode;
            prevNodes = Collections.singletonList(e);
        }
        return prevNodes;
    }  
    
    
    private List<ExitEdge> addMethodInvocationEdges(Dag dag, DagNode methodInvocationNode, LexicalScope scope) {
        MethodInvocation mi = (MethodInvocation) methodInvocationNode.astNode;
        DagNode expressionDag = getDagChild(methodInvocationNode.children, mi.getExpression(), null);
        methodInvocationNode.gvAttributes.put("methodName", mi.getName().toString());
        List<DagNode> argumentDags = getDagChildren(methodInvocationNode.children, mi.arguments(), null);

        List<ExitEdge> prevNodes = null;
        if (expressionDag != null || argumentDags.size() > 0) {
            // move methodInvocation node after the expression & argument nodes
            Rejigger rejigger = hoistNode(dag, methodInvocationNode, expressionDag != null ? expressionDag : argumentDags.get(0));
            // expression is null for method calls within the same object
            if (expressionDag != null) {
                prevNodes = addExpressionEdges(dag, expressionDag, scope);
            }
            for (DagNode a : argumentDags) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = a;
                        e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, a, scope );
            }
            
            // what's the opposite of rejiggering ?
            // hoisting ? petarding ? right I'm just going to say unhoist
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
        } else {
            ExitEdge e = new ExitEdge();
            e.n1 = methodInvocationNode;
            prevNodes = Collections.singletonList(e);
        }
        return prevNodes;
    }   
    

    private List<ExitEdge> addSuperMethodInvocationEdges(Dag dag, DagNode methodInvocationNode, LexicalScope scope) {
        SuperMethodInvocation smi = (SuperMethodInvocation) methodInvocationNode.astNode;
        
        String qualifier = smi.getQualifier() == null ? "" : smi.getQualifier().toString() + ".";
        // smi.typeArguments(); ?
        methodInvocationNode.gvAttributes.put("methodName", qualifier + ".super." + smi.getName().toString());
        List<DagNode> argumentDags = getDagChildren(methodInvocationNode.children, smi.arguments(), null);

        List<ExitEdge> prevNodes = null;
        if (argumentDags.size() > 0) {
            // move methodInvocation node after the argument nodes
            Rejigger rejigger = hoistNode(dag, methodInvocationNode, argumentDags.get(0));
            for (DagNode a : argumentDags) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = a;
                        e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, a, scope );
            }
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
        } else {
            ExitEdge e = new ExitEdge();
            e.n1 = methodInvocationNode;
            prevNodes = Collections.singletonList(e);
        }
        return prevNodes;
    }
    
    
    
    

    private List<ExitEdge> addInfixExpressionEdges(Dag dag, DagNode infixNode, LexicalScope scope) {
        InfixExpression ie = (InfixExpression) infixNode.astNode;
        DagNode leftDag = getDagChild(infixNode.children, ie.getLeftOperand(), null);
        DagNode rightDag = getDagChild(infixNode.children, ie.getRightOperand(), null);
        List<DagNode> extendedDags = getDagChildren(infixNode.children, ie.extendedOperands(), null);
        
        // Operator is a class, not an enum (!)
        Operator op = ie.getOperator();
        infixNode.gvAttributes.put("operatorToken", op.toString());
        infixNode.gvAttributes.put("operatorName", Text.getLastComponent(op.getClass().getName())); // @TODO camelcase
        
        // a + b      becomes a -> b -> +
        // a + b + c  should becomes a -> b -> + -> c -> + , which has two + nodes, even though there's only one in the AST. because you know. eclipse.
        
        
        // move infixExpression node after the a and b nodes
        Rejigger rejigger = hoistNode(dag, infixNode, leftDag);
        List<ExitEdge> prevNodes = null;
        if (op == Operator.CONDITIONAL_AND || 
            op == Operator.CONDITIONAL_OR) {

            // InfixExpressions also include shortcut || which doesn't evaluate the second parameter if the first is true
            // so it should be a something a bit more complicated, control-flow wise
            
            // a 
            // true? -N->  b
            //  :Y         :   
            //  v          v          
            infixNode.classes.add("infixConditional");
            prevNodes = addExpressionEdges(dag, leftDag, scope);

            
            prevNodes = rejigger.unhoistNode(dag,  prevNodes);

            // graphviz diagram is a bit mong unless we swap the false and true edge orders. maybe.
            ExitEdge trueEdge = prevNodes.get(0); 
            trueEdge.classes.add("infixConditional");
            trueEdge.classes.add(op == Operator.CONDITIONAL_OR ? "true" : "false");
            
            DagEdge falseEdge = dag.addEdge(infixNode, rightDag);
            falseEdge.classes.add("infixConditional"); // well this is the non-shortcut branch, but hey
            falseEdge.classes.add(op == Operator.CONDITIONAL_OR ? "false" : "true");
            List<ExitEdge> lastPrevNodes = addExpressionEdges(dag, rightDag, scope);
            
            for (int i=0; i< extendedDags.size(); i++) {
                // actually probably need to add a new node here
                DagNode n = extendedDags.get(i);
                DagNode extInfixNode = new DagNode();
                extInfixNode.keepNode = infixNode.keepNodeMatcher.matches("infixExpressionCondition");
                extInfixNode.type = "infixExpressionCondition"; // even though it isn't
                extInfixNode.lineNumber = n.lineNumber; // even though it isn't
                extInfixNode.classes.add("infixExpression");
                extInfixNode.classes.add("infixConditional");
                extInfixNode.astNode = null;
                extInfixNode.gvAttributes.put("operatorToken", op.toString());
                extInfixNode.gvAttributes.put("operatorName", Text.getLastComponent(op.getClass().getName())); // @TODO camelcase
                DagSubgraph sg = dag.dagNodeToSubgraph.get(infixNode);
                dag.addNode(sg, extInfixNode);
                // needs to be a child of ieNode as well so it's moved to subgraphs when that node moves
                infixNode.children.add(extInfixNode);
                
                for (ExitEdge e : lastPrevNodes) {
                    e.n2 = extInfixNode;
                    dag.edges.add(e);
                }

                trueEdge = new ExitEdge();
                trueEdge.n1 = extInfixNode;
                trueEdge.classes.add("infixConditional");
                trueEdge.classes.add(op == Operator.CONDITIONAL_OR ? "true" : "false");
                prevNodes.add(0, trueEdge);

                falseEdge = dag.addEdge(extInfixNode, n);
                falseEdge.classes.add("infixConditional"); // well this is the non-shortcut branch, but hey
                falseEdge.classes.add(op == Operator.CONDITIONAL_OR ? "false" : "true");
                lastPrevNodes = addExpressionEdges(dag, n, scope);
                
            }
            
            prevNodes.addAll(lastPrevNodes);
            
        } else {
            // non-shortcut e.g. +, just evaluate in order 
            for (DagNode a : infixNode.children) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = a;
                        dag.addEdge(e);
                    }
                } 
                prevNodes = addExpressionEdges(dag, a, scope );
            }
            
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
            
        }
        return prevNodes;
    }

        
    private List<ExitEdge> addConditionalExpressionEdges(Dag dag, DagNode ceNode, LexicalScope scope) {
        ConditionalExpression ce = (ConditionalExpression) ceNode.astNode;
        
        DagNode expressionDag = getDagChild(ceNode.children, ce.getExpression(), null);
        DagNode thenDag = getDagChild(ceNode.children, ce.getThenExpression(), null);
        DagNode elseDag = getDagChild(ceNode.children, ce.getElseExpression(), null);
        
        Rejigger rejigger = hoistNode(dag, ceNode, expressionDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, expressionDag, scope);
        prevNodes = rejigger.unhoistNode(dag, prevNodes); // discard prevNodes I guess
        prevNodes = new ArrayList<>();
        
        // dag.addEdge(ceNode, expressionDag);
        /*
        List<ExitEdge> ee = addExpressionEdges(dag, expressionDag, scope);
        for (DagEdge e : ee) {
            e.n2 = ceNode;
        }
        */
                
        // 
        // DagEdge trueEdge = prevNodes.get(0); trueEdge.n2 = thenDag;
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
        
        ExitEdge e = new ExitEdge();
        e.n1 = nameNode;
        return Collections.singletonList(e);
    }
    
    private List<ExitEdge> addThisExpressionEdges(Dag dag, DagNode thisNode, LexicalScope scope) {
        ThisExpression n = (ThisExpression) thisNode.astNode; // SimpleName or QualifiedName
        thisNode.gvAttributes.put("name", n.toString()); // qualified this
        
        ExitEdge e = new ExitEdge();
        e.n1 = thisNode;
        return Collections.singletonList(e);
    }

    private List<ExitEdge> addMethodReferenceEdges(Dag dag, DagNode thisNode, LexicalScope scope) {
        // CreationReference or ExpressionMethodReference or SuperMethodReference or TypeMethodReference
        MethodReference mr = (MethodReference) thisNode.astNode; 
        thisNode.gvAttributes.put("name", mr.toString()); // qualified reference
        
        ExitEdge e = new ExitEdge();
        e.n1 = thisNode;
        return Collections.singletonList(e);
    }

    
    
    
    
    private List<ExitEdge> addParenthesizedExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        ParenthesizedExpression pe = (ParenthesizedExpression) node.astNode;
        DagNode exprDag = getDagChild(node.children, pe.getExpression(), null);
        
        // don't really need these in the dag
        Rejigger rejigger = hoistNode(dag, node, exprDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, exprDag, scope);
        rejigger.unhoistNode(dag, null);
        
        return prevNodes;
    }
    
    
    private List<ExitEdge> addPostfixExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        PostfixExpression pe = (PostfixExpression) node.astNode;

        DagNode operandDag = getDagChild(node.children, pe.getOperand(), null);
        PostfixExpression.Operator op = pe.getOperator();
        node.gvAttributes.put("operatorToken", op.toString());
        node.gvAttributes.put("operatorName", Text.getLastComponent(op.getClass().getName())); // @TODO camelcase
        
        Rejigger rejigger = hoistNode(dag, node, operandDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, operandDag, scope);
    
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        return prevNodes;
    }
    
    private List<ExitEdge> addPrefixExpressionEdges(Dag dag2, DagNode node, LexicalScope scope) {
        PrefixExpression pe = (PrefixExpression) node.astNode;
        DagNode operandDag = getDagChild(node.children, pe.getOperand(), null);
        PrefixExpression.Operator op = pe.getOperator();
        node.gvAttributes.put("operatorToken", op.toString());
        node.gvAttributes.put("operatorName", Text.getLastComponent(op.getClass().getName())); // @TODO camelcase

        Rejigger rejigger = hoistNode(dag, node, operandDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, operandDag, scope);
    
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        return prevNodes;
    }

    private List<ExitEdge> addCastExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        CastExpression ce = (CastExpression) node.astNode;
        DagNode expressionDag = getDagChild(node.children, ce.getExpression(), null);
        DagNode typeDag = getDagChild(node.children, ce.getType(), null);
        node.gvAttributes.put("type", ce.getType().toString());
        
        Rejigger rejigger = hoistNode(dag, node, expressionDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, expressionDag, scope);
        
        
        /*
        if (typeDag != null) { // types have Names, which are also expressions, but they're inside the ast node so typeDag is null 
            for (DagEdge e : prevNodes) {
                e.n2 = typeDag;
            }
            ExitEdge e = new ExitEdge();
            e.n1 = typeDag;
            prevNodes = Collections.singletonList(e);
        }
        */
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        return prevNodes;
    }

    
    private List<ExitEdge> addInstanceofExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        InstanceofExpression ioe = (InstanceofExpression) node.astNode;
        DagNode expressionDag = getDagChild(node.children, ioe.getLeftOperand(), null);
        DagNode typeDag = getDagChild(node.children, ioe.getRightOperand(), null);
        node.gvAttributes.put("type", ioe.getRightOperand().toString());
        
        Rejigger rejigger = hoistNode(dag, node, expressionDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, expressionDag, scope);
        
        /*
        if (typeDag != null) { // types have Names, which are also expressions, but they're inside the ast node so typeDag is null 
            for (DagEdge e : prevNodes) {
                e.n2 = typeDag;
            }
            ExitEdge e = new ExitEdge();
            e.n1 = typeDag;
            prevNodes = Collections.singletonList(e);
        }
        */
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        return prevNodes;
    }
    
    private List<ExitEdge> addAssignmentEdges(Dag dag, DagNode node, LexicalScope scope) {
        Assignment a = (Assignment) node.astNode;
        // includes things like += as well
        DagNode lhsDag = getDagChild(node.children, a.getLeftHandSide(), null);
        DagNode rhsDag = getDagChild(node.children, a.getRightHandSide(), null);
        Assignment.Operator op = a.getOperator();
        node.gvAttributes.put("operatorToken", op.toString());
        node.gvAttributes.put("operatorName", Text.getLastComponent(op.getClass().getName())); // @TODO camelcase
        
        Rejigger rejigger = hoistNode(dag, node, rhsDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, rhsDag, scope);
        for (DagEdge e : prevNodes) {
            e.n2 = lhsDag;
            dag.edges.add(e);
        }
        
        prevNodes = addExpressionEdges(dag, lhsDag, scope);
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        return prevNodes;
    }

    // QualifiedNames e.g. "a.b.c" can also be represented as FieldAccess chains
    // "this.i"  becomes -> this -> field i
    private List<ExitEdge> addFieldAccessEdges(Dag dag, DagNode node, LexicalScope scope) {
        FieldAccess fa = (FieldAccess) node.astNode;
        // includes things like += as well
        DagNode exprDag = getDagChild(node.children, fa.getExpression(), null);
        // DagNode fieldDag = getDagChild(node.children, fa.getName(), null); // will put the name on the FA node
        
        node.gvAttributes.put("fieldName", fa.getName().toString()); 
        Rejigger rejigger = hoistNode(dag, node, exprDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, exprDag, scope);
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        return prevNodes;
    }
    
    private List<ExitEdge> addSuperFieldAccessEdges(Dag dag, DagNode node, LexicalScope scope) {
        SuperFieldAccess fa = (SuperFieldAccess) node.astNode;
        // DagNode fieldDag = getDagChild(node.children, fa.getName(), null); // will put the name on the FA node
        
        node.gvAttributes.put("fieldName", fa.getName().toString()); // qualified super field
        
        ExitEdge e = new ExitEdge();
        e.n1 = node;
        return Collections.singletonList(e);
    }
    
    private List<ExitEdge> addArrayAccessEdges(Dag dag, DagNode node, LexicalScope scope) {
        ArrayAccess fa = (ArrayAccess) node.astNode;

        // maybe we evaluate the index first ? not sure. reckon it's probably the array ref
        DagNode arrayDag = getDagChild(node.children, fa.getArray(), null);
        DagNode indexDag = getDagChild(node.children, fa.getIndex(), null); 
        // DagNode fieldDag = getDagChild(node.children, fa.getName(), null); // will put the name on the FA node
        
        // node.gvAttributes.put("fieldName", fa.getIndex().toString()); 
        Rejigger rejigger = hoistNode(dag, node, arrayDag);
        List<ExitEdge> prevNodes = addExpressionEdges(dag, arrayDag, scope);
        dag.addEdge(arrayDag, indexDag);
        prevNodes = addExpressionEdges(dag, indexDag, scope);
        prevNodes = rejigger.unhoistNode(dag, prevNodes);
        return prevNodes;
    }    


    private List<ExitEdge> addClassInstanceCreationEdges(Dag dag, DagNode node, LexicalScope scope) {
        ClassInstanceCreation cic = (ClassInstanceCreation) node.astNode;
        
        // maybe we evaluate the index first ? not sure. reckon it's probably the array ref
        DagNode expressionDag = getDagChild(node.children, cic.getExpression(), null);
        List<DagNode> argumentDags = getDagChildren(node.children, cic.arguments(), null);
        DagNode anonClassDag = getDagChild(node.children, cic.getAnonymousClassDeclaration(), "anonymousClass");
        
        // AnonymousClassDeclaration
        
        node.gvAttributes.put("type", cic.getType().toString());
        
        // DagNode indexDag = getDagChild(node.children, fa.getIndex(), null); 
        // DagNode fieldDag = getDagChild(node.children, fa.getName(), null); // will put the name on the FA node
        
        List<ExitEdge> prevNodes = null;
        if (expressionDag != null || argumentDags.size() > 0) {
            // move methodInvocation node after the expression & argument nodes
            Rejigger rejigger = hoistNode(dag, node, expressionDag != null ? expressionDag : argumentDags.get(0));
            // expression is null for method calls within the same object
            if (expressionDag != null) {
                prevNodes = addExpressionEdges(dag, expressionDag, scope);
            }
            for (DagNode a : argumentDags) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = a;
                        e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, a, scope );
            }
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
        } else {
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            prevNodes = Collections.singletonList(e);
        }
        
        // jam the anonymous class in here as if it was a lambda, but with methods like a class
        if (anonClassDag != null) {
            for (ExitEdge e : prevNodes) {
                e.n2 = anonClassDag;
                dag.addEdge(e);
            }
            
            AnonymousClassDeclaration acdNode = (AnonymousClassDeclaration) anonClassDag.astNode;
            
            List<DagNode> bodyDeclarationDags = getDagChildren(anonClassDag.children, acdNode.bodyDeclarations(), null);

            LexicalScope lexicalScope = scope.newTypeScope(); 
            
            // add edges for everything in the class
            // @TODO check fields don't appear though. unless they initalise things, then maybe. Or maybe that goes into the constructors. 
            // or not.
            // yeesh what about the static initializers. what about them indeed.
            List<ExitEdge> ees = new ArrayList<>();
            for (DagNode n : bodyDeclarationDags) {
                // add a transparent edge to each thing defined in this class so that the 'AnonymousClassDeclaration' node appears above them
                DagEdge e = dag.addEdge(anonClassDag, n);
                e.classes.add("anonymousClassDeclarationBegin");
                ees.addAll(addEdges(dag, n, lexicalScope));
            }

            // add an artificial node so we can create an edge out of this thing
            
            CompilationUnit cu = ASTResolving.findParentCompilationUnit(acdNode);
            int endOfAnonClassLine = cu.getLineNumber(acdNode.getStartPosition() + acdNode.getLength());
            DagNode returnNode = new DagNode();
            returnNode.keepNode = anonClassDag.keepNodeMatcher.matches("anonymousClassDeclarationEnd");
            returnNode.type = "anonymousClassDeclarationEnd";
            returnNode.lineNumber = endOfAnonClassLine;
            // rn.name = dag.getUniqueName("m_" + endOfMethodLine);
            returnNode.classes.add("anonymousClassDeclaration");
            returnNode.classes.add("end");
            // rn.label = "return";
            returnNode.astNode = null;
            anonClassDag.children.add(returnNode); // include the artificial return inside the lambda grouping
            
            DagSubgraph sg = dag.dagNodeToSubgraph.get(anonClassDag);
            dag.addNode(sg, returnNode);
            
            
            for (ExitEdge ee : ees) {
                // add a transparent edge from each thing defined in this class to the artifical node
                // so that it appears underneath them
                ee.n2 = returnNode;
                ee.classes.add("anonymousClassDeclarationEnd");
                dag.addEdge(ee);
            }
            
            // there's no exit edges out of an anonymous class
            // this gets truncated to the subgraph boundary in some css
            ExitEdge e = new ExitEdge();
            e.n1 = returnNode; 
            prevNodes = Collections.singletonList(e);
        }
        
        return prevNodes;
    }
    
    
    private List<ExitEdge> addArrayInitializerEdges(Dag dag, DagNode node, LexicalScope scope) {
        ArrayInitializer ai = (ArrayInitializer) node.astNode;

        // maybe we evaluate the index first ? not sure. reckon it's probably the array ref
        List<DagNode> expressionDags = getDagChildren(node.children, ai.expressions(), null);
         
        List<ExitEdge> prevNodes = null;
        if (expressionDags.size() > 0) {
            // move methodInvocation node after the expression & argument nodes
            
            Rejigger rejigger = hoistNode(dag, node, expressionDags.get(0));
            for (DagNode expr : expressionDags) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = expr;
                        e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, expr, scope );
            }
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
        } else {
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            prevNodes = Collections.singletonList(e);
        }
        return prevNodes;
    }
    
    private List<ExitEdge> addArrayCreationEdges(Dag dag, DagNode node, LexicalScope scope) {
        ArrayCreation ac = (ArrayCreation) node.astNode;

        List<DagNode> dimensionDags = getDagChildren(node.children, ac.dimensions(), null);
        DagNode arrayInitDag = getDagChild(node.children, ac.getInitializer(), null);
        node.gvAttributes.put("type", ac.getType().toString());
        
        
        List<ExitEdge> prevNodes = null;
        if (arrayInitDag != null || dimensionDags.size() > 0) {
            // move methodInvocation node after the expression & argument nodes
            Rejigger rejigger = hoistNode(dag, node, dimensionDags.size() == 0 ? arrayInitDag : dimensionDags.get(0));
            for (DagNode a : dimensionDags) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = a;
                        // e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, a, scope );
            }
            // expression is null for method calls within the same object
            if (arrayInitDag != null) {
                if (prevNodes != null) {
                    for (ExitEdge e : prevNodes) {
                        e.n2 = arrayInitDag;
                        // e.classes.add("invocationArgument");
                        dag.addEdge(e);
                    }
                }
                prevNodes = addExpressionEdges(dag, arrayInitDag, scope);
            }
            prevNodes = rejigger.unhoistNode(dag, prevNodes);
        } else {
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            prevNodes = Collections.singletonList(e);
        }
        return prevNodes;
    }

    // same as VariableDeclaration, except it's an expression ( e.g. for loop initialisers ) 
    private List<ExitEdge> addVariableDeclarationExpressionEdges(Dag dag, DagNode node, LexicalScope scope) {
        VariableDeclarationExpression vde = (VariableDeclarationExpression) node.astNode;
        List<DagNode> fragments = getDagChildren(node.children, vde.fragments(), null);
        
        node.gvAttributes.put("type", vde.getType().toString());
        
        ExitEdge e = new ExitEdge();
        e.n1 = node;
        List<ExitEdge> prevNodes = Collections.singletonList(e);
        for (DagNode f : fragments) {
            for (ExitEdge pe : prevNodes) {
                pe.n2 = f;
                dag.edges.add(pe);
            }
            
            // name, dimension(s), expression
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) f.astNode;
            DagNode expressionDag = getDagChild(f.children, vdf.getInitializer(), null);
            if (expressionDag != null) {
                Rejigger rejigger = hoistNode(dag, f, expressionDag);
                prevNodes = addExpressionEdges(dag, expressionDag, scope);
                prevNodes = rejigger.unhoistNode(dag, prevNodes);
            }
            
            f.gvAttributes.put("type", vde.getType().toString());
            f.gvAttributes.put("variableName", vdf.getName().toString());
        }
        
        return prevNodes;
    }
    
}

