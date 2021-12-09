package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The KeepNodeMatcher determines the truthiness of the keepNode field for a node */
public class KeepNodeMatcher implements Cloneable {

    boolean defaultKeepNode; 
    List<String> keepNodeMatch = null;
    
    public KeepNodeMatcher(boolean defaultKeepNode) {
        this.defaultKeepNode = defaultKeepNode;
    }

    public KeepNodeMatcher getModifiedKeepNodeMatcher(boolean defaultKeepNode, String spec) {
        KeepNodeMatcher result = this.clone();
        // @TODO merge this with the existing keepNode settings
        result.defaultKeepNode = defaultKeepNode;
        result.keepNodeMatch = Arrays.asList(spec.split("\\s+"));
        return result;
    }
    
    public KeepNodeMatcher clone() {
        KeepNodeMatcher result = new KeepNodeMatcher(this.defaultKeepNode);
        result.keepNodeMatch = this.keepNodeMatch == null ? null : new ArrayList<>(this.keepNodeMatch);
        return this;
    }
    
    /** Match the 'lowerClass' against this KeepNodeMatcher to see whether to keep the node or not. 
     * For AST nodes, the lowerClass is the class name of the node derived from the nodeType.
     * 
     * @param lowerClass
     * @return
     */
    public boolean matches(String lowerClass) {
        boolean result;
        if (keepNodeMatch != null) {
            if (keepNodeMatch.contains(lowerClass) ||
                keepNodeMatch.contains("+" + lowerClass)) {
                result = true;
            } else if (keepNodeMatch.contains("-" + lowerClass)) {
                result = false;
            } else {
                result = defaultKeepNode;
            }
        } else {
            result = defaultKeepNode;
        }
        return result;
    }
    
}
