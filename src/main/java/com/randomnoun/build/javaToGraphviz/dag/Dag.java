package com.randomnoun.build.javaToGraphviz.dag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;

/** A directed acyclic graph (DAG), used to generate the graphviz diagram.
 * <p>This is two data-structures in one; it mirrors the eclipse AST in a new datastructure that I 
 * have a bit more control over, and it also acts as the source for the graphviz diagram.
 * 
 * <ol><li>The AST structure is defined using the node.children / node.parentDagNode fields in the DagNodes
 *     <ul><li>dag.astToDagNode - a Map from AST nodes -> DAG nodes
 *     <ul><li>node.astNode - the AST Node for this DagNode
 *     </ul>
 *     <li>The diagram is defined using the edges between the visible nodes, which are defined in 
 *     <ul><li>dag.edges - list of all edges
 *         <li>node.inEdges - edges which end in this node (edge.n2 = node)
 *         <li>node.outEdges - edges which start from this node (edge.n1 = node)
 *     </ul>
 * </ol>
 * 
 * <p>Each of the DagNodes also has a DOM node for styling, but I'm keeping that separate for now.
 * 
 * <p>NB: we can make a DAG look like a cyclical graph by reversing the arrows on the diagram, which
 * we might want to do for loops later on. Or I guess we could make it cyclical.
 * 
 * <p>Well, that was the idea, but that's too fiddly to manage, so the DAG is now cyclical.
 * 
 * <p>Okay so now the dag is only a dag if you don't follow back edges ( edge.back == true )
 * 
 */
public class Dag implements SubgraphHolder {
    
    // a digraph.
    // when we're creating clusters we clear the edges each time
    // but probably need to start storing clusters in here as we go, seeing I'm going to want nested clusters soon
    // nodes are in clusters but edges can span clusters
    public Map<ASTNode, DagNode> astToDagNode = new HashMap<>();
    
    public List<DagNode> nodes = new ArrayList<>();
    public List<DagNode> rootNodes = new ArrayList<>(); // subset of nodes which start each Dag tree
    public List<DagEdge> edges = new ArrayList<>();
    public Set<String> names = new HashSet<String>();
    public Logger logger = Logger.getLogger(Dag.class);
    
    // graphviz formatting attributes for the digraph
    public Map<String, String> gvAttributes = new HashMap<>();
    // styles after css rules have been applied
    public Map<String, String> gvStyles = new HashMap<>();
    public Map<String, String> gvNodeStyles = new HashMap<>();
    public Map<String, String> gvEdgeStyles = new HashMap<>();
    
    // OK so I'm aware graphviz can produce different layouts depending on the ordering of nodes and subgraphs, but
    // from my minimal testing it appears the layouts are a bit nicer if all the subgraphs are defined right at the end
    // so that's what I'm going to do.
    
    // NB all nodes and edges are defined in the structures above, these subgraphs *only* hold the subset of those 
    // nodes that are in those subgraphs.
    public Map<DagNode, DagSubgraph> dagNodeToSubgraph = new HashMap<>(); // convenience map for finding nodes already in subgraphs
    public List<DagSubgraph> subgraphs = new ArrayList<>();
    
    @Override
    public List<DagSubgraph> getSubgraphs() {
        return subgraphs;
    }

    
    public void addNode(DagNode n) {
        nodes.add(n);
        astToDagNode.put(n.astNode, n);
    }
    public String toGraphvizHeader() {
        String a = "", na = "", ea = "";
        for (Entry<String, String> e : gvStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  // gv-wordwrap
                a += "  " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        for (Entry<String, String> e : gvNodeStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  // gv-wordwrap
                na += "    " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        for (Entry<String, String> e : gvEdgeStyles.entrySet()) {
            if (!e.getKey().startsWith("gv-")) {  // gv-wordwrap
                ea += "    " + e.getKey() + " = " + e.getValue() + ";\n";
            }
        }
        
        return "digraph G {\n" +
          (na.equals("") ? "" : "  node [\n" + na + "  ]\n") +
          (ea.equals("") ? "" : "  edge [\n" + ea + "  ]\n") +
          a;
    }
    public String toGraphvizFooter() {
        return "}\n";
    }
    
    
    public void addRootNode(DagNode n) {
        rootNodes.add(n);
    }
    public boolean hasEdge(DagNode n1, DagNode n2) {
        for (DagEdge e : edges) {
            if (e.n1 == n1 && e.n2 == n2 && !e.back) { return true; }
        }
        return false;
    }
    public void addEdge(DagEdge e) {
        edges.add(e);
        e.n1.outEdges.add(e);
        e.n2.inEdges.add(e);
    }
    public DagEdge addEdge(DagNode n1, DagNode n2) {
        return addEdge(n1, n2, null);
    }
    public DagEdge addEdge(DagNode n1, DagNode n2, String label) {
        if (n1 == null) { throw new NullPointerException("null n1"); }
        if (n2 == null) { throw new NullPointerException("null n2"); }
        DagEdge e = new DagEdge();
        e.n1 = n1;
        e.n2 = n2;
        e.label = label;
        edges.add(e);
        e.n1.outEdges.add(e);
        e.n2.inEdges.add(e);
        return e;
    }

    // NB back edges means this is no longer a DAG
    // set a 'back' flag on this edge so we don't get into recursive loops
    // also sets port to :e on each endpoint so we don't overlap lines
    public DagEdge addBackEdge(DagNode n1, DagNode n2, String label) {
        if (n1 == null) { throw new NullPointerException("null n1"); }
        if (n2 == null) { throw new NullPointerException("null n2"); }
        DagEdge e = new DagEdge();
        e.n1 = n1; 
        e.n2 = n2;
        e.n1Port = "e"; // eastern side of the node
        e.n2Port = "e";
        e.label = label;
        e.back = true;
        edges.add(e);
        e.n1.outEdges.add(e);
        e.n2.inEdges.add(e);
        // e.gvAttributes.put("dir", "back");
        // e.gvAttributes.put("style", "dashed");
        e.classes.add("back");
        return e;
    }
    
    public void addBackEdge(ExitEdge e) {
        if (e.n1 == null) { throw new NullPointerException("null n1"); }
        if (e.n2 == null) { throw new NullPointerException("null n2"); }
        e.back = true;
        edges.add(e);
    }

    
    public void removeEdge(DagEdge e) {
        if (e.n1 == null) { logger.warn("e.n1 is null"); } else { e.n1.outEdges.remove(e); }
        if (e.n2 == null) { logger.warn("e.n2 is null"); } else { e.n2.inEdges.remove(e); }
        e.n1 = null; e.n2 = null;
        edges.remove(e);
    }
    
    /** Create a unique node name (appending _2, _3 etc suffixes until it's unique).
     * Our node names are basically the line number (badoom boom tish), so this is required when there's >1 statement on a line.
     * 
     * @param n
     * @return
     */
    public String getUniqueName(String n) {
        if (!names.contains(n)) {
            names.add(n); 
            return n;
        }
        int idx = 2;
        while (names.contains(n + "_" + idx)) {
            idx++;
        }
        names.add(n + "_" + idx); 
        return n + "_" + idx;
    }
}