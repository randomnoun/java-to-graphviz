package com.randomnoun.build.javaToGraphviz.comment;

import java.util.List;

import org.eclipse.jdt.core.dom.Comment;

public class GvEndSubgraphComment extends CommentText {

    public GvEndSubgraphComment(Comment c, int line) {
        super(c, line, null);
    }

}