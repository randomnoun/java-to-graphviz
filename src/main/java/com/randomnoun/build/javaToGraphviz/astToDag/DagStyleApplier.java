package com.randomnoun.build.javaToGraphviz.astToDag;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
    Document document;
    Map<DagNode, DagElement> dagNodesToElements = new HashMap<>();
    
    // 'type' is a var in cast & instanceof expressions, but might move that into it's own DagNode later
    public static String[] NODE_LABEL_VARIABLES = new String[] { "className", "methodName", "name", 
        "operatorToken", "operatorName", "type", "variableName", "literalValue", "fieldName" };
    
    
    public static String[] EDGE_LABEL_VARIABLES = new String[] { "breakLabel", "continueLabel" };
    
    public DagStyleApplier(Dag dag, DagSubgraph root) {
        this.dag = dag;
        this.root = root;
    }
    
    public Document createDom() {
        // construct a DOM, as we need that to be able to apply the CSS rules
        document = Document.createShell("");
        Element bodyEl = document.getElementsByTag("body").get(0);
        
        // construct the initial DOM
        DagElement graphEl = getDagElement(dagNodesToElements, root);
        bodyEl.appendChild(graphEl);
        return document;
    }
    
    public Document getDocument() { 
        return document;
    }
    
    
    /** Apply the rules in the stylesheet to the Dag, and populates the gvStyles 
     * fields on the Dag, DagEdge and DagNode objects.
     * 
     * @param dag
     * @param stylesheet
     * @throws IOException
     */
    public void inlineStyles(CSSStyleSheet stylesheet) throws IOException {

        // OK so before we create the subgraphs, create dag edges for things that are enabled by style properties
        // applyDomStyles(document, stylesheet);
        // List<DagElement> elementsToExpandInExcruciatingDetail = getElementsWithStyleProperty(document, "gv-fluent", "true"); // enable method nodes

        
        // use the gv-newSubgraph CSS properties to add subgraphs into both the Dag and the DOM
        resetDomStyles(document);
        applyDomStyles(document, stylesheet);
        List<DagElement> elementsToCreateSubgraphsInside = getElementsWithStyleProperty(document, "gv-newSubgraph", "true");
        List<DagElement> elementsToCreateSubgraphsInside2 = getElementsWithStyleProperty(document, "gv-ohAndTheEdgesConnectToTheSubgraph", "true");
        
        List<DagElement> elementsToCreateSubgraphsFrom = getElementsWithStyleProperty(document, "gv-beginOuterSubgraph", "true");
        List<DagElement> elementsToCreateSubgraphsTo = getElementsWithStyleProperty(document, "gv-endOuterSubgraph", "true");

        
        
        // and construct CSS-defined subgraphs. gv-newSubgraph creates subgraphs under the node so that 
        //   node.method > subgraph 
        // CSS rules match. 
        for (int i = 0; i < elementsToCreateSubgraphsInside.size(); i++) {
            DagElement outsideEl = elementsToCreateSubgraphsInside.get(i);
            DagNode outsideNode = outsideEl.dagNode;
            
            
            DagSubgraph newSg;
            DagSubgraph sg = dag.dagNodeToSubgraph.get(outsideNode);
            if (sg == null) {
                throw new IllegalStateException("this shouldn't happen any more");
            } else {
                newSg = new DagSubgraph(dag, sg);
                sg.subgraphs.add(newSg);
            }
            newSg.lineNumber = outsideNode.lineNumber;
            newSg.gvAttributes = new HashMap<>(outsideNode.gvAttributes);
            
            // moves the nodes in the dom
            DagElement newSgEl = new DagElement("subgraph", newSg, newSg.gvAttributes);
            while (outsideEl.childrenSize() > 0) {
                Element c = outsideEl.child(0);
                c.remove();
                newSgEl.appendChild(c);
            }
            outsideEl.appendChild(newSgEl);

            // moves the nodes in the dag subgraphs
            moveToSubgraph(newSg, newSgEl, dag, dagNodesToElements, outsideNode);
            
            if (elementsToCreateSubgraphsInside2.contains(outsideEl)) {
                // outsideEl.addClass("red");
                // outsideNode.classes.add("red");
                List<DagEdge> inEdges = new ArrayList<>();
                List<DagEdge> outEdges = new ArrayList<>();
                
                // don't need to check subgraphs of subgraphs
                // actually maybe I do now
                for (DagEdge e : dag.edges) {
                    if (newSg.nodes.contains(e.n1) && !newSg.nodes.contains(e.n2)) { outEdges.add(e); }
                    if (newSg.nodes.contains(e.n2) && !newSg.nodes.contains(e.n1)) { inEdges.add(e); }
                }
                for (DagEdge e : outEdges) {
                    // e.n1 = newSg; // needs to be node -> node, not subgraph -> node
                    // instead, outgoing edges need 'ltail' with the name of the source subgraph
                    e.gvObjectStyles.put("ltail", newSg); // don't know the name of this subgraph yet
                }
                for (DagEdge e : inEdges) {
                    e.gvObjectStyles.put("lhead", newSg); // don't know the name of this subgraph yet
                }
                
            }
        }
        
        
        // gv-beginOuterSubgraph creates subgraphs outside the node so that 
        //   subgraph:has(> node.method) 
        // CSS rules match
        
        for (int i = 0; i < elementsToCreateSubgraphsFrom.size(); i++) {
            DagElement fromEl = elementsToCreateSubgraphsFrom.get(i);
            Element fromParentEl = fromEl.parent();
            DagNode fromNode = fromEl.dagNode;
            
            DagSubgraph newSg;
            DagSubgraph sg = dag.dagNodeToSubgraph.get(fromNode);
            if (sg == null) {
                throw new IllegalStateException("this shouldn't happen any more");
            } else {
                newSg = new DagSubgraph(dag, sg);
                sg.subgraphs.add(newSg);
            }
            newSg.lineNumber = fromNode.lineNumber;
            newSg.gvAttributes = new HashMap<>(fromNode.gvAttributes);
            
            // moves the nodes in the dom
            DagElement newSgEl = new DagElement("subgraph", newSg, newSg.gvAttributes);
            
            int idx = fromEl.elementSiblingIndex();
            boolean done = false;
            while (idx < fromParentEl.childrenSize() && !done) {
                
                // TODO: could probably remove the from and to nodes completely, but then I'll have to rejig the edges, 
                // although they should always be straight-through so that should be easy enough
                // and then apply the 'from' styles to the newly-created subgraph
                
                DagElement c = (DagElement) fromParentEl.child(idx);
                if (elementsToCreateSubgraphsTo.contains(c)) {
                    elementsToCreateSubgraphsTo.remove(c);
                    done = true;
                }
                c.remove();
                newSgEl.appendChild(c);

                // moves the nodes in the dag subgraphs
                if (c.dagNode != null) { // dag subgraphs don't contain edges
                    moveToSubgraph(newSg, newSgEl, dag, dagNodesToElements, c.dagNode);  
                }

            }
            if (!done) {
                logger.warn("gv-subgraph without gv-end, closing subgraph at AST boundary");
            }
            fromParentEl.insertChildren(idx, newSgEl);


        }
        
        if (elementsToCreateSubgraphsTo.size() > 0) {
            throw new IllegalStateException("gv-end without gv-subgraph");
        }
        
        
        // reapply styles now the DOM contains CSS-defined subgraph elements
        resetDomStyles(document);
        applyDomStyles(document, stylesheet);
        setDagStyles(document, stylesheet, true); // true = set IDs
        
        // TODO: arguably now that the node IDs have changed, couple reapply a third time in case there are any
        // ID-specific CSS rules
        // TODO: also recreate element inNodeId, outNodeId inNodeIds, outNodeIds attributes

        
        
        // applyStyles(document, stylesheet, dag);
        
        setDagSubgraphLiterals(document, stylesheet);
        
    }
    

    // recursively within this subgraph only
    public List<DagElement> getDagElements(Map<DagNode, DagElement> dagNodesToElements, DagSubgraph sg, DagNode node) {
        
        List<DagElement> result = new ArrayList<>();
        node.gvStyles = new HashMap<>(); // clear applied styles
        
        boolean isLiteral = node.classes.contains("gv-literal");
        
        DagElement nodeElement = new DagElement(isLiteral ? "literal" : "node", node, node.gvAttributes);
        
        dagNodesToElements.put(node, nodeElement);
        for (int i = 0; i < node.children.size(); i++) {
            DagNode childNode = node.children.get(i);
            if (dagNodesToElements.containsKey(childNode)) {
                // should never happen, but skip it
                logger.warn("repeated node in Dag");
            } else if (!sg.nodes.contains(childNode)) {
                // wrong subgraph, skip it
                
            } else {
                nodeElement.appendChildren(getDagElements(dagNodesToElements, sg, childNode));
            }
        }
        result.add(nodeElement);
        String inNodeIds = null; 
        String outNodeIds = null; 
        Set<String> inNodeClasses = new HashSet<>();
        Set<String> outNodeClasses = new HashSet<>();
        for (int j = 0; j < dag.edges.size(); j++) {  /* TODO: outrageously inefficient */
            DagEdge edge = dag.edges.get(j);
            // if (sg.nodes.contains(edge.n1)) { 
            if (edge.n1 == node) {
                edge.gvStyles = new HashMap<>(); // clear applied styles
                DagElement edgeElement = new DagElement(edge, edge.gvAttributes);
                // edges don't have IDs here but could
                // child.attr("id", edge.name);
                edgeElement.attr("inNodeId", edge.n1.name);
                edgeElement.attr("outNodeId", edge.n2.name);
                edgeElement.attr("inNodeClass", Text.join(edge.n1.classes, " "));
                edgeElement.attr("outNodeClass", Text.join(edge.n2.classes, " "));
                outNodeIds = outNodeIds == null ? edge.n2.name : outNodeIds + " " + edge.n2.name;
                outNodeClasses.addAll(edge.n2.classes);
                result.add(edgeElement);
            }
            if (edge.n2 == node) {
                inNodeIds = inNodeIds == null ? edge.n1.name : inNodeIds + " " + edge.n1.name;
                inNodeClasses.addAll(edge.n1.classes);
            }
        }
        nodeElement.attr("inNodeId", inNodeIds);
        nodeElement.attr("outNodeId", outNodeIds);
        nodeElement.attr("inNodeClass", Text.join(inNodeClasses,  " "));
        nodeElement.attr("outNodeClass", Text.join(outNodeClasses,  " "));
        return result;
    }

    public DagElement getDagElement(Map<DagNode, DagElement> dagNodesToElements, DagSubgraph sg) {
        DagElement graphEl = new DagElement(sg.container == null ? "graph" : "subgraph", sg, sg.gvAttributes);
        // bodyEl.appendChild(graphEl);
        DagElement graphNodeEl = new DagElement("graphNode", sg, sg.gvAttributes);
        graphEl.appendChild(graphNodeEl);
        DagElement graphEdgeEl = new DagElement("graphEdge", sg, sg.gvAttributes);
        graphEl.appendChild(graphEdgeEl);
        
        for (int i = 0; i < sg.nodes.size(); i++) {
            DagNode node = sg.nodes.get(i);
            if (dagNodesToElements.containsKey(node)) {
                // skip it
            } else {
                graphEl.appendChildren(getDagElements(dagNodesToElements, sg, node));
                
            }
        }
        
        return graphEl;
    }
    private List<DagElement> getElementsWithStyleProperty(Document document, final String propertyName, final String propertyValue) {
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
                    if (dagNode != null && declaration.getPropertyValue(propertyName).equals(propertyValue)) {
                        result.add(dagElement);
                    }
                }
            }

            @Override
            public void tail( Node node, int depth ) {}
        }, document.body() );
        return result;


    }

    private void setIdLabel(DagNode node) {
        String idFormat = node.gvStyles.get("gv-idFormat");
        String labelFormat = node.gvStyles.get("gv-labelFormat");
        
        Map<String, String> vars = new HashMap<>();
        vars.put("lineNumber", String.valueOf(node.lineNumber));
        vars.put("nodeType", node.type);
        vars.put("lastKeepNodeId",  node.lastKeepNode == null ? "" : node.lastKeepNode.name);
        for (String v : NODE_LABEL_VARIABLES) {
            vars.put(v,  node.gvAttributes.get(v) == null ? "" : node.gvAttributes.get(v));
        }
        
        String[] idLabel = getIdLabel(idFormat, labelFormat, node.name, node.label, vars);
        node.name = idLabel[0];
        node.label = idLabel[1];
    }

    private void setIdLabel(DagEdge edge) {
        String labelFormat = edge.gvStyles.get("gv-labelFormat");
        String xlabelFormat = edge.gvStyles.get("gv-xlabelFormat");
        if ("unset".equals(labelFormat)) { labelFormat = null; }
        if ("unset".equals(xlabelFormat)) { xlabelFormat = null; }
        
        Map<String, String> vars = new HashMap<>();
        vars.put("startNodeId", edge.n1.name);
        vars.put("endNodeId", edge.n2.name);
        for (String v : EDGE_LABEL_VARIABLES) {
            vars.put(v, edge.gvAttributes.get(v) == null ? "" : edge.gvAttributes.get(v));
        }

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
            edge.gvStyles.put("xlabel",  "\"" + Text.substitutePlaceholders(vars, xlabelFormat).trim() + "\"");
        }
        
    }

    private void setIdLabel(DagSubgraph sg) {
        String idFormat = sg.gvStyles.get("gv-idFormat");
        String labelFormat = sg.gvStyles.get("gv-labelFormat");
        

        Map<String, String> vars = new HashMap<>();
        vars.put("lineNumber", String.valueOf(sg.lineNumber));
        //vars.put("nodeType", sg.type);
        //vars.put("lastKeepNodeId",  sg.lastKeepNode == null ? "" : sg.lastKeepNode.name);

        // node vars get copied into subgraph object 
        for (String v : NODE_LABEL_VARIABLES) {
            vars.put(v,  sg.gvAttributes.get(v) == null ? "" : sg.gvAttributes.get(v));
        }
        
        String[] idLabel = getIdLabel(idFormat, labelFormat, sg.name, sg.label, vars);
        sg.name = idLabel[0];
        sg.label = idLabel[1];
        
        for (int i=0; i<sg.subgraphs.size(); i++) {
            setIdLabel(sg.subgraphs.get(i));
        }
    }
    
    private String[] getIdLabel(String idFormat, String labelFormat, String id, String label, Map<String, String> vars) {
        String[] result = new String[] { id, label };
        
        if ("unset".equals(idFormat)) { idFormat = null; }
        if ("unset".equals(labelFormat)) { labelFormat = null; }
        
        // id can refer to label, or label can refer to id, but not both
        boolean idFirst = true; 
        if (labelFormat != null && labelFormat.contains("${label}")) {
            throw new IllegalArgumentException("circular dependency in gv-labelFormat '" + labelFormat + "'");
        } else if (idFormat != null && idFormat.contains("${id}")) {
            throw new IllegalArgumentException("circular dependency in gv-idFormat '" + idFormat + "'");
        } else if (labelFormat != null && idFormat != null && idFormat.contains("${label}") && labelFormat.contains("${id}")) {
            throw new IllegalArgumentException("circular dependency between gv-idFormat '" + idFormat + "' and gv-labelFormat '" + labelFormat + "'");
        } else {
            idFirst = labelFormat == null || labelFormat.contains("${id}");
        }
        
        
        for (int j=0; j<2; j++) {
            if ((( j==0 ) == idFirst)  && (id == null && idFormat != null)) {
                if (idFormat.startsWith("\"") && idFormat.endsWith("\"")) {
                    idFormat = idFormat.substring(1, idFormat.length() - 1);
                } else {
                    throw new IllegalArgumentException("invalid gv-idFormat '" + idFormat + "'; expected double-quoted string");
                }
                vars.put("label", label == null ? "NOLABEL" : label );
                id = dag.getUniqueName(Text.substitutePlaceholders(vars, idFormat));
                result[0] = id;
            }
            if ((( j==1 ) == idFirst) && (label == null && labelFormat != null)) {
                if (labelFormat.startsWith("\"") && labelFormat.endsWith("\"")) {
                    labelFormat = labelFormat.substring(1, labelFormat.length() - 1);
                } else {
                    throw new IllegalArgumentException("invalid gv-labelFormat '" + labelFormat + "'; expected double-quoted string");
                }
                vars.put("id",  id == null ? "NOID" : id);
                label = Text.substitutePlaceholders(vars, labelFormat).trim();
                result[1] = label;
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
            // logger.warn("moving node to new subgraph");
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
    
    private void resetDomStyles(Document document) {
        NodeTraversor.traverse( new NodeVisitor() {
            @Override
            public void head( Node node, int depth ) {
                if ( node instanceof DagElement && node.hasAttr( "style" ) ) {
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
    }
    
    private void applyDomStyles(Document document, CSSStyleSheet stylesheet) {
        logger.debug("before styles: " + document.toString());
        StylesheetApplier applier = new StylesheetApplier(document, stylesheet);
        applier.apply();
        logger.debug("after styles: " + document.toString());
    }
    
    private void setDagStyles(Document document, CSSStyleSheet stylesheet, boolean setIds) {

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
                                logger.debug("setting graph prop " + prop + " to " + declaration.getPropertyValue(prop));
                                dagSubgraph.gvStyles.put(prop,  declaration.getPropertyValue(prop));

                            } else if (tagName.equals("graphNode")) {
                                logger.debug("setting graphNode prop " + prop + " to " + declaration.getPropertyValue(prop));
                                dagSubgraph.gvNodeStyles.put(prop,  declaration.getPropertyValue(prop));
                            } else if (tagName.equals("graphEdge")) {
                                logger.debug("setting graphEdge prop " + prop + " to " + declaration.getPropertyValue(prop));
                                dagSubgraph.gvEdgeStyles.put(prop,  declaration.getPropertyValue(prop));
                            }
                        }
                        if ((tagName.equals("graph") || tagName.equals("subgraph")) && setIds) {
                            setIdLabel(dagSubgraph);
                        }
                    
                    } else if (dagNode != null) {
                        for (int i=0; i<declaration.getLength(); i++) {
                            String prop = declaration.item(i);
                            logger.debug("setting " + dagNode.name + " prop " + prop + " to " + declaration.getPropertyValue(prop));
                            dagNode.gvStyles.put(prop,  declaration.getPropertyValue(prop));
                        }
                        if (setIds) {
                            setIdLabel(dagNode);
                        }
                    } else if (dagEdge != null) {
                        for (int i=0; i<declaration.getLength(); i++) {
                            String prop = declaration.item(i);
                            logger.debug("setting dagEdge prop " + prop + " to " + declaration.getPropertyValue(prop));
                            dagEdge.gvStyles.put(prop,  declaration.getPropertyValue(prop));
                        }
                        if (setIds) {
                            setIdLabel(dagEdge);
                        }
                        
                    } 
                }
            }

            @Override
            public void tail( Node node, int depth ) {}
        }, document.body() );
        
    }

    // copy any literal elements into their containing subgraphs
    private void setDagSubgraphLiterals(Document document, CSSStyleSheet stylesheet) {
        Stack<DagSubgraph> subgraphStack = new Stack<>();
        
        final CSSOMParser inlineParser = new CSSOMParser();
        inlineParser.setErrorHandler( new ExceptionErrorHandler() );
        NodeTraversor.traverse( new NodeVisitor() {
            @Override
            public void head( Node node, int depth ) {
                if ( node instanceof DagElement) {

                    DagElement dagElement = (DagElement) node;
                    String tagName = ((Element) node).tagName();
                    DagSubgraph dagSubgraph = dagElement.dagSubgraph;
                    DagNode dagNode = dagElement.dagNode;
                    if (dagSubgraph != null) {
                        subgraphStack.push(dagSubgraph);
                    }

                    if ("literal".equals(tagName)) {
                        if (subgraphStack.size() == 0) {
                            // could add to root subgraph instead
                            throw new IllegalStateException("literal outside of subgraph");
                        }
                        subgraphStack.peek().literals.add(dagNode.label);
                    }
                }
            }

            @Override
            public void tail( Node node, int depth ) {
                if ( node instanceof DagElement) {
                    DagElement dagElement = (DagElement) node;
                    if (dagElement.dagSubgraph != null) {
                        subgraphStack.pop();
                    }
                }
            }
        }, document.body() );
    }

    
}