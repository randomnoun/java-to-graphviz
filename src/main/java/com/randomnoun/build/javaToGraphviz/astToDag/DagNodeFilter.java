package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;

public class DagNodeFilter {
    
    Logger logger = Logger.getLogger(DagNodeFilter.class);
    Dag dag;
    
    public DagNodeFilter(Dag dag) {
        this.dag = dag;
    }
	    
	
	/** Traces through the DAG, storing in each node the most recently seen node that had 'keepNode' set to true
     * (the 'lastKeepNode').
     *  
     * @param dag dag we're traversing
     * @param node the node in the dag we're up to
     * @param lastKeepNode most recently seen node with keepNode set to true
     */
    public void setLastKeepNode(DagNode node, DagNode lastKeepNode) {
        List<DagEdge> outEdges = node.outEdges;
        if (node.lastKeepNode != null) {
            // rejoined with other edges
            // logger.warn("lastKeepNode already set on node " + node.name + " as " + node.lastKeepNode.name + ", node.keepNode=" + node.keepNode + ", lastKeepNode=" + lastKeepNode.name);
            return;
        } else if (node.keepNode) {
            lastKeepNode = node;
        }
        node.lastKeepNode = lastKeepNode;
        // ok so if we've got any outEdges, follow those
        if (outEdges.size() > 0) {
            for (DagEdge e : outEdges) {
                setLastKeepNode(e.n2, lastKeepNode);
            }
        } else {
            // otherwise descend the AST instead.
            for (DagNode n : node.children) {
                setLastKeepNode(n, lastKeepNode);
            }
        }
    }

    // from the fromNode ?
    // could do something with node styles and edge styles here
    
    /** Prune all edges and nodes from an edge (e) to a keepNode
     * 
     * @param dag
     * @param e
     * @param fromNode
     * @param keepNode
     */
    private void pruneEdges(DagEdge e, DagNode fromNode, DagNode keepNode) {
        if (e.n1 == keepNode) {
            dag.removeEdge(e);
            return;
        } else if (e.n1 == null) {
            throw new IllegalStateException("e.n1 was null");
        } else {
            DagNode prevNode = e.n1;
            // logger.info("removing edge from " + prevNode.name + " to " + e.n2.name);
            dag.removeEdge(e);
            
            // prevNode may have already been removed
            if (!prevNode.keepNode) { // dag.nodes.contains(prevNode) && 
                // logger.info("removing node " + prevNode.name + " with " + prevNode.inEdges.size() + " inEdges");
                // removes it from the dag but not from DagNode.children; maybe it should
                dag.nodes.remove(prevNode);
                for (DagEdge prevEdge : new ArrayList<DagEdge>(prevNode.inEdges)) { // may be modified whilst iterating ?
                    if (dag.edges.contains(prevEdge)) { // prevNode.inEdges
                        pruneEdges(prevEdge, fromNode, keepNode);
                    }
                }
            }
        }
    }

    public void removeNodes(DagNode node) {
        removeNodes(node, false); // , new HashSet<DagNode>()
    }

    // @TODO if the incoming keepNodes are different but share a common ancestor, could 
    // replace one of the edge chains with a single edge
    
    /** Clean up the dag by removing unused edges and nodes.
     * There are two operations performed here:
     * <ul><li>'mergeEdges': if multiple inward edges on a node have the same lastKeepNode, 
     *         then remove everything back to that lastKeepNode
     * <pre>
     *         /-->  B --> C --> D  --\
     *     A  <                        >  H
     *         \-->  E --> F --> G  --/
     *  </pre>
     *  becomes
     *  <pre>
     *       A -> H
     *  </pre>
     *  assuming B through G are not keepNodes
     *   
     * <li>'shortenPath': if a node has 1 inward edge and 1 outward edge, remove the node and merge the edges, i.e.
     * <pre>
     *      A -> B -> C 
     * </pre>
     *  becomes
     *  <pre> 
     *      A -> C
     *  </pre> 
     *  assuming B is not a keepNode
     *  </ul>
     * 
     * @param dag
     * @param node
     * @param mergeEdges
     */
	private void removeNodes(DagNode node, boolean mergeEdges) { // Set<DagNode> seenNodes
	    
        List<DagEdge> inEdges = node.inEdges;
        List<DagEdge> outEdges = node.outEdges;
        Set<DagNode> redoNodes = new HashSet<>();
    
        /*
        if (seenNodes.contains(node)) {
            logger.error("loop detected: seen node " + node.name);
            return;
        } 
        seenNodes.add(node);
        */
        
        if (mergeEdges && inEdges.size() > 0) {
            
            // if multiple in edges share a keepNode then remove everything back to that keepNode
            // and merge the edges
            for (int i = 0; i < inEdges.size() - 1; i++) {
                for (int j = i + 1; j < inEdges.size(); j++) {
                    DagEdge inEdge1 = inEdges.get(i);
                    DagEdge inEdge2 = inEdges.get(j);
                    DagNode inEdge1KeepNode = inEdge1.n1.lastKeepNode;
                    DagNode inEdge2KeepNode = inEdge2.n1.lastKeepNode;
                    if (inEdge1KeepNode == inEdge2KeepNode && inEdge1KeepNode != null) {
                        logger.info("on node " + node.name + ", merged edges back to " + inEdge1KeepNode.name + ", i=" + i + ", j=" + j);
                        DagEdge newEdge = new DagEdge();
                        newEdge.n1 = inEdge1KeepNode;
                        newEdge.n2 = node;
                        newEdge.label = "something";
                        // format the edge a bit ?
                        
                        pruneEdges(inEdge1, node, inEdge1KeepNode);  
                        pruneEdges(inEdge2, node, inEdge2KeepNode); 
                        if ((newEdge.n1 != newEdge.n2) && !dag.hasEdge(newEdge.n1, newEdge.n2)) {
                            dag.addEdge(newEdge);
                        }
                        redoNodes.add(inEdge1KeepNode);
                        
                        // restart from the first inEdge of this node 
                        i=0;
                        j=1;
                    }
                }
            }
            
        }

        // if there's one edge leading in and one leading out
        // and there's no comment on this node, remove it
        // (and continue tracing from the next node )
        // logger.info("what about " + node.name);
        
        if (inEdges.size() == 1 && outEdges.size() == 1 && !node.keepNode) {
            
            DagEdge inEdge = inEdges.get(0);
            DagEdge outEdge = outEdges.get(0);

            DagEdge newEdge = new DagEdge();
            newEdge.n1 = inEdge.n1;
            newEdge.n2 = outEdge.n2;
            newEdge.label = inEdge.label;
            newEdge.back = inEdge.back || outEdge.back;
            if (newEdge.back) { newEdge.classes.add("back"); } 
            
            // if either edge was colored (break edges), the merged edge is as well
            String color = inEdge.gvAttributes.get("color") == null ? outEdge.gvAttributes.get("color") : inEdge.gvAttributes.get("color");
            if (color != null) { newEdge.gvAttributes.put("color", color); }

            // TODO: combine styles, probably
            
            dag.removeEdge(inEdge);
            dag.removeEdge(outEdge);
            if (!dag.hasEdge(newEdge.n1, newEdge.n2)) {
                dag.addEdge(newEdge);
            }
            
            logger.info("removed node " + node.type + ", " + node.lineNumber + ", " + node.name);
            dag.nodes.remove(node);
            // start from n1 again as removing this node may make it possible to remove the parent node
            removeNodes(newEdge.n1, mergeEdges); // seenNodes
        
        } else if (true) { // mergeEdges
            
            // trace down the outEdges
            
            // logger.warn(node.outEdges.size() + " outEdges from node " + node.name);
            for (DagEdge e : new ArrayList<DagEdge>(node.outEdges)) {
                // logger.info("outEdges from " + node.type + ":" + node.line);
                if (dag.edges.contains(e)) {
                    if (dag.nodes.contains(e.n2)) { 
                        if (!e.back) {
                            // don't follow back edges
                            // logger.info("checking " + e.n1.type + ":" + e.n1.line + " -> " + e.n2.type + ":" + e.n2.line + ", " + e.n2.name);
                            removeNodes(e.n2);
                        }
                    } else {
                        logger.warn("subnode " + e.n2.name + " missing from " + node.name);
                        dag.removeEdge(e);
                    }
                } else {
                    // logger.warn("edge " + e + " missing from " + node.name);
                }
            }
            
            // if there's no outEdges or inEdges, check the children.
            if (node.inEdges.size() == 0 && node.outEdges.size() == 0) {
                for (DagNode n : node.children) {
                    removeNodes(n);
                }
            }
        }
        
        // if we've merged edges, may need to shorten paths again from the keepNodes
        for (DagNode redo : redoNodes) {
            logger.info("redoing " + node.name);
            removeNodes(redo, false); // false = shorten only
        }
	    
	}

}