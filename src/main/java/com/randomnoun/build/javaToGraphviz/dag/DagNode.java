package com.randomnoun.build.javaToGraphviz.dag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;

import com.randomnoun.build.javaToGraphviz.comment.GvComment;
import com.randomnoun.common.Text;

public class DagNode {
    
    public DagNode parentDagNode;
    public List<DagNode> children = new ArrayList<>();
    
    public boolean keepNode = false;
    public boolean skipNode = false; // true for literal nodes only
    public DagNode lastKeepNode = null;
    public List<DagEdge> inEdges = new ArrayList<>();
    public List<DagEdge> outEdges = new ArrayList<>();

    // graphviz formatting attributes
    public Map<String, String> gvAttributes = new HashMap<>();
    public Set<String> classes = new HashSet<>();
    
    // styles after css rules have been applied
    // clear this after every diagram is generated
    public Map<String, String> gvStyles = new HashMap<>();
    
    public ASTNode astNode;
    public String type;      // astNode class, or one of a couple of extra artificial node types (comment, doExpression)
    public String javaLabel; // if this is a labeledStatement, the name of that label 
    public int lineNumber;
    
    public String name;  // graphviz name
    public String label; // graphviz label
    public boolean hasComment;
    public List<GvComment> gvComments = new ArrayList<>(); // in case we need to bubble these up the AST. although what are the chances of that. I mean really.
    
    public void addChild(DagNode node) {
        children.add(node);
    }
    
    public String wrapLabel(String label) {
        String wordwrapString = gvStyles.get("gv-wordwrap");
        String result = "";
        if (label == null) { label = "NOLABEL"; } // should never happen
        if (wordwrapString == null) {
            result = label.trim();
        } else {
            long wordwrap = Long.parseLong(wordwrapString);
            // convert \ns back to newlines, maybe. 
            String[] words = label.trim().split("\\s+");
            
            boolean firstWord = true;
            int lineLen = 0;
            for (int i = 0 ; i < words.length; i++) {
                String word = words[i];
                if (lineLen + word.length() > wordwrap && !firstWord) {
                    result += "\\n" + word; 
                    lineLen = word.length();
                } else {
                    result += " " + word;
                    lineLen += word.length();
                }
                firstWord = false;
            }
        }
        return Text.replaceString(result, "\"",  "\\\"");
    }
    
    public String toGraphviz(boolean isDebugLabel) {
        String labelText = 
            (isDebugLabel ? lineNumber + ": " : "") + wrapLabel(label) +
            (isDebugLabel && lastKeepNode != null ? ", lkn=" + lastKeepNode.name : ""); 
        String a = "";
        for (Entry<String, String> e : gvStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  // gv-wordwrap
                a += "    " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        
        return
          "  " + name + " [\n" +
          // (classes.size() == 0 ? "" : "  /* " + classes + " */\n") +
          // undocumented 'class' attribute is included in graphviz svg output; see
          // https://stackoverflow.com/questions/47146706/how-do-i-associate-svg-elements-generated-by-graphviz-to-elements-in-the-dot-sou
          (classes.size() == 0 ? "" : "    class = \"" + Text.join(classes,  " ") + "\";\n") + 
          "    label = \"" + labelText + "\";\n" + 
          a + 
          "  ];";	        
    }        

}