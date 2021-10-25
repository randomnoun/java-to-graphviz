package com.randomnoun.build.javaToGraphviz.dag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.randomnoun.common.Text;

public class DagEdge {
    public DagNode n1; 
    public DagNode n2;
    public String n1Port;
    public String n2Port;
    public String label;
    public Map<String, String> gvAttributes = new HashMap<>();
    public Set<String> classes = new HashSet<>();
    
    public Map<String, String> gvStyles = new HashMap<>();
    public boolean back = false;

    public String toGraphviz() {
        String labelText = label == null ? null : Text.replaceString(label, "\"",  "\\\"");
        String a = "";
        for (Entry<String, String> e : gvStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  
                a += "    " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }

        return "  " + n1.name + // (n1Port == null ? "" : ":" + n1Port) + 
            " -> " + 
            n2.name + // (n2Port == null ? "" : ":" + n2Port) + 
            (label == null && gvStyles.size() == 0? "" : 
                " [\n" +
                (label == null ? "" : "    label=\"" + labelText + "\";\n") +
                a +
                "  ]") + ";";
    }
}