package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

public class GvEndGraphComment extends CommentText {

    public GvEndGraphComment(Comment c, int line, int column, boolean eolComment) {
        super(c, line, column, eolComment, null);
    }

}   