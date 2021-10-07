package com.randomnoun.build.javaToGraphviz;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.randomnoun.common.StreamUtil;

/**
 * A complete standalone example of ASTParser
 * @see https://www.programcreek.com/2011/01/a-complete-standalone-example-of-astparser/
 *
 */
public class JavaToGraphviz2 {

	
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
		
		PrintWriter pw = new PrintWriter(System.out);
		pw.println("digraph G {\r\n" +
			"  graph [fontname = \"Handlee\"];\r\n" +
			"  node [fontname = \"Handlee\"; shape=rect; ];\r\n" +
			"  edge [fontname = \"Handlee\"];\r\n" +
			"\r\n" +
			"  bgcolor=transparent;");
		
		
		// https://www.programcreek.com/2013/03/get-internal-comments-by-using-eclipse-jdt-astparser/
		// hmm this seems like a weird way of doing things. anyway.
		// CommentVisitor cv = new CommentVisitor(cu, src);
		
		List<CommentText> comments = getComments(cu, src);
		
		// Comments have an alternoteRoot which links it back to the compilationUnit AST node

		// probably need to construct a DAG now don't I
		
		DagVisitor dv = new DagVisitor(cu, src, comments);
        cu.accept(dv);
        Dag dag = dv.getDag();
        
        for (DagNode node : dag.nodes) {
            pw.println(node.toGraphviz());
        }
        for (DagEdge edge : dag.edges) {
            pw.println(edge.toGraphviz());
        }
        
        pw.flush();
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
	
	static class Dag {
	    List<DagNode> nodes = new ArrayList<>();
	    List<DagEdge> edges = new ArrayList<>();
	    
	    Map<ASTNode, DagNode> astToDagNode = new HashMap<>();
	    public void addNode(DagNode n) {
	        nodes.add(n);
	        astToDagNode.put(n.astNode, n);
	    }
	    public void addEdge(DagNode n1, DagNode n2) {
	        if (n1 == null) { throw new NullPointerException("null n1"); }
	        if (n2 == null) { throw new NullPointerException("null n2"); }
	        DagEdge e = new DagEdge();
	        e.n1 = n1;
	        e.n2 = n2;
	        edges.add(e);
	    }
	    
	}
	static class DagNode {
	    String dagLoc;
	    String type;
	    int line;
	    String name;
	    String label;
	    
	    ASTNode astNode;
	    
	    public String toGraphviz() {
	        return
	          (dagLoc == null ? "" : dagLoc) + 
	          name + " [\n" + 
              "  label = \"" + label + "\";\n" +  // @TODO escape
              "];";	        
	    }
	}
	static class DagEdge {
	    DagNode n1;
	    DagNode n2;

       public String toGraphviz() {
            return n1.name + " -> " + n2.name + ";"; 
        }

       
	}
	
	
	static class DagVisitor extends ASTVisitor {
	    
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
        
        DagNode writeCommentsToLine(DagNode lastNode, int line) {
            // DagNode lastNode = null;
            while (lastIdx < comments.size() && comments.get(lastIdx).line < line) {
                CommentText ct = comments.get(lastIdx);
                
                String dagLoc = "";
                
                DagNode dn = new DagNode();
                dn.dagLoc = dagLoc;
                dn.type = "comment";
                dn.line = ct.line;
                dn.name = "c_" + line;
                dn.label = ct.text;
                dag.nodes.add(dn);
                if (lastNode != null) {
                    // pw.println(lastNode.name + " -> " + dn.name);
                    DagEdge de = new DagEdge();
                    de.n1 = lastNode;
                    de.n2 = dn;
                    dag.edges.add(de);
                }
                lastNode = dn;
                lastIdx ++; 
            }
            return lastNode;
        }
        
        public boolean visit(TypeDeclaration node) {
            int line = cu.getLineNumber(node.getStartPosition());
            // writeCommentsToLine(line);
            boolean isClass = !node.isInterface();
            
            className = node.getName().toString();
            return true;  
        }
        
        public boolean visit(MethodDeclaration node) {
            int line = cu.getLineNumber(node.getStartPosition());
            // writeCommentsToLine(line);
            
            boolean isConstructor = node.isConstructor();
            methodName = node.getName().toString();
            return true; // set to 'false' to not visit usage info 
        }
        
        public boolean visit(Block node) {
            
            int line = cu.getLineNumber(node.getStartPosition());
            System.out.println("block started " + line);
            // skipCommentsToLine(line);
            // writeCommentsToLine(line);
            if ( (lastIdx < comments.size() && comments.get(lastIdx).line < line)) {
                CommentText ct = comments.get(lastIdx);
                throw new IllegalStateException("unwritten comment line " + ct.text);
            }
            
            // pw.println("# " + cu.getLineNumber(node.getStartPosition()) + ":     block with " + node.statements().size() + " statements");
            // return true; // set to 'false' to not visit usage info
            DagNode lastDn = null;
            Statement lastStatement = null;
            
            List<DagNode> exitNodes = new ArrayList<>();
            
            for ( int i=0; i < node.statements().size(); i++ ) {
                
                Statement s = (Statement) node.statements().get(i);
                line = cu.getLineNumber(s.getStartPosition());
                System.out.println("statement line " + line);
                
                lastDn = writeCommentsToLine(lastDn, line);
                
                // this.visit(s);
                s.accept(this);
                DagNode dn = dag.astToDagNode.get(s);
                if (dn == null) {
                    // no dag node created, let's create one anyway
                    dn = new DagNode();
                    dn.type = "s";
                    dn.line = cu.getLineNumber(s.getStartPosition());
                    dn.name = "s_" + line;
                    dn.label = s.getClass().getName();
                    dn.astNode = node;
                    dag.addNode(dn);
                    
                    if (lastIdx < comments.size() && comments.get(lastIdx).line == line) {
                        CommentText ct = comments.get(lastIdx);
                        dn.label = ct.text;
                        lastIdx++;
                    }
                }
                
                // TODO: get exitNodes from this dn in a slightly more generic way
                
                // this seems a bit fragile
                if ((lastStatement instanceof IfStatement) && (lastDn != null)) {
                    Statement thenStatement = ((IfStatement) lastStatement).getThenStatement();
                    Statement elseStatement = ((IfStatement) lastStatement).getElseStatement();
                    DagNode thenDag = dag.astToDagNode.get(thenStatement);
                    DagNode elseDag = dag.astToDagNode.get(elseStatement);
                    
                    if (thenDag != null) { 
                        // hmm
                        dag.addEdge(thenDag, dn);
                    }
                    if (elseDag != null) {
                        dag.addEdge(elseDag, dn);
                    }
                    
                } else {
                    if (lastDn != null) {
                        dag.addEdge(lastDn, dn);
                    }
                }
                
                lastStatement = s;
                lastDn = dn;
            }
            
            if (lastDn != null) {
                exitNodes.add(lastDn);
            }
            
            
         
            return false;
        }

        /* 
             * 
Statement:
 *    {@link AssertStatement},
 *    {@link Block},
 *    {@link BreakStatement},
 *    {@link ConstructorInvocation},
 *    {@link ContinueStatement},
 *    {@link DoStatement},
 *    {@link EmptyStatement},
 *    {@link EnhancedForStatement}
 *    {@link ExpressionStatement},
 *    {@link ForStatement},
 *    {@link IfStatement},
 *    {@link LabeledStatement},
 *    {@link ReturnStatement},
 *    {@link SuperConstructorInvocation},
 *    {@link SwitchCase},
 *    {@link SwitchStatement},
 *    {@link SynchronizedStatement},
 *    {@link ThrowStatement},
 *    {@link TryStatement},
 *    {@link TypeDeclarationStatement},
 *    {@link VariableDeclarationStatement},
 *    {@link WhileStatement}
 * </pre>
             */

        
        
        public DagNode getClosestDagNode(ASTNode node) {
            while (node != null) {
                if (dag.astToDagNode.get(node) != null) { 
                    return dag.astToDagNode.get(node);
                }
                ASTNode parent = node.getParent();
                StructuralPropertyDescriptor spd = node.getLocationInParent(); // hrm
                node = parent;
            }
            return null;
        }
        
        // @Override
        public boolean visit(IfStatement node) {
            DagNode pdn = getClosestDagNode(node);
            
            int line = cu.getLineNumber(node.getStartPosition());
            // writeCommentsToLine(line);

            DagNode dn = new DagNode();
            dn.type = "if";
            dn.line = line;
            dn.name = "if_" + line;
            dn.label = "if";
            dn.astNode = node;
            dag.addNode(dn);
            
            if (lastIdx < comments.size() && comments.get(lastIdx).line == line) {
                CommentText ct = comments.get(lastIdx);
                dn.label = ct.text;
                lastIdx++;
                
                // dag.addEdge(pdn, dn); // well this will be wrong in so many ways

                // search for first gv comment in both statement and else blocks and the first statement after this if
                // and draw edges to all three
                
                // maybe I should be drawing edges to every statement
                // and then just cull the dag afterwards
                
                
            } else {
                // hmm
            }
            
            DagNode thenDn = new DagNode();
            dn.type = null; // container dag node 
            dn.astNode = node.getThenStatement();
            dag.addEdge(dn, thenDn);
            
            if (node.getElseStatement() != null) {
                DagNode elseDn = new DagNode();
                dn.type = null; // container dag node 
                dn.astNode = node.getElseStatement();
                dag.addEdge(dn, elseDn);
            }
            
            // pw.println("# " + cu.getLineNumber(node.getStartPosition()) + ":     if");
            return true; // set to 'false' to not visit usage info 
        }
        
        public void endVisit(IfStatement node) {
            DagNode dn = dag.astToDagNode.get(node);
            if (dn!=null) {
                // draw some lines from the thingy to the whatsit
                
                
            }
        }            

        
        
    }	
	
	
	public static void main(String args[]) throws Exception { 
		JavaToGraphviz2 javaToGraphviz = new JavaToGraphviz2();
		javaToGraphviz.test();
	}
}