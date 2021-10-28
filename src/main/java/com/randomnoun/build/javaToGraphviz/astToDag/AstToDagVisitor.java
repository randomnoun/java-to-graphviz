package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

import com.randomnoun.build.javaToGraphviz.comment.CommentText;
import com.randomnoun.build.javaToGraphviz.comment.GvComment;
import com.randomnoun.build.javaToGraphviz.comment.GvGraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvSubgraphComment;
import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.common.Text;

/** An ASTVisitor that constructs the Dag
 * 
 * <p>Each AstNode is converted into a DagNode, and extra nodes are created for comments, which 
 * don't appears in the eclipse AST in the right place for some reason. 
 * 
 * <p>Comments that appear on the same line as a statement are associated with that statement.
 *
 * <p>If there are multiple statements on a line, it will be associated with the first one on the line.
 */
public class AstToDagVisitor extends ASTVisitor {
    
    Logger logger = Logger.getLogger(AstToDagVisitor.class);
    
    int lastIdx = 0;
    String className; // SimpleName ?
    String methodName;
    String lastClass = null;
    String lastMethod = null;
    Dag dag;
    List<CommentText> comments;
    CompilationUnit cu;
    String src;
    boolean includeThrowNode;
    
    public AstToDagVisitor(CompilationUnit cu, String src, List<CommentText> comments, boolean includeThrowNode) {
        super(true);
        this.cu = cu;
        this.comments = comments;
        this.src = src;
        this.includeThrowNode = includeThrowNode;
        dag = new Dag();
    }
    
    public Dag getDag() { 
        return dag;
    }
    
    void processCommentsToMethodNode(DagNode mn, int line) {
        // DagNode lastNode = null;
        while (lastIdx < comments.size() && comments.get(lastIdx).line < line) {
            CommentText ct = comments.get(lastIdx);
            /*
            DagNode dn = new DagNode();
            dn.keepNode = true; // always keep comments
            dn.type = "comment";
            dn.line = ct.line;
            dn.name = dag.getUniqueName("c_" + ct.line);
            dn.label = ct.text;
            dn.astNode = null;
            */
            if (ct instanceof GvComment) {
                mn.classes.addAll(((GvComment) ct).classes);
                mn.label = ct.text; // last comment wins
            } else if (ct instanceof GvGraphComment) {
                mn.digraphId = ct.text;
            } else if (ct instanceof GvSubgraphComment) {
                mn.subgraph = ct.text;
            }
            lastIdx ++; 
        }
    }
    
    void createCommentNodesToLine(DagNode pdn, int line) {
        // DagNode lastNode = null;
        while (lastIdx < comments.size() && comments.get(lastIdx).line < line) {
            CommentText ct = comments.get(lastIdx);
            
            DagNode dn = new DagNode();
            dn.keepNode = true; // always keep comments
            dn.type = "comment";
            dn.line = ct.line;
            // dn.name = dag.getUniqueName("c_" + ct.line);
            dn.classes.add("comment");
            dn.label = ct.text;
            dn.astNode = null;
            if (ct instanceof GvComment) {
                dn.classes.addAll(((GvComment) ct).classes);
            } else if (ct instanceof GvGraphComment) {
                dn.digraphId = ct.text;
            } else if (ct instanceof GvSubgraphComment) {
                dn.subgraph = ct.text;
            }
            
            if (pdn!=null) {
                dag.addNode(dn);
                pdn.addChild(dn);
            } else {
                throw new IllegalStateException("null pdn in createCommentNodesToLine");
                // logger.warn("null pdn on " + dn.type + " on line " + dn.line);
            }
            lastIdx ++; 
        }
    }
    
    
    
    public boolean preVisit2(ASTNode node) {
        DagNode pdn = getClosestDagNode(node);
        
        int line = cu.getLineNumber(node.getStartPosition());
        // writeCommentsToLine(line);

        if (node instanceof MethodDeclaration ||
            node instanceof CatchClause ||
            (node instanceof Statement &&
            (includeThrowNode || !(node instanceof ThrowStatement)) )) {
            
            DagNode dn = new DagNode();
            String clazz = Text.getLastComponent(node.getClass().getName());
            if (clazz.endsWith("Statement")) {
                clazz = clazz.substring(0, clazz.length() - 9);
            }
            // String lp = "s"; // linePrefix
            // if (clazz.equals("If")) { lp = "if"; }
            // else if (clazz.equals("MethodDeclaration")) { lp = "cluster"; }
            
            dn.type = clazz; // "if";
            // dn.label = clazz; // "if"; 
            dn.label = null; // now set by gv-labelFormat
            dn.classes.add(Text.toFirstLower(clazz));
            
            dn.line = line;
            //dn.name = dag.getUniqueName(lp + "_" + line); // "if_" + line;
            dn.name = null;
            dn.parentDagNode = pdn;
            dn.astNode = node;
            dn.locationInParent = node.getLocationInParent().getId();

            if (node instanceof MethodDeclaration) {
                // comments get assigned to this node
                processCommentsToMethodNode(dn, line);
            } else {
                // comments have their own nodes
                createCommentNodesToLine(pdn, line);                    
            }
            
            dag.addNode(dn);
            if (pdn!=null) {
                pdn.addChild(dn);
            } else {
                // logger.warn("null pdn on " + node);
                logger.warn("preVisit: null pdn on " + dn.type + " on line " + dn.line);
                // each method is it's own subgraph
                dag.addRootNode(dn);
            }
            
            if (lastIdx < comments.size() && comments.get(lastIdx).line == line) {
                CommentText ct = comments.get(lastIdx);
                dn.keepNode = true; // always keep comments
                
                if (ct instanceof GvComment) {
                    GvComment gvComment = (GvComment) ct;
                    dn.label = gvComment.text; // if not blank ?
                    dn.classes.addAll(gvComment.classes);
                    if (gvComment.inlineStyleString!=null) {
                        dn.gvAttributes.put("style", gvComment.inlineStyleString);
                    }
                } else if (ct instanceof GvGraphComment) {
                    dn.digraphId = ct.text;
                } else if (ct instanceof GvSubgraphComment) {
                    dn.subgraph = ct.text;
                }
                lastIdx++;
            }
        }
        
        return true;
    }
    
    public void postVisit(ASTNode node) {
        
    }
    
    
    /** Navigate back up the AST tree until we find an ASTNode that's already in the Dag,
     * and then return that DagNode
     * 
     * @param node
     * @return
     */
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