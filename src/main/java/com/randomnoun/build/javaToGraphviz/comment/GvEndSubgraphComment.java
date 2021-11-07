package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

public class GvEndSubgraphComment extends CommentText {

    public GvEndSubgraphComment(Comment c, int line, int column, boolean eolComment) {
        super(c, line, column, eolComment, null);
    }

}   