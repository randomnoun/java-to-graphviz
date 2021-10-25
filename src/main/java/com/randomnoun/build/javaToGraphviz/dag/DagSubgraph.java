package com.randomnoun.build.javaToGraphviz.dag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.randomnoun.common.Text;

// in this data model, a subgraph is just used to group nodes together
// (those nodes must already exist at the Dag level)
public class DagSubgraph implements SubgraphHolder {
    public Dag dag; // top-level dag
    public SubgraphHolder container;
    
    public String name;  // graphviz name
    public String label; // graphviz label
    public int line;
    
    // graphviz formatting attributes for the digraph
    public Map<String, String> gvAttributes = new HashMap<>();
    public Set<String> classes = new HashSet<>();
    
    // styles after css rules have been applied
    public Map<String, String> gvStyles = new HashMap<>();
    public Map<String, String> gvNodeStyles = new HashMap<>();
    public Map<String, String> gvEdgeStyles = new HashMap<>();

    // nodes that are in this subgraph only (not subsubgraphs)
    public List<DagNode> nodes = new ArrayList<>();

    // subsubgraphs
    public List<DagSubgraph> subgraphs = new ArrayList<>();
    
    public DagSubgraph(Dag dag, SubgraphHolder container) {
        this.dag = dag;
        this.container = container;
    }

    public String toGraphviz(int indent) {
        var s = "";
        for (int i=0; i<indent; i++) { s += " "; }
        String labelText = label == null ? null : Text.replaceString(label, "\"",  "\\\"");
        
        String a = "";
        for (Entry<String, String> e : gvStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  
                a += s + "  " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        
        String result = s + "subgraph " + name + " {\n" +
          (label == null ? "" : s + "  label=\"" + labelText + "\";\n") +
          a;
        result += s + " ";
        for (DagNode dn : nodes) {
            // only draw nodes if they have an edge
            boolean hasEdge = false;
            for (DagEdge e : dag.edges) {
                if (e.n1 == dn || e.n2 == dn) { hasEdge = true; break; }
            }
            if (hasEdge) {
                result += " " + dn.name + ";";
            }
        }
        result += "\n";
        for (DagSubgraph sg : subgraphs) {
            result += sg.toGraphviz(indent + 2);
        }
        result += s + "}\n";
        return result;
    }

    @Override
    public List<DagSubgraph> getSubgraphs() {
        return subgraphs;
    }
    
}