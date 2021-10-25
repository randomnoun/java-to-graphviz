package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

/** A source-code comment */
public class CommentText {
    public Comment comment;
    public int line;
    
    public String text;
    
    public CommentText(Comment c, int line, String text) {
        this.comment = c;
        this.line = line;
        this.text = text;
    }
}