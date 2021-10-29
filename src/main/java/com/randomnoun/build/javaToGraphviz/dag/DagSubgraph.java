package com.randomnoun.build.javaToGraphviz.dag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.randomnoun.common.Text;

// a DagSubgraph is a graphviz graph ( if container == null)
// or a graphviz subgraph 
public class DagSubgraph  {
    public Dag dag; // top-level dag
    public DagSubgraph container; // if null, this is a root graph
    
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
    
    public DagSubgraph(Dag dag, DagSubgraph container) {
        this.dag = dag;
        this.container = container;
    }

    public String toGraphviz(int indent) {
        boolean isRoot = (container == null);
        var s = "";
        for (int i=0; i<indent; i++) { s += " "; }
        String labelText = label == null ? null : Text.replaceString(label, "\"",  "\\\"");

        /*
        String a = "";
        for (Entry<String, String> e : gvStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  
                a += s + "  " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        */
        
        String a = "", na = "", ea = "";
        for (Entry<String, String> e : gvStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  // gv-wordwrap
                a += s + "  " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        for (Entry<String, String> e : gvNodeStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  // gv-wordwrap
                na += s + "    " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        for (Entry<String, String> e : gvEdgeStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  // gv-wordwrap
                ea += s + "    " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        
        // return "digraph G {\n" +
          
        
        
        String result = s +  (isRoot ? "digraph " : "subgraph ") +
          (name == null ? "" : name + " " ) + "{\n" +
          (label == null ? "" : s + "  label=\"" + labelText + "\";\n") +
          (na.equals("") ? "" : "  node [\n" + na + "  ]\n") +
          (ea.equals("") ? "" : "  edge [\n" + ea + "  ]\n") +
          a;
        
        // TODO: if it's a root node, include nodes & edges here
        if (isRoot) {
            for (DagNode node : dag.nodes) {
                // only draw nodes if they have an edge
                boolean hasEdge = false;
                for (DagEdge e : dag.edges) {
                    if (e.n1 == node || e.n2 == node) { hasEdge = true; break; }
                }
                // if (node != methodNode && hasEdge) {
                if (hasEdge) {
                    result += node.toGraphviz(false) + "\n"; // isDebugLabel
                }
                // }
            }
            for (DagEdge edge : dag.edges) {
                // if (edge.n1 != methodNode) {
                    result += edge.toGraphviz() + "\n";
                // }
            }            

        } else {
            
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
        }
        
        result += "\n";
        for (DagSubgraph sg : subgraphs) {
            result += sg.toGraphviz(indent + 2);
        }
        result += s + "}\n";
        return result;
    }

    public List<DagSubgraph> getSubgraphs() {
        return subgraphs;
    }
    
}