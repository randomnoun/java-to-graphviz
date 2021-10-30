package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

import com.randomnoun.build.javaToGraphviz.comment.CommentText;
import com.randomnoun.build.javaToGraphviz.comment.GvComment;
import com.randomnoun.build.javaToGraphviz.comment.GvEndSubgraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvGraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvSubgraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvLiteralComment;
import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
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
    DagSubgraph root;
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
        root = new DagSubgraph(dag, null);
        dag.rootGraphs.add(root);
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
                GvGraphComment gc =  ((GvGraphComment) ct);
                // mn.digraphId = ct.text;
                
                // if the previous root doesn't have any nodes in it, apply these classes/styles to that root
                if (root.nodes.size() > 0) {
                    root = new DagSubgraph(dag, null);
                    dag.rootGraphs.add(root);
                }
                root.name = gc.id;
                root.classes.addAll(gc.classes);
                root.gvAttributes.put("style", gc.inlineStyleString); // append to existing ?
                
            } else if (ct instanceof GvSubgraphComment) {
                logger.warn("gv-subgraph outside of method");
            } else if (ct instanceof GvLiteralComment) {
                GvLiteralComment gvlc = (GvLiteralComment) ct;
                root.literals.add(gvlc.text);

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
            dn.lineNumber = ct.line;
            // dn.name = dag.getUniqueName("c_" + ct.line);
            dn.classes.add("comment");
            dn.label = ct.text;
            dn.astNode = null;
            if (ct instanceof GvComment) {
                GvComment gc = (GvComment) ct;
                dn.name = gc.id;
                dn.classes.addAll(gc.classes);
                dn.gvAttributes.put("style", gc.inlineStyleString); // append to existing ?
                
            } else if (ct instanceof GvGraphComment) {
                // @TODO something

            } else if (ct instanceof GvSubgraphComment) {
                GvSubgraphComment gvsc = (GvSubgraphComment) ct;
                dn.name = gvsc.id;
                dn.classes.addAll(gvsc.classes);
                dn.classes.add("beginSubgraph");

            } else if (ct instanceof GvEndSubgraphComment) {
                dn.classes.add("endSubgraph");

            } else if (ct instanceof GvLiteralComment) {
                GvLiteralComment gvlc = (GvLiteralComment) ct;
                root.literals.add(gvlc.text);
                dn = null;
                
            }

            if (dn != null) {
                if (pdn!=null) {
                    dag.addNode(root, dn);
                    pdn.addChild(dn);
                } else {
                    throw new IllegalStateException("null pdn in createCommentNodesToLine");
                    // logger.warn("null pdn on " + dn.type + " on line " + dn.line);
                }
            }
            lastIdx ++; 
        }
    }
    
    
    
    public boolean preVisit2(ASTNode node) {
        DagNode pdn = getClosestDagNode(node);
        
        int lineNumber = cu.getLineNumber(node.getStartPosition());
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
            
            dn.type = clazz; // "if";
            dn.name = null;
            dn.label = null; // now set by gv-labelFormat
            dn.classes.add(Text.toFirstLower(clazz));
            
            dn.lineNumber = lineNumber;
            dn.parentDagNode = pdn;
            dn.astNode = node;

            if (node instanceof MethodDeclaration) {
                // comments get assigned to this node
                processCommentsToMethodNode(dn, lineNumber);
            } else {
                // comments have their own nodes
                createCommentNodesToLine(pdn, lineNumber);                    
            }
            
            dag.addNode(root, dn);
            if (pdn!=null) {
                pdn.addChild(dn);
            } else {
                // logger.warn("null pdn on " + node);
                logger.warn("preVisit: null pdn on " + dn.type + " on line " + dn.lineNumber);
                // each method is it's own subgraph
                dag.addRootNode(dn);
            }
            
            if (lastIdx < comments.size() && comments.get(lastIdx).line == lineNumber) {
                CommentText ct = comments.get(lastIdx);
                dn.keepNode = true; // always keep commented nodes
                
                if (ct instanceof GvComment) {
                    GvComment gvComment = (GvComment) ct;
                    if (!Text.isBlank(gvComment.id)) { dn.name = gvComment.id; }
                    if (!Text.isBlank(gvComment.text)) { dn.label = gvComment.text; } 
                    dn.classes.addAll(gvComment.classes);
                    if (gvComment.inlineStyleString!=null) {
                        dn.gvAttributes.put("style", gvComment.inlineStyleString);
                    }
                } else if (ct instanceof GvGraphComment) {
                    // @TODO something
                    
                } else if (ct instanceof GvSubgraphComment) {
                    GvSubgraphComment gvsc = (GvSubgraphComment) ct;
                    dn.label = gvsc.text; // if not blank ?
                    dn.name = gvsc.id;
                    dn.classes.addAll(gvsc.classes);
                    dn.classes.add("beginGraph");
                    
                } else if (ct instanceof GvEndSubgraphComment) {
                    dn.classes.add("endGraph");

                } else if (ct instanceof GvLiteralComment) {
                    GvLiteralComment gvlc = (GvLiteralComment) ct;
                    root.literals.add(gvlc.text);

                }
                lastIdx++;
            }
            
            // if this is a method node, include comments to the end of the method
            
            
            
        }
        
        return true;
    }
    
    public void postVisit(ASTNode node) {
        // DagNode pdn = getClosestDagNode(node);

        if (node instanceof Block) {
            DagNode dn = dag.astToDagNode.get(node);
            int endLineNumber = cu.getLineNumber(node.getStartPosition() + node.getLength());
            
            // add comments at the end of a block to that block
            createCommentNodesToLine(dn, endLineNumber);
        }
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