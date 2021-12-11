package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import com.randomnoun.build.javaToGraphviz.comment.CommentText;
import com.randomnoun.build.javaToGraphviz.comment.GvComment;
import com.randomnoun.build.javaToGraphviz.comment.GvEndGraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvEndSubgraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvGraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvKeepNodeComment;
import com.randomnoun.build.javaToGraphviz.comment.GvLiteralComment;
import com.randomnoun.build.javaToGraphviz.comment.GvOptionComment;
import com.randomnoun.build.javaToGraphviz.comment.GvSubgraphComment;
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
 * 
 * <p>Actually this is a bit more complicated now, as comments can have directions ('^', '>', 'v', '<') to  
 * give the developer more control over which statement is being annotated.
 */
public class AstToDagVisitor extends ASTVisitor {
    
    Logger logger = Logger.getLogger(AstToDagVisitor.class);
    
    int lastIdx = 0;
    Dag dag;
    DagSubgraph root;
    int rootGraphIdx = 0;
    List<CommentText> comments;
    CompilationUnit cu;
    String src;
    boolean defaultKeepNode;
    int nextLineFromLine = 0;
    List<GvComment> nextLineComments = new ArrayList<>(); 
    List<GvComment> nextDagComments = new ArrayList<>();
    KeepNodeMatcher keepNodeMatcher = null;
    Map<String, String> options = null;
    
    
    // Map of line number -> list of ASTs that start / end on that line
    Map<Integer, List<ASTNode>> startLineNumberAsts;
    Map<Integer, List<ASTNode>> endLineNumberAsts;
    
    
    public AstToDagVisitor(CompilationUnit cu, String src, List<CommentText> comments, boolean defaultKeepNode) {
        super(true);
        this.cu = cu;
        this.comments = comments;
        this.src = src;
        this.defaultKeepNode = defaultKeepNode;
        this.keepNodeMatcher = new KeepNodeMatcher(defaultKeepNode);
        dag = new Dag();
        root = new DagSubgraph(dag, null);
        dag.rootGraphs.add(root);
        
        AstToLineVisitor lv = new AstToLineVisitor(cu, src, comments);
        cu.accept(lv);
        startLineNumberAsts = lv.getStartLineNumberAstsMap();
        endLineNumberAsts = lv.getEndLineNumberAstsMap();
        
        this.options = new HashMap<>();
        options.put("defaultKeepNode", defaultKeepNode ? "true" : "false");
    }
    
    public Dag getDag() { 
        return dag;
    }
    
    public Map<String, String> getOptions() {
        return options;
    }
    
    
    private DagNode getStartingDagNodeAboveLine(int originalLine) {
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
        dn.gvComments.add(gc);
        dn.keepNode = true;
    }
    
    
   
    
    private void processCommentsToTypeOrMethodNode(DagNode pdn, int line, DagNode mn) {
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
            dn.options = options;
            */
            DagNode dn = null;
            if (ct instanceof GvComment) {
                mn.classes.addAll(((GvComment) ct).classes);
                mn.label = ct.text; // last comment wins

            } else if (ct instanceof GvGraphComment) {
                GvGraphComment gc =  ((GvGraphComment) ct);
                // if this is the first root graph defined in this file, replace the existing one
                // to remove the class ast node from the 0th root graph
                if (rootGraphIdx == 0) {
                    dag.clear();
                    root = new DagSubgraph(dag, null);
                    dag.rootGraphs.add(root);
                    
                } else {
                    root = new DagSubgraph(dag, null);
                    dag.rootGraphs.add(root);
                }
                root.name = gc.id;
                root.classes.addAll(gc.classes);
                root.gvAttributes.put("style", gc.inlineStyleString); // append to existing ?
                rootGraphIdx++;

            } else if (ct instanceof GvEndGraphComment) {
                if (rootGraphIdx == 0) {
                    throw new IllegalStateException("gv-endGraph without gv-graph");
                } else {
                    root = null;
                }
            
            } else if (ct instanceof GvSubgraphComment) {
                GvSubgraphComment gvsc = (GvSubgraphComment) ct;
                dn = new DagNode();
                // dn.name = dag.getUniqueName("c_" + ct.line);
                dn.keepNode = true; // always keep gv comments
                dn.type = "comment";
                dn.lineNumber = ct.line;
                dn.classes.add("comment");
                dn.label = ct.text;
                dn.astNode = null;
                dn.options = options;
                dn.keepNodeMatcher = keepNodeMatcher;
                
                dn.name = gvsc.id;
                dn.classes.addAll(gvsc.classes);
                dn.classes.add("beginSubgraph");

            } else if (ct instanceof GvEndSubgraphComment) {
                dn = new DagNode();
                // dn.name = dag.getUniqueName("c_" + ct.line);
                dn.keepNode = true; // always keep gv comments
                dn.type = "comment";
                dn.lineNumber = ct.line;
                dn.classes.add("comment");
                dn.label = ct.text;
                dn.astNode = null;
                dn.options = options;
                dn.keepNodeMatcher = keepNodeMatcher;
                
                dn.classes.add("endSubgraph");

            } else if (ct instanceof GvLiteralComment) {
                logger.warn("gv-literal outside of method");

            } else if (ct instanceof GvKeepNodeComment) {
                GvKeepNodeComment knc =  ((GvKeepNodeComment) ct);
                boolean defaultKeepNode = "true".equals(options.get("defaultKeepNode")); 
                keepNodeMatcher = keepNodeMatcher.getModifiedKeepNodeMatcher(defaultKeepNode, knc.text.trim()); 

            } else if (ct instanceof GvOptionComment) {
                GvOptionComment oc =  ((GvOptionComment) ct);
                options = newOptions(oc.text.trim());

            }
            
            if (dn != null) {
                if (pdn!=null) {
                    if (root == null) { throw new IllegalStateException("gv comment outside of graph"); }
                    dag.addNode(root, dn);
                    pdn.addChild(dn);
                } else {
                    // could add as a root node, but let's see how we go
                    throw new IllegalStateException("null pdn in processCommentsToTypeOrMethodNode");
                }
            }
            
            lastIdx ++; 
        }
    }
    
    private void processCommentsToStatementNode(DagNode pdn, int line, int column, DagNode currentLineDn) {
        // DagNode lastNode = null;
        while (lastIdx < comments.size() && 
            (comments.get(lastIdx).line < line || 
            (comments.get(lastIdx).line == line && comments.get(lastIdx).column < column))) {
            
            CommentText ct = comments.get(lastIdx);
            DagNode dn;
            
            if (currentLineDn == null) {
                dn = new DagNode();
                // dn.name = dag.getUniqueName("c_" + ct.line);
                dn.keepNode = true; // always keep gv comments
                dn.type = "comment";
                dn.lineNumber = ct.line;
                dn.classes.add("comment");
                dn.label = ct.text;
                dn.astNode = null;
                dn.options = options;
                dn.keepNodeMatcher = keepNodeMatcher;
                
            } else {
                dn = currentLineDn;
            }
            
            if (ct instanceof GvComment) {
                GvComment gc = (GvComment) ct;
                if (Text.isBlank(gc.direction)) {
                    DagNode prevDagNode = getStartingDagNodeOnLine(ct.line);
                    if (prevDagNode == null) {
                        // no direction
                        annotateDag(dn, gc);
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
                GvGraphComment gc =  ((GvGraphComment) ct);
                // if this is the first root graph defined in this file, replace the existing one
                // to remove the class ast node from the 0th root graph
                if (rootGraphIdx == 0) {
                    dag.clear();
                    root = new DagSubgraph(dag, null);
                    dag.rootGraphs.add(root);

                } else {
                    root = new DagSubgraph(dag, null);
                    dag.rootGraphs.add(root);
                }
                root.name = gc.id;
                root.classes.addAll(gc.classes);
                root.gvAttributes.put("style", gc.inlineStyleString); // append to existing ?
                rootGraphIdx++;
                dn = null;

            } else if (ct instanceof GvEndGraphComment) {
                if (rootGraphIdx == 0) {
                    throw new IllegalStateException("gv-endGraph without gv-graph");
                } else {
                    root = null;
                }
                dn = null;

            } else if (ct instanceof GvSubgraphComment) {
                GvSubgraphComment gvsc = (GvSubgraphComment) ct;
                dn.name = gvsc.id;
                dn.classes.addAll(gvsc.classes);
                dn.classes.add("beginSubgraph");

            } else if (ct instanceof GvEndSubgraphComment) {
                dn.classes.add("endSubgraph");

            } else if (ct instanceof GvLiteralComment) {
                GvLiteralComment gvlc = (GvLiteralComment) ct;
                dn.classes.add("gv-literal");
                dn.skipNode = true;
             
            } else if (ct instanceof GvKeepNodeComment) {
                GvKeepNodeComment knc =  ((GvKeepNodeComment) ct);
                boolean defaultKeepNode = "true".equals(options.get("defaultKeepNode")); 
                keepNodeMatcher = keepNodeMatcher.getModifiedKeepNodeMatcher(defaultKeepNode, knc.text.trim()); 
                dn = null;

            } else if (ct instanceof GvOptionComment) {
                GvOptionComment oc =  ((GvOptionComment) ct);
                options = newOptions(oc.text.trim());
                dn = null;
            }


            if (dn != null && dn != currentLineDn) {
                if (pdn!=null) {
                    if (root == null) { throw new IllegalStateException("gv comment outside of graph"); }
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
    
    
    




    private Map<String, String> newOptions(String t) {
        // name=value pairs. probably need some quoting rules
        options = new HashMap<>(options); // don't affect nodes with existing options
        
        Pattern optionPattern = Pattern.compile("([a-zA-Z0-9-_]+)\\s*=\\s*([a-zA-Z0-9-_]+)\\s*");
        Matcher m = optionPattern.matcher(t);
        while (m.find()) {
            String k = m.group(1);
            String v = m.group(2);
            if (v.equals("unset")) {
                options.remove(k);
            } else {
                options.put(k, v);
            }
        }
        return options;
    }

    @Override 
    public boolean preVisit2(ASTNode node) {
        DagNode pdn = getClosestDagNodeInRoot(root, node);
        
        int lineNumber = cu.getLineNumber(node.getStartPosition());
        int columnNumber = cu.getColumnNumber(node.getStartPosition());
        
        
        
        // writeCommentsToLine(line);

        if (node instanceof MethodDeclaration ||
            node instanceof CatchClause ||
            node instanceof Statement ||
            node instanceof Expression || // inside ExpressionStatements
            node instanceof VariableDeclaration ||
            node instanceof TypeDeclaration ||
            node instanceof AnonymousClassDeclaration
            ) {
            
            DagNode dn = new DagNode();
            String clazz = Text.getLastComponent(node.getClass().getName());
            if (clazz.endsWith("Statement") && !clazz.equals("ExpressionStatement")) {
                clazz = clazz.substring(0, clazz.length() - 9); // @TODO remove this and update the css to match
            }
            String lowerClass = Text.toFirstLower(clazz);
            
            // ok we treat all Expressions as DagNodes here so we can edge them up later
            // and then if it turns out that we're not edging them, we bubble up any comments associated with
            // those DagNodes to the containing statement.
            
            dn.type = clazz; // "if";
            dn.name = null;
            dn.label = null; // now set by gv-labelFormat
            dn.classes.add(lowerClass);
            
            dn.lineNumber = lineNumber;
            dn.parentDagNode = pdn;
            dn.astNode = node;

            // these comments may start a new graph, when that happens we need to reset the pdn
            int beforeRootGraphIdx = rootGraphIdx;
            if (node instanceof TypeDeclaration) {
                // comments get assigned to this node
                processCommentsToTypeOrMethodNode(pdn, lineNumber, dn); 
            } else if (node instanceof MethodDeclaration) {
                // comments get assigned to this node
                processCommentsToTypeOrMethodNode(pdn, lineNumber, dn);
            } else {
                // within methods, comments can have their own nodes
                processCommentsToStatementNode(pdn, lineNumber, columnNumber, null);                    
            }
            if (rootGraphIdx != beforeRootGraphIdx) {
                // new graph, this node is a root node
                pdn = null;
            }

            // options and keepNodeMatcher may be affected by gv comments
            dn.options = options;
            dn.keepNodeMatcher = keepNodeMatcher;

            if (keepNodeMatcher.matches(lowerClass)) {
                dn.keepNode = true;
            }
            
            // omit nodes outside of graphs
            if (root != null) {
                dag.addNode(root, dn);
                if (pdn!=null) {
                    pdn.addChild(dn);
                } else {
                    logger.debug("preVisit: null pdn on " + dn.type + " on line " + dn.lineNumber);
                    // each typeDeclaration is it's own subgraph
                    
                    // think these need to go with the rootGraph rather than the dag
                    // actually maybe there should be a list of dags rather than a list of subgraphs
                    
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
            }

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

    @Override
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
    private DagNode getClosestDagNodeInRoot(DagSubgraph root, ASTNode node) {
        while (node != null) {
            if (dag.astToDagNode.get(node) != null) {
                DagNode dagNode = dag.astToDagNode.get(node);
                if (root.nodes.contains(dagNode)) {
                    return dagNode;
                } else {
                    return null;
                }
            }
            ASTNode parent = node.getParent();
            node = parent;
        }
        return null;
    }
    
}