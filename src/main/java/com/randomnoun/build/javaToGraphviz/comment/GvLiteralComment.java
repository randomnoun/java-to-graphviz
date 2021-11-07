package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

public class GvLiteralComment extends CommentText {

    public GvLiteralComment(Comment c, int line, int column, boolean eolComment, String text) {
        super(c, line, column, eolComment, text);
    }

}