package com.randomnoun.build.javaToGraphviz;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

/** Convert an AST tree of a java class ( CompilationUnit, created by the eclipse ASTParser )
 * into a graphviz diagram ( Dag )
 * 
 * A complete standalone example of ASTParser
 * @see https://www.programcreek.com/2011/01/a-complete-standalone-example-of-astparser/
 *
 */
public class JavaToGraphviz3 {

    Logger logger = Logger.getLogger(JavaToGraphviz3.class);
    
	
	public void test() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = this.getClass().getResourceAsStream("/test.java");
		StreamUtil.copyStream(is, baos);
		is.close();
		
		String src = baos.toString();
		ASTParser parser = ASTParser.newParser(AST.JLS11); // JLS3
		parser.setSource(src.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		
		
		// https://www.programcreek.com/2013/03/get-internal-comments-by-using-eclipse-jdt-astparser/
		// hmm this seems like a weird way of doing things. anyway.
		// CommentVisitor cv = new CommentVisitor(cu, src);
		
		List<CommentText> comments = getComments(cu, src);
		
		// Comments have an alternoteRoot which links it back to the compilationUnit AST node

		// probably need to construct a DAG now don't I
		
		DagVisitor dv = new DagVisitor(cu, src, comments);
        cu.accept(dv);
        Dag dag = dv.getDag();
        DagNode block = dag.nodes.get(0);
        if (!block.type.equals("Block")) {
            throw new IllegalStateException("first node must be a block; found " + block.type);
        }
        addBlockEdges(dag, block);
        block.keepNode = true;
        setLastKeepNode(dag, block, block);
        removeNodes(dag, block); // peephole node removal.
        
        
        
        PrintWriter pw = new PrintWriter(System.out);
        pw.println("digraph G {\r\n" +
            "  graph [fontname = \"Handlee\"];\r\n" +
            "  node [fontname = \"Handlee\"; shape=rect; ];\r\n" +
            "  edge [fontname = \"Handlee\"];\r\n" +
            "\r\n" +
            "  bgcolor=transparent;");
        for (DagNode node : dag.nodes) {
            // only draw nodes if they have an edge
            boolean hasEdge = false;
            for (DagEdge e : dag.edges) {
                if (e.n1 == node || e.n2 == node) { hasEdge = true; break; }
            }
            if (hasEdge) {
                pw.println(node.toGraphviz());
            }
        }
        for (DagEdge edge : dag.edges) {
            pw.println(edge.toGraphviz());
        }
        pw.println("}");
        
        pw.flush();
	}
	
	// returns exit nodes
	private List<ExitEdge> addEdges(Dag dag, DagNode node) {
	    if (node.type.equals("Block")) {
	        return addBlockEdges(dag, node);
	    } else if (node.type.equals("If")) {
	        return addIfEdges(dag, node);
        } else if (node.type.equals("Try")) {
            return addTryEdges(dag, node);
            
        // @TODO
        // Break
        // Continue
        // Do
        // EnhancedFor
        // For
        // Return
        // SwitchCase
        // Switch
        // Throw
        // While
            
        } else if (node.type.equals("Assert") ||
          node.type.equals("ConstructorInvocation") ||
          node.type.equals("EmptyStatement") ||
          node.type.equals("Expression") ||
          node.type.equals("LabeledStatement") ||
          node.type.equals("SuperConstructorInvocation") ||
          node.type.equals("Synchronized") || // could be same as Block really
          node.type.equals("TypeDeclaration") ||
          node.type.equals("VariableDeclaration") ||
          node.type.equals("comment")) {
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
	private List<ExitEdge> addBlockEdges(Dag dag, DagNode block) {
        // draw the edges from the block
        
	    ExitEdge start = new ExitEdge();
        start.n1 = block;
	    List<ExitEdge> prevNodes = Collections.singletonList(start);
	    
	    for (DagNode c : block.children) {
	        if (prevNodes != null) {
	            for (ExitEdge e : prevNodes) {
	                e.n2 = c;
	                dag.addEdge(e);
	            }
	            prevNodes = addEdges(dag, c);
	        } else {
	            ExitEdge e = new ExitEdge();
	            e.n1 = c;
	            prevNodes = Collections.singletonList(e);
	        }
	    }
	    return prevNodes;
    }
	
	// draw branches from this block to then/else blocks
	// exit nodes are combined exit nodes of both branches
	private List<ExitEdge> addIfEdges(Dag dag, DagNode block) {
        // draw the edges
	    List<ExitEdge> prevNodes = new ArrayList<>();
	    if (block.children.size() == 1) {
	        DagNode c = block.children.get(0);
            dag.addEdge(block, c, "Y");
            List<ExitEdge> branchPrevNodes = addEdges(dag, c);
            ExitEdge e = new ExitEdge();
            e.n1 = block;
            e.label = "N";
            
            prevNodes.addAll(branchPrevNodes); // Y branch
            prevNodes.add(e); // N branch
	       
	    } else if (block.children.size() == 2) {
	        DagNode c1 = block.children.get(0);
	        DagNode c2 = block.children.get(1);
            dag.addEdge(block, c1, "Y");
            dag.addEdge(block, c2, "N");
            List<ExitEdge> branch1PrevNodes = addEdges(dag, c1);
            List<ExitEdge> branch2PrevNodes = addEdges(dag, c2);
            prevNodes.addAll(branch1PrevNodes);
            prevNodes.addAll(branch2PrevNodes);
            
	    } else {
	        throw new IllegalStateException("expected 2 children, found " + block.children.size());
	    }
           
        return prevNodes;
    }
	
	// draw branches into and out of try body
	// TODO: catch block
    private List<ExitEdge> addTryEdges(Dag dag, DagNode tryNode) {
        // draw the edges
        TryStatement ts;
        DagNode body = null;
        DagNode catchNode = null;
        for (DagNode c : tryNode.children) {
            // logger.info("try child " + c.locationInParent);
            // both try and catch have a locationInParent of 'body'
            if (c.astNode.getParent() instanceof CatchClause) {
                catchNode = c;
            } else {
                body = c;
            }
        }
        if (body == null) {
            throw new IllegalStateException("try with no body");
        }
        
        dag.addEdge(tryNode,  body);
        List<ExitEdge> bodyPrevNodes = addEdges(dag, body);
        return bodyPrevNodes;

      
           
        // return prevNodes;
    }
	
	
    /** Traces through the DAG, storing in each node the most recently seen node that had 'keepNode' set to true
     * (the 'lastKeepNode').
     *  
     * @param dag dag we're traversing
     * @param node the node in the dag we're up to
     * @param lastKeepNode most recently seen node with keepNode set to true
     */
    private void setLastKeepNode(Dag dag, DagNode node, DagNode lastKeepNode) {
        List<DagEdge> outEdges = node.outEdges;
        if (node.lastKeepNode != null) {
            // rejoined with other edges
            // logger.warn("lastKeepNode already set on node " + node.name + " as " + node.lastKeepNode.name + ", node.keepNode=" + node.keepNode + ", lastKeepNode=" + lastKeepNode.name);
            return;
        } else if (node.keepNode) {
            lastKeepNode = node;
        }
        node.lastKeepNode = lastKeepNode;
        for (DagEdge e : outEdges) {
            setLastKeepNode(dag, e.n2, lastKeepNode);
        }
    }

    // from the fromNode ?
    /** Prune all edges and nodes from an edge (e) to a keepNode
     * 
     * @param dag
     * @param e
     * @param fromNode
     * @param keepNode
     */
    private void pruneEdges(Dag dag, DagEdge e, DagNode fromNode, DagNode keepNode) {
        if (e.n1 == keepNode) {
            dag.removeEdge(e);
            return;
        } else if (e.n1 == null) {
            throw new IllegalStateException("e.n1 was null");
        } else {
            DagNode prevNode = e.n1;
            // logger.info("removing edge from " + prevNode.name + " to " + e.n2.name);
            dag.removeEdge(e);
            
            // prevNode may have already been removed
            if (!prevNode.keepNode) { // dag.nodes.contains(prevNode) && 
                // logger.info("removing node " + prevNode.name + " with " + prevNode.inEdges.size() + " inEdges");
                dag.nodes.remove(prevNode);
                for (DagEdge prevEdge : new ArrayList<DagEdge>(prevNode.inEdges)) { // may be modified whilst iterating ?
                    if (dag.edges.contains(prevEdge)) { // prevNode.inEdges
                        
                        pruneEdges(dag, prevEdge, fromNode, keepNode);
                    }
                }
            }
        }
    }

    private void removeNodes(Dag dag, DagNode node) {
        removeNodes(dag, node, true);
    }

    /** Clean up the dag by removing unused edges and nodes.
     * There are two operations performed here:
     * 'mergeEdges': if multiple inward edges on a node have the same lastKeepNode, then remove everything back to that lastKeepNode
     * 
     *         /-->  B --> C --> D  --\
     *     A  <                        >  H
     *         \-->  E --> F --> G  --/
     *  becomes
     *  
     *       A -> H
     *       
     *  assuming B through G are not keepNodes
     *   
     * 'shortenPath': if a node has 1 inward edge and 1 outward edge, remove the node and merge the edges, i.e.
     *      A -> B -> C 
     *  becomes 
     *      A -> C 
     *  if B is not a keepNode
     * 
     * @param dag
     * @param node
     * @param mergeEdges
     */
	private void removeNodes(Dag dag, DagNode node, boolean mergeEdges) {
	    
        List<DagEdge> inEdges = node.inEdges;
        List<DagEdge> outEdges = node.outEdges;
        Set<DagNode> redoNodes = new HashSet<>();
        
        if (mergeEdges && inEdges.size() > 0) {
            // if multiple in edges share a keepNode then remove everything back to that keepNode
            // and merge the edges
            for (int i = 0; i < inEdges.size() - 1; i++) {
                for (int j = i + 1; j < inEdges.size(); j++) {
                    DagEdge inEdge1 = inEdges.get(i);
                    DagEdge inEdge2 = inEdges.get(j);
                    DagNode inEdge1KeepNode = inEdge1.n1.lastKeepNode;
                    DagNode inEdge2KeepNode = inEdge2.n1.lastKeepNode;
                    if (inEdge1KeepNode == inEdge2KeepNode && inEdge1KeepNode != null) {
                        // logger.info("on node " + node.name + ", merged edges back to " + inEdge1KeepNode.name + ", i=" + i + ", j=" + j);
                        DagEdge newEdge = new DagEdge();
                        newEdge.n1 = inEdge1KeepNode;
                        newEdge.n2 = node;
                        newEdge.label = "something";

                        pruneEdges(dag, inEdge1, node, inEdge1KeepNode); // removed ++; } 
                        pruneEdges(dag, inEdge2, node, inEdge2KeepNode); // removed ++; }
                        if ((newEdge.n1 != newEdge.n2) && !dag.hasEdge(newEdge.n1, newEdge.n2)) {
                            dag.addEdge(newEdge);
                        }
                        redoNodes.add(inEdge1KeepNode);
                        
                        // inEdge1.n1 = inEdge1KeepNode; // format the edge a bit ?
                        // inEdges.remove(j); j--;
                        // if (lastEdge1 != null) { lastEdge1.n2 = node; }
                        // if (lastEdge2 != null) { lastEdge2.n2 = node; }
                        // start from the top
                        i=0;
                        j=0;
                    }
                }
            }
        }

        // if there's one edge leading in and one leading out
        // and there's no comment on this node, remove it
        
        if (inEdges.size() == 1 && outEdges.size() == 1 && !node.keepNode) {
            
            DagEdge inEdge = inEdges.get(0);
            DagEdge outEdge = outEdges.get(0);

            DagEdge newEdge = new DagEdge();
            newEdge.n1 = inEdge.n1;
            newEdge.n2 = outEdge.n2;
            newEdge.label = inEdge.label;
            
            dag.removeEdge(inEdge);
            dag.removeEdge(outEdge);
            if (!dag.hasEdge(newEdge.n1, newEdge.n2)) {
                dag.addEdge(newEdge);
            }
            
            dag.nodes.remove(node);
            removeNodes(dag, newEdge.n2, mergeEdges);
        
        } else if (mergeEdges) {
            // logger.warn(node.outEdges.size() + " outEdges from node " + node.name);
            for (DagEdge e : new ArrayList<DagEdge>(node.outEdges)) {
                if (dag.edges.contains(e)) {
                    if (dag.nodes.contains(e.n2)) {
                        removeNodes(dag, e.n2);
                    } else {
                        logger.warn("subnode " + e.n2.name + " missing from " + node.name);
                        dag.removeEdge(e);
                    }
                } else {
                    // logger.warn("edge " + e + " missing from " + node.name);
                }
            }
        }
        
        for (DagNode redo : redoNodes) {
            removeNodes(dag, redo, false);
        }
	    
	}


    @SuppressWarnings("unchecked")
    private List<CommentText> getComments(CompilationUnit cu, String src) {
	    List<CommentText> comments = new ArrayList<>();
        for (Comment c : (List<Comment>) cu.getCommentList()) {
            // comment.accept(cv);
            int start = c.getStartPosition();
            int end = start + c.getLength();
            String text = src.substring(start, end);
            if (c.isLineComment()) {
                if (text.startsWith("//")) {
                    text = text.substring(2).trim();
                    if (text.startsWith("gv:")) {
                        text = text.substring(3).trim();
                        // c.getAlternateRoot() is the CompilationUnit for all of these comments
                        // which isn't all that useful really
                        comments.add(new CommentText(c, cu.getLineNumber(start), text));
                    }
                    
                } else {
                    throw new IllegalStateException("Line comment does not start with '//': '" + text + "'");
                }
            }
            
            
        }
        return comments;
    }




    static class CommentText {
	    Comment comment;
	    int line;
	    String text;
	    
	    public CommentText(Comment c, int line, String text) {
	        this.comment = c;
	        this.line = line;
	        this.text = text;
	    }
	}
	
    /** A directed acyclic graph (DAG), used to generate the graphviz diagram.
     * <p>This is two data-structures in one; it mirrors the eclipse AST in a new datastructure that I 
     * have a bit more control over, and it also acts as the source for the graphviz diagram.
     * 
     * <ol><li>The AST structure is defined using the node.children / node.parentDagNode fields in the DagNodes
     *     <ul><li>Each DagNode has an astNode field linking it back to the original CompilationUnit AST
     *     </ul>
     *     <li>The diagram is defined using the edges between the visible nodes, which are defined in 
     *     <ul><li>dag.edges
     *         <li>node.inEdges
     *         <li>node.outEdges
     *     </ul>
     * </ol>
     * 
     * <p>NB: we can make a DAG look like a cyclical graph by reversing the arrows on the diagram, which
     * we might want to do for loops later on. Or I guess we could make it cyclical. 
     */
	static class Dag {
	    List<DagNode> nodes = new ArrayList<>();
	    List<DagEdge> edges = new ArrayList<>();
	    Set<String> names = new HashSet<String>();
	    Logger logger = Logger.getLogger(Dag.class);
	    
	    Map<ASTNode, DagNode> astToDagNode = new HashMap<>();
	    public void addNode(DagNode n) {
	        nodes.add(n);
	        astToDagNode.put(n.astNode, n);
	    }
        public boolean hasEdge(DagNode n1, DagNode n2) {
            for (DagEdge e : edges) {
                if (e.n1 == n1 && e.n2 == n2) { return true; }
            }
            return false;
        }
        public void addEdge(DagEdge e) {
            edges.add(e);
            e.n1.outEdges.add(e);
            e.n2.inEdges.add(e);
        }
	    public DagEdge addEdge(DagNode n1, DagNode n2) {
	        return addEdge(n1, n2, null);
	    }
	    public DagEdge addEdge(DagNode n1, DagNode n2, String label) {
	        if (n1 == null) { throw new NullPointerException("null n1"); }
	        if (n2 == null) { throw new NullPointerException("null n2"); }
	        DagEdge e = new DagEdge();
	        e.n1 = n1;
	        e.n2 = n2;
	        e.label = label;
	        edges.add(e);
	        e.n1.outEdges.add(e);
            e.n2.inEdges.add(e);
	        return e;
	    }
        public void removeEdge(DagEdge e) {
            if (e.n1 == null) { logger.warn("e.n1 is null"); } else { e.n1.outEdges.remove(e); }
            if (e.n2 == null) { logger.warn("e.n2 is null"); } else { e.n2.inEdges.remove(e); }
            e.n1 = null; e.n2 = null;
            edges.remove(e);
        }
        
        /** Create a unique node name (appending _2, _3 etc suffixes until it's unique).
         * Our node names are basically the line number (badoom boom tish), so this is required when there's >1 statement on a line.
         * 
         * @param n
         * @return
         */
        public String getUniqueName(String n) {
            if (!names.contains(n)) {
                names.add(n); 
                return n;
            }
            int idx = 2;
            while (names.contains(n + "_" + idx)) {
                idx++;
            }
            names.add(n + "_" + idx); 
            return n + "_" + idx;
        }
	    
	}
	static class DagNode {
	    DagNode parentDagNode;
	    List<DagNode> children = new ArrayList<>();
	    
	    String dagLoc;
	    String locationInParent; 
	    
	    boolean keepNode = false;
	    DagNode lastKeepNode = null;
	    List<DagEdge> inEdges = new ArrayList<>();
	    List<DagEdge> outEdges = new ArrayList<>();
	    
	    String type;
	    int line;
	    String name;
	    String label;
	    
	    ASTNode astNode;
	    
	    public void addChild(DagNode node) {
	        children.add(node);
	    }
	    
	    public String toGraphviz() {
	        String labelText = line + ": " + Text.replaceString(label, "\"",  "\\\"") +
	            (lastKeepNode == null ? "" : ", lkn=" + lastKeepNode.name);
	        return
	          (dagLoc == null ? "" : dagLoc) + 
	          name + " [\n" + 
              "  label = \"" + labelText + "\";\n" + 
              "];";	        
	    }
	}
	
	static class DagEdge {
	    DagNode n1;
	    DagNode n2;
	    String label;

	    public String toGraphviz() {
	        String labelText = label == null ? null : Text.replaceString(label, "\"",  "\\\"");
            return n1.name + " -> " + n2.name + 
               (label == null ? "" : " [ label = \"" + labelText + "\" ]") +
                ";"; 
        }
	}
	
	// an edge whose second node isn't known yet
	static class ExitEdge extends DagEdge {
	    
	}
	
	
	static class DagVisitor extends ASTVisitor {
	    
	    Logger logger = Logger.getLogger(DagVisitor.class);
	    
        int lastIdx = 0;
        String className; // SimpleName ?
        String methodName;
        String lastClass = null;
        String lastMethod = null;
        Dag dag;
        List<CommentText> comments;
        CompilationUnit cu;
        String src;
        
        public DagVisitor(CompilationUnit cu, String src, List<CommentText> comments) {
            super(true);
            this.cu = cu;
            this.comments = comments;
            this.src = src;
            dag = new Dag();
        }
        
        public Dag getDag() { 
            return dag;
        }
        
        void writeCommentsToLine(DagNode pdn, int line) {
            // DagNode lastNode = null;
            while (lastIdx < comments.size() && comments.get(lastIdx).line < line) {
                CommentText ct = comments.get(lastIdx);
                
                String dagLoc = "";
                
                DagNode dn = new DagNode();
                dn.keepNode = true; // always keep comments
                dn.dagLoc = dagLoc;
                dn.type = "comment";
                dn.line = ct.line;
                dn.name = dag.getUniqueName("c_" + ct.line);
                dn.label = ct.text;
                dn.astNode = null;
                
                dag.addNode(dn);
                if (pdn!=null) {
                    pdn.addChild(dn);
                } else {
                    logger.warn("null pdn on " + dn.type + " on line " + dn.line);
                }
                lastIdx ++; 
            }
        }
        
        public boolean preVisit2(ASTNode node) {
            DagNode pdn = getClosestDagNode(node);
            
            int line = cu.getLineNumber(node.getStartPosition());
            // writeCommentsToLine(line);

            if (node instanceof Statement) {
                
                writeCommentsToLine(pdn, line);
                
                DagNode dn = new DagNode();
                String clazz = Text.getLastComponent(node.getClass().getName());
                if (clazz.endsWith("Statement")) {
                    clazz = clazz.substring(0, clazz.length() - 9);
                }
                String lp = "s"; // linePrefix
                if (clazz.equals("If")) { lp = "if"; }
                
                dn.type = clazz; // "if";
                dn.line = line;
                dn.name = dag.getUniqueName(lp + "_" + line); // "if_" + line;
                dn.label = clazz; // "if";
                dn.parentDagNode = pdn;
                dn.astNode = node;
                dn.locationInParent = node.getLocationInParent().getId();
                dag.addNode(dn);
                if (pdn!=null) {
                    pdn.addChild(dn);
                } else {
                    // logger.warn("null pdn on " + node);
                    logger.warn("preVisit: null pdn on " + dn.type + " on line " + dn.line);
                }
                
                if (lastIdx < comments.size() && comments.get(lastIdx).line == line) {
                    CommentText ct = comments.get(lastIdx);
                    dn.keepNode = true; // always keep comments
                    dn.label = ct.text;
                    lastIdx++;
                }
            }
            
            return true;
        }
        
        public void postVisit(ASTNode node) {
            
        }
        
        public DagNode getClosestDagNode(ASTNode node) {
            while (node != null) {
                if (dag.astToDagNode.get(node) != null) { 
                    return dag.astToDagNode.get(node);
                }
                ASTNode parent = node.getParent();
                node = parent;
            }
            return null;
        }
        
    }	
	
	
	public static void main(String args[]) throws Exception {
	    Log4jCliConfiguration lcc = new Log4jCliConfiguration();
	    lcc.init("[JavaToGraphviz]", null);
	    Logger logger = Logger.getLogger(JavaToGraphviz3.class);
	    
		JavaToGraphviz3 javaToGraphviz = new JavaToGraphviz3();
		javaToGraphviz.test();
	}
}