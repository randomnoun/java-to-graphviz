package com.randomnoun.build.javaToGraphviz.astToDag;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleSheet;

import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
import com.randomnoun.build.javaToGraphviz.dom.DagElement;
import com.randomnoun.build.javaToGraphviz.dom.StylesheetApplier;
import com.randomnoun.build.javaToGraphviz.dom.StylesheetApplier.ExceptionErrorHandler;
import com.randomnoun.common.Text;
import com.steadystate.css.parser.CSSOMParser;

public class DagStyleApplier {
    Logger logger = Logger.getLogger(DagStyleApplier.class);
    Dag dag;
    DagSubgraph root;
    
    public DagStyleApplier(Dag dag, DagSubgraph root) {
        this.dag = dag;
        this.root = root;
    }
    

    public DagElement getDagElement(Map<DagNode, DagElement> dagNodesToElements, DagSubgraph sg) {
        DagElement graphEl = new DagElement(sg.container == null ? "graph" : "subgraph", sg, sg.gvAttributes);
        // bodyEl.appendChild(graphEl);
        DagElement graphNodeEl = new DagElement("graphNode", sg, sg.gvAttributes);
        graphEl.appendChild(graphNodeEl);
        DagElement graphEdgeEl = new DagElement("graphEdge", sg, sg.gvAttributes);
        graphEl.appendChild(graphEdgeEl);
        
        // dag.nodes here or sg.nodes ?
        // hmm. depends whether we style nodes based on which subgraph they're in.
        // which I guess might be useful
        
        for (int i = 0; i < sg.nodes.size(); i++) {
            DagNode node = sg.nodes.get(i);
            node.gvStyles = new HashMap<>(); // clear applied styles
            
            DagElement child = new DagElement(node, node.gvAttributes);
            // could add attributes for edges and/or connected nodes here
            graphEl.appendChild(child);
            
            dagNodesToElements.put(node, child);
        }
        // hrm where to we put the edges then ?
        // either they're all top level, or they're grouped with the start node, or they're grouped with the end node.
        // which would allow us to style them based on which subgraph they start in, or which subgraph they end in, respectively.
        // let's group with the start node and see how that goes.
        
        for (int i = 0; i < dag.edges.size(); i++) {
            DagEdge edge = dag.edges.get(i);
            if (sg.nodes.contains(edge.n1)) { 
                
                edge.gvStyles = new HashMap<>(); // clear applied styles
                DagElement child = new DagElement(edge, edge.gvAttributes);
                // edges don't have IDs here but could
                // child.attr("id", edge.name);
                graphEl.appendChild(child);
            }
        }
        return graphEl;
    }
    
    
    /** Apply the rules in the stylesheet to the Dag, and populates the gvStyles 
     * fields on the Dag, DagEdge and DagNode objects.
     * 
     * @param dag
     * @param stylesheet
     * @throws IOException
     */
    public void inlineStyles(CSSStyleSheet stylesheet) throws IOException {

        // construct a DOM, as we need that to be able to apply the CSS rules
        Document document = Document.createShell("");
        Element bodyEl = document.getElementsByTag("body").get(0);
        
        // construct the initial DOM
        Map<DagNode, DagElement> dagNodesToElements = new HashMap<>();

        DagElement graphEl = getDagElement(dagNodesToElements, root);
        bodyEl.appendChild(graphEl);
        
        
        
        /*
        DagElement graphEl = new DagElement("graph", dag, dag.gvAttributes);
        bodyEl.appendChild(graphEl);
        DagElement graphNodeEl = new DagElement("graphNode", dag, dag.gvAttributes);
        graphEl.appendChild(graphNodeEl);
        DagElement graphEdgeEl = new DagElement("graphEdge", dag, dag.gvAttributes);
        graphEl.appendChild(graphEdgeEl);
        for (int i = 0; i < dag.nodes.size(); i++) {
            DagNode node = dag.nodes.get(i);
            node.gvStyles = new HashMap<>(); // clear applied styles
            
            DagElement child = new DagElement(node, node.gvAttributes);
            // could add attributes for edges and/or connected nodes here
            graphEl.appendChild(child);
            
            dagNodesToElements.put(node, child);
        }
        for (int i = 0; i < dag.edges.size(); i++) {
            DagEdge edge = dag.edges.get(i);
            edge.gvStyles = new HashMap<>(); // clear applied styles
            DagElement child = new DagElement(edge, edge.gvAttributes);
            // edges don't have IDs here but could
            // child.attr("id", edge.name);
            graphEl.appendChild(child);
        }
        */
        
        // use the gv-newSubgraph CSS properties to add subgraphs into both the Dag and the DOM
        List<DagElement> subgraphElements = getSubgraphElementsFromStylesheet(document, stylesheet);
        
        // should probably do the idFormat and labelFormat changes here as well
        // so that we only need 2 CSS passes

        // and construct CSS-defined subgraphs
        
        for (int i = 0; i < subgraphElements.size(); i++) {
            DagElement sgEl = subgraphElements.get(i);
            DagNode sgNode = sgEl.dagNode;
            
            // put the subgraph under the node so that 
            //   node.method > subgraph 
            // CSS rules match. will mean we can only style the subgraph, not the nodes in the subgraph, but that seems good enough for me.
            // so not nesting subgraphs in the DOM.
            // but will be nesting subgraphs in the Dag
            DagSubgraph newSg;
            DagSubgraph sg = dag.dagNodeToSubgraph.get(sgNode);
            if (sg == null) {
                throw new IllegalStateException("this shouldn't happen any more");
                // newSg = new DagSubgraph(dag, dag);
                // dag.subgraphs.add(newSg);
            } else {
                newSg = new DagSubgraph(dag, sg);
                sg.subgraphs.add(newSg);
            }
            newSg.line = sgNode.line;
            newSg.gvAttributes = new HashMap<>(sgNode.gvAttributes);
            
            DagElement newSgEl = new DagElement("subgraph", newSg, newSg.gvAttributes);
            sgEl.appendChild(newSgEl);

            
            moveToSubgraph(newSg, newSgEl, dag, dagNodesToElements, sgNode); 
        }
        
        // reapply styles now the DOM contains CSS-defined subgraph elements
        
        applyStyles(document, stylesheet, true);
        
        
        // TODO: arguably now that the node IDs have changed, couple reapply a third time in case there are any
        // ID-specific CSS rules
        // applyStyles(document, stylesheet, dag);
    }
    
    private void setIdLabel(DagNode node) {
        String idFormat = node.gvStyles.get("gv-idFormat");
        String labelFormat = node.gvStyles.get("gv-labelFormat");
        
        Map<String, String> vars = new HashMap<>();
        vars.put("lineNumber", String.valueOf(node.line));
        vars.put("nodeType", node.type);
        vars.put("lastKeepNodeId",  node.lastKeepNode == null ? "" : node.lastKeepNode.name);
        vars.put("className",  node.gvAttributes.get("className") == null ? "" : node.gvAttributes.get("className"));
        vars.put("methodName",  node.gvAttributes.get("methodName") == null ? "" : node.gvAttributes.get("methodName"));
        
        String[] idLabel = getIdLabel(idFormat, labelFormat, node.name, node.label, vars);
        node.name = idLabel[0];
        node.label = idLabel[1];
    }

    private void setIdLabel(DagEdge edge) {
        String labelFormat = edge.gvStyles.get("gv-labelFormat");
        String xlabelFormat = edge.gvStyles.get("gv-xlabelFormat");
        Map<String, String> vars = new HashMap<>();
        vars.put("startNodeId", edge.n1.name);
        vars.put("endNodeId", edge.n2.name);
        vars.put("breakLabel", edge.gvAttributes.get("breakLabel") == null ? "" : edge.gvAttributes.get("breakLabel")); 
        vars.put("continueLabel", edge.gvAttributes.get("continueLabel") == null ? "" : edge.gvAttributes.get("continueLabel"));

        if (edge.label == null && labelFormat != null) {
            if (labelFormat.startsWith("\"") && labelFormat.endsWith("\"")) {
                labelFormat = labelFormat.substring(1, labelFormat.length() - 1);
            } else {
                throw new IllegalArgumentException("invalid gv-labelFormat '" + labelFormat + "'; expected double-quoted string");
            }
            edge.label = Text.substitutePlaceholders(vars, labelFormat).trim();
        }
        if (edge.label == null && xlabelFormat != null) {
            if (xlabelFormat.startsWith("\"") && xlabelFormat.endsWith("\"")) {
                xlabelFormat = xlabelFormat.substring(1, xlabelFormat.length() - 1);
            } else {
                throw new IllegalArgumentException("invalid gv-xlabelFormat '" + xlabelFormat + "'; expected double-quoted string");
            }
            edge.gvAttributes.put("xlabel",  Text.substitutePlaceholders(vars, xlabelFormat).trim());
        }
        
    }

    private void setIdLabel(DagSubgraph sg) {
        String idFormat = sg.gvStyles.get("gv-idFormat");
        String labelFormat = sg.gvStyles.get("gv-labelFormat");
        
        Map<String, String> vars = new HashMap<>();
        vars.put("lineNumber", String.valueOf(sg.line));
        //vars.put("nodeType", sg.type);
        //vars.put("lastKeepNodeId",  sg.lastKeepNode == null ? "" : sg.lastKeepNode.name);
        vars.put("className",  sg.gvAttributes.get("className") == null ? "" : sg.gvAttributes.get("className"));
        vars.put("methodName",  sg.gvAttributes.get("methodName") == null ? "" : sg.gvAttributes.get("methodName"));
        
        String[] idLabel = getIdLabel(idFormat, labelFormat, sg.name, sg.label, vars);
        sg.name = idLabel[0];
        sg.label = idLabel[1];
        
        for (int i=0; i<sg.subgraphs.size(); i++) {
            setIdLabel(sg.subgraphs.get(i));
        }
    }
    
    private String[] getIdLabel(String idFormat, String labelFormat, String id, String label, Map<String, String> vars) {
        String[] result = new String[] { id, label };
        
        // id can refer to label, or label can refer to id, but not both
        boolean idFirst = true; 
        if (labelFormat != null && labelFormat.contains("${label}")) {
            throw new IllegalArgumentException("circular dependency in gv-labelFormat '" + labelFormat + "'");
        } else if (idFormat != null && idFormat.contains("${id}")) {
            throw new IllegalArgumentException("circular dependency in gv-idFormat '" + idFormat + "'");
        } else if (labelFormat != null && idFormat != null && idFormat.contains("${label}") && labelFormat.contains("${id}")) {
            throw new IllegalArgumentException("circular dependency between gv-idFormat '" + idFormat + "' and gv-labelFormat '" + labelFormat + "'");
        } else {
            idFirst = labelFormat == null || !labelFormat.contains("${id}");
        }
        
        for (int j=0; j<2; j++) {
            if ((( j==0 ) == idFirst)  && (id == null && idFormat != null)) {
                if (idFormat.startsWith("\"") && idFormat.endsWith("\"")) {
                    idFormat = idFormat.substring(1, idFormat.length() - 1);
                } else {
                    throw new IllegalArgumentException("invalid gv-idFormat '" + idFormat + "'; expected double-quoted string");
                }
                vars.put("label", label == null ? "NOLABEL" : label );
                result[0] = dag.getUniqueName(Text.substitutePlaceholders(vars, idFormat));
            }
            if ((( j==1 ) == idFirst) && (label == null && labelFormat != null)) {
                if (labelFormat.startsWith("\"") && labelFormat.endsWith("\"")) {
                    labelFormat = labelFormat.substring(1, labelFormat.length() - 1);
                } else {
                    throw new IllegalArgumentException("invalid gv-labelFormat '" + labelFormat + "'; expected double-quoted string");
                }
                vars.put("id",  id == null ? "NOID" : id);
                result[1] = Text.substitutePlaceholders(vars, labelFormat).trim();
            }
        }
        return result;
    }
    
    /** Moves a node into a subgraph (and all that nodes children, and it's childrens children, and it's childrens childrens children and so on and so forth.
     * 
     * <p>This is only going to work if we don't clear the DagNode.children collection when we're pruning nodes, which conveniently it appears we don't do.
     *
     * <p>Will rejig the subgraph hierarchy if a child is already in a subgraph
     * 
     * @param sg
     * @param dag
     * @param node
     */
    public void moveToSubgraph(DagSubgraph sg, DagElement sgEl, Dag dag,  Map<DagNode, DagElement> dagNodesToElements, DagNode node) {
        if (dag.dagNodeToSubgraph.containsKey(node)) {
            // throw new IllegalStateException("subgraph already contains node");
            logger.warn("moving node to new subgraph");
            dag.dagNodeToSubgraph.get(node).nodes.remove(node);
        }
        sg.nodes.add(node);
        dag.dagNodeToSubgraph.put(node, sg);

        for (DagNode child : node.children) {
            // if they're already in a subgraph, that subgraph should become a subgraph of this subgraph.
            // this should always be an immediate subgraph as we're traversing this in depth-first order
            // a node can only be in 1 subgraph at a time.
            
            DagSubgraph ssg = dag.dagNodeToSubgraph.get(child);
            moveToSubgraph(sg, sgEl, dag, dagNodesToElements, child);
            
            // @TODO maybe this still needs to happen
            /*
            if (ssg != null) {
                if (ssg.container == sg) {
                    logger.warn("we're already in the right subgraph");
                } else {
                    logger.warn("rejigging subgraphs"); // hoist the rigging
                    ssg.container.getSubgraphs().remove(ssg);
                    sg.subgraphs.add(ssg);
                }
            } else {
                moveToSubgraph(sg, sgEl, dag, dagNodesToElements, child);
            }
            */
        }
        
    }
    
    
    private List<DagElement> getSubgraphElementsFromStylesheet(Document document, CSSStyleSheet stylesheet) {
        logger.info("gsefs before styles: " + document.toString());
        StylesheetApplier applier = new StylesheetApplier(document, stylesheet);
        applier.apply();
        logger.info("gsefs after styles: " + document.toString());

        List<DagElement> result = new ArrayList<>();
        
        // copy the calculated styles from the DOM back into the gvStyles field
        final CSSOMParser inlineParser = new CSSOMParser();
        inlineParser.setErrorHandler( new ExceptionErrorHandler() );
        NodeTraversor.traverse( new NodeVisitor() {
            @Override
            public void head( Node node, int depth ) {
                if ( node instanceof DagElement && node.hasAttr( "style" ) ) {
                    // parse the CSS into a CSSStyleDeclaration
                    InputSource input = new InputSource( new StringReader( node.attr( "style" ) ) );
                    CSSStyleDeclaration declaration = null;
                    try {
                        declaration = inlineParser.parseStyleDeclaration( input );
                    } catch ( IOException e ) {
                        throw new IllegalStateException("IOException on string", e);
                    }
                    DagElement dagElement = (DagElement) node;
                    DagNode dagNode = dagElement.dagNode;
                    if (dagNode != null && declaration.getPropertyValue("gv-newSubgraph").equals("true")) {
                        result.add(dagElement);
                    }
                    
                    // @TODO idFormat and labelFormat here ?
                    
                    // reset styles back for the next round
                    if (node.hasAttr("original-Style")) {
                        node.attr("style", node.attr("original-style"));
                    } else {
                        node.removeAttr("style");
                    }
                }
            }

            @Override
            public void tail( Node node, int depth ) {}
        }, document.body() );
        return result;
    }
    
    private void applyStyles(Document document, CSSStyleSheet stylesheet, boolean setIds) {
        //dag.gvStyles = new HashMap<>(); // clear applied styles
        //dag.gvNodeStyles = new HashMap<>(); // clear applied styles
        //dag.gvEdgeStyles = new HashMap<>(); // clear applied styles
        
        logger.info("before styles: " + document.toString());
        StylesheetApplier applier = new StylesheetApplier(document, stylesheet);
        applier.apply();
        logger.info("after styles: " + document.toString());

        // copy the calculated styles from the DOM back into the gvStyles field
        final CSSOMParser inlineParser = new CSSOMParser();
        inlineParser.setErrorHandler( new ExceptionErrorHandler() );
        NodeTraversor.traverse( new NodeVisitor() {
            @Override
            public void head( Node node, int depth ) {
                if ( node instanceof DagElement && node.hasAttr( "style" ) ) {
                    // parse the CSS into a CSSStyleDeclaration
                    InputSource input = new InputSource( new StringReader( node.attr( "style" ) ) );
                    CSSStyleDeclaration declaration = null;
                    try {
                        declaration = inlineParser.parseStyleDeclaration( input );
                    } catch ( IOException e ) {
                        throw new IllegalStateException("IOException on string", e);
                    }
                    DagElement dagElement = (DagElement) node;
                    String tagName = ((Element) node).tagName();
                    // Dag dag = dagElement.dag;
                    DagNode dagNode = dagElement.dagNode;
                    DagEdge dagEdge = dagElement.dagEdge;
                    DagSubgraph dagSubgraph = dagElement.dagSubgraph;
                    if (dagSubgraph != null) {
                        for (int i=0; i<declaration.getLength(); i++) {
                            String prop = declaration.item(i);
                            if (tagName.equals("graph") || tagName.equals("subgraph")) {
                                logger.info("setting graph prop " + prop + " to " + declaration.getPropertyValue(prop));
                                dagSubgraph.gvStyles.put(prop,  declaration.getPropertyValue(prop));
                                if (setIds) {
                                    setIdLabel(dagSubgraph);
                                }

                            } else if (tagName.equals("graphNode")) {
                                logger.info("setting graphNode prop " + prop + " to " + declaration.getPropertyValue(prop));
                                dagSubgraph.gvNodeStyles.put(prop,  declaration.getPropertyValue(prop));
                            } else if (tagName.equals("graphEdge")) {
                                logger.info("setting graphEdge prop " + prop + " to " + declaration.getPropertyValue(prop));
                                dagSubgraph.gvEdgeStyles.put(prop,  declaration.getPropertyValue(prop));
                            }
                        }
                    
                    } else if (dagNode != null) {
                        for (int i=0; i<declaration.getLength(); i++) {
                            String prop = declaration.item(i);
                            logger.info("setting " + dagNode.name + " prop " + prop + " to " + declaration.getPropertyValue(prop));
                            dagNode.gvStyles.put(prop,  declaration.getPropertyValue(prop));
                            /*
                            if (prop.startsWith("gv-")) {
                                String newProp = prop.substring(3);
                                logger.info("setting prop " + newProp + " to " + declaration.getPropertyValue(prop));
                                dagNode.gvAttributes.put(newProp,  declaration.getPropertyValue(prop));
                            }
                            */
                        }
                        if (setIds) {
                            setIdLabel(dagNode);
                        }
                    } else if (dagEdge != null) {
                        for (int i=0; i<declaration.getLength(); i++) {
                            String prop = declaration.item(i);
                            logger.info("setting dagEdge prop " + prop + " to " + declaration.getPropertyValue(prop));
                            dagEdge.gvStyles.put(prop,  declaration.getPropertyValue(prop));
                        }
                        if (setIds) {
                            setIdLabel(dagEdge);
                        }
                        
                    } /* else if (dagSubgraph != null) {
                        for (int i=0; i<declaration.getLength(); i++) {
                            String prop = declaration.item(i);
                            logger.info("setting sg " + dagSubgraph.name + " prop " + prop + " to " + declaration.getPropertyValue(prop));
                            dagSubgraph.gvStyles.put(prop,  declaration.getPropertyValue(prop));
                        }
                    } */
                }
            }

            @Override
            public void tail( Node node, int depth ) {}
        }, document.body() );
        
    }

}