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
    public Map<String, Object> gvObjectStyles = new HashMap<>();
    public boolean back = false;

    public String toGraphviz() {
        if (n1==null) { throw new IllegalStateException("null n1"); }
        if (n2==null) { throw new IllegalStateException("null n2"); }
        
        String labelText = label == null ? null : Text.replaceString(label, "\"",  "\\\"");
        String a = "";
        for (Entry<String, String> e : gvStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  
                a += "    " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        // resolve dag objects to names of those objects
        for (Entry<String, Object> e : gvObjectStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {
                if (e.getValue() instanceof DagSubgraph) {
                    a += "    " + e.getKey() + " = " + ((DagSubgraph) e.getValue()).name + ";\n";
                } else {
                    throw new IllegalStateException("unexpected object " + e.getValue().getClass().getName() + " in gvObjectStyles");
                }
            }
        }

        return "  " + n1.name + // (n1Port == null ? "" : ":" + n1Port) + 
            " -> " + 
            n2.name + // (n2Port == null ? "" : ":" + n2Port) + 
            (label == null && a.equals("") ? "" : 
                " [\n" +
                (label == null ? "" : "    label=\"" + labelText + "\";\n") +
                a +
                "  ]") + ";";
    }
}