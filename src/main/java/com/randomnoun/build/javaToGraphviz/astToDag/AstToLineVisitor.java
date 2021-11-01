package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

import com.randomnoun.build.javaToGraphviz.comment.CommentText;

/** An ASTVisitor that constructs a list of ASTNodes that start / end per line
 * 
 * <p>Which we'll use later on when attributing comments to specific ASTNodes
 * to save having to look ahead in the AST tree
 * 
 * <p>Will I add comments into this list ? Will I indeed.
 */
public class AstToLineVisitor extends ASTVisitor {
    
    Logger logger = Logger.getLogger(AstToLineVisitor.class);
    
    Map<Integer, List<ASTNode>> startLineNumberAstsMap = new LinkedHashMap<>(); // order-preserving
    Map<Integer, List<ASTNode>> endLineNumberAstsMap = new LinkedHashMap<>();
    
    List<CommentText> comments;
    CompilationUnit cu;
    String src;
    boolean includeThrowNode;
    
    public AstToLineVisitor(CompilationUnit cu, String src, List<CommentText> comments, boolean includeThrowNode) {
        super(true);
        this.cu = cu;
        this.comments = comments;
        this.src = src;
        this.includeThrowNode = includeThrowNode;
    }
    
    public Map<Integer, List<ASTNode>> getStartLineNumberAstsMap() { 
        return startLineNumberAstsMap;
    }
    public Map<Integer, List<ASTNode>> getEndLineNumberAstsMap() { 
        return endLineNumberAstsMap;
    }
    
    public boolean preVisit2(ASTNode node) {
        if (node instanceof MethodDeclaration ||
            node instanceof CatchClause ||
            (node instanceof Statement &&
            (includeThrowNode || !(node instanceof ThrowStatement)) )) {
            
            int lineNumber = cu.getLineNumber(node.getStartPosition());
            // int columnNumber = cu.getColumnNumber(node.getStartPosition());
            
            List<ASTNode> lineList = startLineNumberAstsMap.get(lineNumber);
            if (lineList == null) {
                lineList = new ArrayList<>();
                startLineNumberAstsMap.put(lineNumber, lineList);
            }
            lineList.add(node);
        }
        
        return true;
    }
    
    public void postVisit(ASTNode node) {
        if (node instanceof MethodDeclaration ||
            node instanceof CatchClause ||
            (node instanceof Statement &&
            (includeThrowNode || !(node instanceof ThrowStatement)) )) {
            
            int lineNumber = cu.getLineNumber(node.getStartPosition() + node.getLength());
            // int columnNumber = cu.getColumnNumber(node.getStartPosition());
            
            List<ASTNode> lineList = endLineNumberAstsMap.get(lineNumber);
            if (lineList == null) {
                lineList = new ArrayList<>();
                endLineNumberAstsMap.put(lineNumber, lineList);
            }
            lineList.add(node);
        }
    }
    
    
}