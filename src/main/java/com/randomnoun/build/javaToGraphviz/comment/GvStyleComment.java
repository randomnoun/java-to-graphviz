package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

public class GvStyleComment extends CommentText {
    public String style;
    public GvStyleComment(Comment c, int line, String text, String style) {
        super(c, line, text);
        this.style = style;
    }
}