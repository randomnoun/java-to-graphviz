package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

/** A source-code comment.
 * 
 * <p>The AST object for a comment is {@link org.eclipse.jdt.core.dom.Comment}, but those are a bit painful 
 * to use as they don't contain the comment text, and they're not in the CompilationUnit AST graph.
 * 
 * <p>We convert these to CommentText objects (or, if the comment begins with "gv", a subclass of CommentText),
 * and associate these with the AST node that the comment is commenting on.
 */
public class CommentText {
    public Comment comment;
    public int line;
    public int column;
    boolean eolComment;
    public String text;
    
    public CommentText(Comment c, int line, int column, boolean eolComment, String text) {
        this.comment = c;
        this.line = line;
        this.column = column;
        this.eolComment = eolComment; 
        this.text = text;
    }
}