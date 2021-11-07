package com.randomnoun.build.javaToGraphviz.comment;

import java.util.List;

import org.eclipse.jdt.core.dom.Comment;

public class GvComment extends CommentText {
    public String id;
    public List<String> classes;
    public String direction;
    public String inlineStyleString;
    public GvComment(Comment c, int line, int column, boolean eolComment, String id, List<String> classes, String direction, String text, String inlineStyleString) {
        super(c, line, column, eolComment, text);
        this.id = id;
        this.classes = classes;
        this.direction = direction;
        this.inlineStyleString = inlineStyleString;
    }
}