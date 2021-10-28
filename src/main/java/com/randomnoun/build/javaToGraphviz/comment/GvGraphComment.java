package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

public class GvDigraphComment extends CommentText {
    public GvDigraphComment(Comment c, int line, String text) {
        super(c, line, text);
    }
}