package com.randomnoun.build.javaToGraphviz.comment;

import java.util.List;

import org.eclipse.jdt.core.dom.Comment;

public class GvComment extends CommentText {
    public List<String> classes;
    public String inlineStyleString;
    public GvComment(Comment c, int line, List<String> classes, String text, String inlineStyleString) {
        super(c, line, text);
        this.classes = classes;
        this.inlineStyleString = inlineStyleString;
    }
}