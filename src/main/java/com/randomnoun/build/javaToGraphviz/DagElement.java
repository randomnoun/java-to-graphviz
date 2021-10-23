package com.randomnoun.build.javaToGraphviz;

import java.util.Map;

import org.jsoup.nodes.Element;

import com.randomnoun.build.javaToGraphviz.JavaToGraphviz.Dag;
import com.randomnoun.build.javaToGraphviz.JavaToGraphviz.DagEdge;
import com.randomnoun.build.javaToGraphviz.JavaToGraphviz.DagNode;
import com.randomnoun.common.Text;

public class DagElement extends Element {

    // only one of these will be populated
    Dag dag;
    DagNode dagNode;
    DagEdge dagEdge;
    
    public DagElement(DagNode dagNode, Map<String, String> attributes) {
        super("node");
        attributes.entrySet().stream().forEach(e -> { 
            // this.attributes().add(e.getKey(), e.getValue());
            // 'style' attribute is used in CSS to store actual styles. guess we don't need to do that, do we.
            this.attr("gv-" + e.getKey(), e.getValue());
        }); 
        if (!dagNode.classes.isEmpty()) {
            this.attr("class", Text.join(dagNode.classes, " "));
        }
        this.dagNode = dagNode;
    }

    public DagElement(Dag dag, Map<String, String> attributes) {
        super("digraph");
        attributes.entrySet().stream().forEach(e -> { 
            // this.attributes().add(e.getKey(), e.getValue());
            // 'style' attribute is used in CSS to store actual styles. guess we don't need to do that, do we.
            this.attr("gv-" + e.getKey(), e.getValue());
        }); 
        this.dag = dag;
    }

    public DagElement(DagEdge dagEdge, Map<String, String> attributes) {
        super("edge");
        attributes.entrySet().stream().forEach(e -> { 
            // this.attributes().add(e.getKey(), e.getValue());
            // 'style' attribute is used in CSS to store actual styles. guess we don't need to do that, do we.
            this.attr("gv-" + e.getKey(), e.getValue());
        }); 
        if (!dagEdge.classes.isEmpty()) {
            this.attr("class", Text.join(dagNode.classes, " "));
        }

        this.dagEdge = dagEdge;
    }
    
    

}
