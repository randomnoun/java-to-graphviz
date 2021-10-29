package com.randomnoun.build.javaToGraphviz.comment;

import java.util.List;

import org.eclipse.jdt.core.dom.Comment;

public class GvSubgraphComment extends CommentText {
    public List<String> classes;
    public String id;
    public String inlineStyleString;
    public GvSubgraphComment(Comment c, int line, String id, List<String> classes, String text, String inlineStyleString) {
        super(c, line, text);
        this.id = id;
        this.classes = classes;
        this.inlineStyleString = inlineStyleString;
    }

}