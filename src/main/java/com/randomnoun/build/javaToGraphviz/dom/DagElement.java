package com.randomnoun.build.javaToGraphviz.dom;

import java.util.Map;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
import com.randomnoun.common.Text;

public class DagElement extends Element {

    // only one of these will be populated
    public Dag dag;
    public DagNode dagNode;
    public DagEdge dagEdge;
    public DagSubgraph dagSubgraph;
    
    public DagElement(DagNode dagNode, Map<String, String> attributes) {
        super("node");
        this.attr("id", dagNode.name);
        this.attr("label", dagNode.label);
        attributes.entrySet().stream().forEach(e -> { 
            this.attr(e.getKey(), e.getValue());
        });
        if (!dagNode.classes.isEmpty()) {
            this.attr("class", Text.join(dagNode.classes, " "));
        }
        if (attributes.containsKey("style")) {
            // keep a copy of this as it may change when we apply the CSS the first time round to get the gv-newSubgraph properties
            this.attr("original-style", attributes.get("style"));
        }
        this.dagNode = dagNode;
    }

    public DagElement(String tagName, Dag dag, Map<String, String> attributes) {
        super(tagName);
        attributes.entrySet().stream().forEach(e -> { 
            this.attr(e.getKey(), e.getValue());
        }); 
        if (attributes.containsKey("style")) {
            // keep a copy of this as it may change when we apply the CSS the first time round to get the gv-newSubgraph properties
            this.attr("original-style", attributes.get("style"));
        }
        this.dag = dag;
    }

    public DagElement(DagEdge dagEdge, Map<String, String> attributes) {
        super("edge");
        attributes.entrySet().stream().forEach(e -> { 
            this.attr(e.getKey(), e.getValue());
        }); 
        if (!dagEdge.classes.isEmpty()) {
            this.attr("class", Text.join(dagEdge.classes, " "));
        }
        if (attributes.containsKey("style")) {
            // keep a copy of this as it may change when we apply the CSS the first time round to get the gv-newSubgraph properties
            this.attr("original-style", attributes.get("style"));
        }
        this.dagEdge = dagEdge;
    }

    // new subgraph elements
    public DagElement(DagSubgraph dagSubgraph, Map<String, String> attributes) {
        super("subgraph");
        if (!dagSubgraph.classes.isEmpty()) {
            this.attr("class", Text.join(dagSubgraph.classes, " "));
        }

        // attributes are null when subgraphs are defined by CSS rules
        if (attributes != null) {
            attributes.entrySet().stream().forEach(e -> { 
                this.attr(e.getKey(), e.getValue());
            }); 
            if (attributes.containsKey("style")) {
                // keep a copy of this as it may change when we apply the CSS the first time round to get the gv-newSubgraph properties
                this.attr("original-style", attributes.get("style"));
            }
        }
        this.dagSubgraph = dagSubgraph;
    }
    
    

}
