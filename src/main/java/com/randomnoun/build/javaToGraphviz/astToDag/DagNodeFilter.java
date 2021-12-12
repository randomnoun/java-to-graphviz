package com.randomnoun.build.javaToGraphviz.astToDag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
     * 
     * @return true if any removed edge was a back edge
     */
    private boolean pruneEdges(DagEdge e, DagNode fromNode, DagNode keepNode) {
        boolean result = false;
        if (e.n1 == keepNode) {
            result = e.back;
            dag.removeEdge(e);
            return result;
        } else if (e.n1 == null) {
            throw new IllegalStateException("e.n1 was null");
        } else {
            DagNode prevNode = e.n1;
            // logger.info("removing edge from " + prevNode.name + " to " + e.n2.name);
            result = e.back;
            dag.removeEdge(e);
            
            // prevNode may have already been removed
            if (!prevNode.keepNode) { // dag.nodes.contains(prevNode) && 
                // logger.info("removing node " + prevNode.name + " with " + prevNode.inEdges.size() + " inEdges");
                // removes it from the dag but not from DagNode.children; maybe it should
                dag.nodes.remove(prevNode);
                for (DagEdge prevEdge : new ArrayList<DagEdge>(prevNode.inEdges)) { // may be modified whilst iterating ?
                    if (dag.edges.contains(prevEdge)) { // prevNode.inEdges
                        result = result || pruneEdges(prevEdge, fromNode, keepNode);
                    }
                }
            }
            return result;
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
    
    // this will probably overflow the stack
    // and will removeNodes() the same nodes over and over again
    // maybe keep a stack of nodes to process and iterate over that instead.
    
	private void removeNodes(DagNode node, boolean mergeEdges) { // Set<DagNode> seenNodes
	    
        Set<DagNode> redoNodes = new LinkedHashSet<>();
        redoNodes.add(node);
        
        while (!redoNodes.isEmpty()) {
            node = redoNodes.iterator().next();
            redoNodes.remove(node);
            if (!dag.nodes.contains(node)) { continue; } // already removed

            List<DagEdge> inEdges = node.inEdges;
            List<DagEdge> outEdges = node.outEdges;

            if (mergeEdges && inEdges.size() > 0) {
                
                // if multiple in edges share a lastKeepNode then remove everything back to that keepNode
                // and merge the edges
                for (int i = 0; i < inEdges.size() - 1; i++) {
                    for (int j = i + 1; j < inEdges.size(); j++) {
                        DagEdge inEdge1 = inEdges.get(i);
                        DagEdge inEdge2 = inEdges.get(j);
                        DagNode inEdge1KeepNode = inEdge1.n1.lastKeepNode;
                        DagNode inEdge2KeepNode = inEdge2.n1.lastKeepNode;
                        // if inEdge1KeepNode == null for both, then could remove diagram above this node ?
                        if (inEdge1KeepNode == inEdge2KeepNode && inEdge1KeepNode != null) {
                            logger.info("on node " + node.name + ", merged edges back to " + inEdge1KeepNode.name + ", i=" + i + ", j=" + j);
                            DagEdge newEdge = new DagEdge();
                            newEdge.n1 = inEdge1KeepNode;
                            newEdge.n2 = node;
                            // newEdge.label = "something";
                            // format the edge a bit ?
                            
                            boolean prunedBackEdge = false;
                            prunedBackEdge |= pruneEdges(inEdge1, node, inEdge1KeepNode);  
                            prunedBackEdge |= pruneEdges(inEdge2, node, inEdge2KeepNode);
                            newEdge.back = prunedBackEdge;
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
    
            // if there's zero or one edges leading in and zero or one leading out
            // and there's no comment on this node, remove it
            // (and continue tracing from the next node )
            // logger.info("what about " + node.name);
            
            if (inEdges.size() == 0 && outEdges.size() == 1 && !node.keepNode) {
    
                // if this was a rootNode, then the rootNode is now the next node 
                
                DagEdge outEdge = outEdges.get(0);
                DagNode nextNode = outEdge.n2;
                dag.removeEdge(outEdge);
                
                logger.info("removed start node " + node.type + ", " + node.lineNumber + ", " + node.name);
                dag.nodes.remove(node);
                redoNodes.add(nextNode);
    
            } else if (inEdges.size() == 1 && outEdges.size() == 0 && !node.keepNode) {
    
                DagEdge inEdge = inEdges.get(0);
                dag.removeEdge(inEdge);
                
                logger.info("removed terminal node " + node.type + ", " + node.lineNumber + ", " + node.name);
                dag.nodes.remove(node);
    
            } else if (inEdges.size() >= 1 && outEdges.size() == 1 && !node.keepNode) {
                
                // DagEdge inEdge = inEdges.get(0);
                DagEdge outEdge = outEdges.get(0);
    
                for (DagEdge inEdge : new ArrayList<>(inEdges)) {
                    if (inEdge.n1 == null) { continue; } // already removed
                    if (inEdge.n2 == null) { continue; } // already removed
                    if (inEdge.n1 == inEdge.n2) { continue; } // hmm
                    DagEdge newEdge = new DagEdge();
                    newEdge.n1 = inEdge.n1;
                    newEdge.n2 = outEdge.n2;
                    newEdge.label = inEdge.label;
                    newEdge.back = inEdge.back || outEdge.back;
                    // if (newEdge.back) { newEdge.classes.add("back"); }
                    newEdge.classes.addAll(inEdge.classes);
                    newEdge.classes.addAll(outEdge.classes);
                    
                    // if either edge was colored (break edges), the merged edge is as well
                    // String color = inEdge.gvAttributes.get("color") == null ? outEdge.gvAttributes.get("color") : inEdge.gvAttributes.get("color");
                    // if (color != null) { newEdge.gvAttributes.put("color", color); }
                    
                    newEdge.gvAttributes.putAll(inEdge.gvAttributes);
                    newEdge.gvAttributes.putAll(outEdge.gvAttributes);
                
                    if (!dag.hasEdge(newEdge.n1, newEdge.n2)) {
                        dag.addEdge(newEdge);
                    } else {
                        // merge classes, attributes, back flag into the existing edge ?
                    }
                    dag.removeEdge(inEdge);
                }
                
                DagNode nextNode = outEdge.n2;
                dag.removeEdge(outEdge);
                
                logger.info("removed node " + node.type + ", " + node.lineNumber + ", " + node.name);
                dag.nodes.remove(node);
                // start from n1 again as removing this node may make it possible to remove the parent node
                redoNodes.add(nextNode);
            
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
                                redoNodes.add(e.n2);
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
                        redoNodes.add(n);
                    }
                }
            }
            
        }
	    
	}

}