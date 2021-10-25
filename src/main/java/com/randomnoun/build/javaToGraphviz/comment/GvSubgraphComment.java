package com.randomnoun.build.javaToGraphviz.comment;

import org.eclipse.jdt.core.dom.Comment;

public class GvSubgraphComment extends CommentText {
    public GvSubgraphComment(Comment c, int line, String text) {
        super(c, line, text);
    }
}