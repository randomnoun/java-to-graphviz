package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    Dag dag;
    DagSubgraph root;
    List<CommentText> comments;
    CompilationUnit cu;
    String src;
    boolean includeThrowNode;
    int nextLineFromLine = 0;
    List<GvComment> nextLineComments = new ArrayList<>(); 
    List<GvComment> nextDagComments = new ArrayList<>();
    
    
    // Map of line number -> list of ASTs that start / end on that line
    Map<Integer, List<ASTNode>> startLineNumberAsts;
    Map<Integer, List<ASTNode>> endLineNumberAsts;
    
    public DagNode getStartingDagNodeAboveLine(int originalLine) {
        // find the first ast node above this line which has a dag node
        int line = originalLine;
        line--;
        while (line > 0) {
            List<ASTNode> astNodes = startLineNumberAsts.get(line);
            if (astNodes != null) {
                for (ASTNode n : astNodes) {
                    DagNode dn = dag.astToDagNode.get(n); 
                    if (dn != null) {
                        return dn;
                    }
                }
            }
        }
        // throw new IllegalStateException("No dagNode found above line " + originalLine);
        return null;
    }
    
    private DagNode getStartingDagNodeOnLine(int originalLine) {
        // find the first ast node on this line which has a dag node
        List<ASTNode> astNodes = startLineNumberAsts.get(originalLine);
        if (astNodes == null) {
            return null;
        }
        // first dag node that doesn't already have a comment
        for (int i = 0; i < astNodes.size(); i++) {
            ASTNode n = astNodes.get(i);
            DagNode dn = dag.astToDagNode.get(n); 
            if (dn != null && !dn.hasComment) {
                return dn;
            }
        }
        // there isn't one; first dag node
        for (int i = 0; i < astNodes.size(); i++) {
            ASTNode n = astNodes.get(i);
            DagNode dn = dag.astToDagNode.get(n); 
            if (dn != null) {
                return dn;
            }
        }
        
        return null;
    }
    
    private DagNode getEndDagNodeBeforeLineColumn(int line, int column) {
        // find the last ast node on this line before this column which has a dag node

        List<ASTNode> astNodes = endLineNumberAsts.get(line);
        if (astNodes == null) {
            throw new IllegalStateException("No astNode found on line " + line);
        }
        for (int i = astNodes.size() - 1; i >= 0; i--) {
            ASTNode n = astNodes.get(i);
            int endColumn = cu.getColumnNumber(n.getStartPosition() + n.getLength());
            if (endColumn < column) {
                DagNode dn = dag.astToDagNode.get(n); 
                if (dn != null) {
                    return dn;
                }
            }
        }
        // throw new IllegalStateException("No dagNode found on line " + line);
        return null;
    }

    private void annotateDag(DagNode dn, GvComment gc) {
        if (!Text.isBlank(gc.id)) { dn.name = gc.id; }
        if (!Text.isBlank(gc.text)) { dn.label = gc.text; } 
        dn.classes.addAll(gc.classes);
        if (gc.inlineStyleString!=null) {
            dn.gvAttributes.put("style", gc.inlineStyleString); // append to existing ?
        }
        if (dn.hasComment) {
            logger.warn("multiple comments associated with statement");
        } else {
            dn.hasComment = true;
        }
    }
    
    
    public AstToDagVisitor(CompilationUnit cu, String src, List<CommentText> comments, boolean includeThrowNode) {
        super(true);
        this.cu = cu;
        this.comments = comments;
        this.src = src;
        this.includeThrowNode = includeThrowNode;
        dag = new Dag();
        root = new DagSubgraph(dag, null);
        dag.rootGraphs.add(root);
        
        AstToLineVisitor lv = new AstToLineVisitor(cu, src, comments, includeThrowNode);
        cu.accept(lv);
        startLineNumberAsts = lv.getStartLineNumberAstsMap();
        endLineNumberAsts = lv.getEndLineNumberAstsMap();
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
                logger.warn("gv-literal outside of method");

            }
            
            lastIdx ++; 
        }
    }
    
    void processCommentsToStatementNode(DagNode pdn, int line, int column, DagNode currentLineDn) {
        // DagNode lastNode = null;
        while (lastIdx < comments.size() && 
            (comments.get(lastIdx).line < line || 
            (comments.get(lastIdx).line == line && comments.get(lastIdx).column < column))) {
            
            CommentText ct = comments.get(lastIdx);
            DagNode dn;
            
            if (currentLineDn == null) {
                dn = new DagNode();
                // dn.name = dag.getUniqueName("c_" + ct.line);
                dn.keepNode = true; // always keep comments
                dn.type = "comment";
                dn.lineNumber = ct.line;
                dn.classes.add("comment");
                dn.label = ct.text;
                dn.astNode = null;
            } else {
                dn = currentLineDn;
            }
            
            if (ct instanceof GvComment) {
                GvComment gc = (GvComment) ct;
                if (Text.isBlank(gc.direction)) {
                    DagNode prevDagNode = getStartingDagNodeOnLine(ct.line);
                    if (prevDagNode == null) {
                        // no direction
                        dn.name = gc.id;
                        dn.classes.addAll(gc.classes);
                        dn.gvAttributes.put("style", gc.inlineStyleString); // append to existing ?                
                    } else {
                        annotateDag(prevDagNode, gc);
                        dn = null;
                    }
                    
                } else if (gc.direction.equals("^")) {
                    // apply to previous node instead
                    DagNode prevDagNode = getStartingDagNodeAboveLine(ct.line);
                    if (prevDagNode == null) {
                        throw new IllegalStateException("Could not find previous statement to associate with '^' comment on line " + ct.line);
                    }
                    annotateDag(prevDagNode, gc);
                    dn = null;
                
                } else if (gc.direction.equals("<")) {
                    // apply to previous ast on this line
                    DagNode prevDagNode = getEndDagNodeBeforeLineColumn(ct.line, ct.column);
                    if (prevDagNode == null) {
                        throw new IllegalStateException("Could not find previous statement to associate with '<' comment on line " + ct.line);
                    }
                    annotateDag(prevDagNode, gc);
                    dn = null;
                    
                } else if (gc.direction.equals("v")) {
                    // apply to first node on next line instead
                    nextLineComments.add(gc);
                    nextLineFromLine = ct.line;
                    dn = null;
                    
                } else if (gc.direction.equals(">")) {
                    nextDagComments.add(gc);
                    dn = null;
                    
                } else { 
                    throw new IllegalStateException("Unknown direction '" + gc.direction + "'");
                    
                }
                
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
                dn.classes.add("literal");
                dn.skipNode = true;
                
            }

            if (dn != null && dn != currentLineDn) {
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
        int columnNumber = cu.getColumnNumber(node.getStartPosition());
        
        
        
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
                processCommentsToStatementNode(pdn, lineNumber, columnNumber, null);                    
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
            
            // ok. so.
            // if this line has multiple comments, then attribute them
            // I'd also like to attribute them using some kind of indicator in the comment; e.g. 
            //   gv:^: text the statement that ended above this line
            //   gv:<: text the statement that ended to the left
            //   gv:>: text the statement that starts to the right
            //   gv:v: text the statement that starts below
            // or some other syntax
            // for nested AST/DagNodes, the outermost AST/DagNode that started/ended on that line
            
            if (nextDagComments.size() > 0) {
                for (GvComment gc : nextDagComments) {
                    annotateDag(dn, gc);
                }
                nextDagComments.clear();
            }
            if (nextLineComments.size() > 0 && nextLineFromLine != dn.lineNumber) {
                for (GvComment gc : nextLineComments) {
                    annotateDag(dn, gc);
                }
                nextLineComments.clear();
                nextLineFromLine = 0;
            }
            
            processCommentsToStatementNode(pdn, lineNumber, columnNumber, dn);
            

            /*
            if (lastIdx < comments.size() && comments.get(lastIdx).line == lineNumber) {
                CommentText ct = comments.get(lastIdx);
                dn.keepNode = true; // always keep commented nodes
                
                if (ct instanceof GvComment) {
                    // just undirected comments at the end of the line. probably.
                    
                    GvComment gc = (GvComment) ct;
                    if (Text.isBlank(gc.direction)) {
                        annotateDag(dn, gc); 
                    }
                    // if this comment start col is before columnNumber then it's not for this statement
                    
                    
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
                    dn.classes.add("literal");

                }
                lastIdx++;
            }
            */
            
            // lastDagNode = dn;
        }
        
        
        
        return true;
    }
    
    public void postVisit(ASTNode node) {
        // DagNode pdn = getClosestDagNode(node);

        if (node instanceof Block) {
            DagNode dn = dag.astToDagNode.get(node);
            int endLineNumber = cu.getLineNumber(node.getStartPosition() + node.getLength());
            int columnNumber = cu.getColumnNumber(node.getStartPosition());
            
            // add comments at the end of a block to that block
            processCommentsToStatementNode(dn, endLineNumber, columnNumber, null);
        }
        
        // @TODO if it's the CompilationUnit, check nextLineComments and nextDagComments
        
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