package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

public class GvOptionComment extends CommentText {

    public GvOptionComment(Comment c, int line, int column, boolean eolComment, String text) {
        super(c, line, column, eolComment, text);
    }

}