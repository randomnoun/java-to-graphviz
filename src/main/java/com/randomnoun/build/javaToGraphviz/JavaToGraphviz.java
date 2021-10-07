package com.randomnoun.build.javaToGraphviz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;

// so the next bit is going to be styling
// and supporting more types of statements

/** Convert an AST tree of a java class ( CompilationUnit, created by the eclipse ASTParser )
 * into a graphviz diagram ( Dag )
 * 
 * A complete standalone example of ASTParser
 * @see https://www.programcreek.com/2011/01/a-complete-standalone-example-of-astparser/
 *
 */
public class JavaToGraphviz4 {

    Logger logger = Logger.getLogger(JavaToGraphviz4.class);
    
	
	public void test(InputStream is) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamUtil.copyStream(is, baos);
		
		String src = baos.toString();
		ASTParser parser = ASTParser.newParser(AST.JLS11); // JLS3
		parser.setSource(src.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		
		
		// https://www.programcreek.com/2013/03/get-internal-comments-by-using-eclipse-jdt-astparser/
		// hmm this seems like a weird way of doing things. anyway.
		// CommentVisitor cv = new CommentVisitor(cu, src);
		
		List<CommentText> comments = getComments(cu, src);
		CSSStyleSheet styleSheet = getStyleSheet(comments);
		
		// Comments have an alternoteRoot which links it back to the compilationUnit AST node
        // turns out some astNodes have a leadingComment
        // so would have been good to know about that earlier, but hey, it mostly works now.

		// probably need to construct a DAG now don't I
		
		DagVisitor dv = new DagVisitor(cu, src, comments);
        cu.accept(dv);
        Dag dag = dv.getDag();
        DagNode block = dag.nodes.get(0);
        if (!block.type.equals("Block")) {
            throw new IllegalStateException("first node must be a block; found " + block.type);
        }
        addBlockEdges(dag, block, null, null);
        block.keepNode = true;
        setLastKeepNode(dag, block, block);
        removeNodes(dag, block); // peephole node removal.
        
        
        inlineStyles(dag, styleSheet);
        
        
        
        PrintWriter pw = new PrintWriter(System.out);
        pw.println("digraph G {\r\n" +
            "  graph [fontname = \"Handlee\"];\r\n" +
            "  node [fontname = \"Handlee\"; shape=rect; ];\r\n" +
            "  edge [fontname = \"Handlee\"];\r\n" +
            "\r\n" +
            "  bgcolor=transparent;");
        for (DagNode node : dag.nodes) {
            // only draw nodes if they have an edge
            boolean hasEdge = false;
            for (DagEdge e : dag.edges) {
                if (e.n1 == node || e.n2 == node) { hasEdge = true; break; }
            }
            if (hasEdge) {
                pw.println(node.toGraphviz());
            }
        }
        for (DagEdge edge : dag.edges) {
            pw.println(edge.toGraphviz());
        }
        pw.println("}");
        
        pw.flush();
	}

	// when we construct the DagNodes, automatically add classes based on AST type
	// and line number, which will make colouring these things in from jacoco output that much simpler
	// and whatever JVMTI uses, which is probably line numbers as well
	
    public CSSStyleSheet getStyleSheet(List<CommentText> comments) throws IOException {
        String css = "";
        for (CommentText c : comments) {
            String t = c.text;
            if (t.startsWith("gv.style:")) {
                // remove outer braces
                css = css + t;
            }
        }
        // pre-process the // comments out of the block comments, which isn't standard CSS
        
        CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
        InputSource source = new InputSource(new StringReader(css));
        CSSStyleSheet stylesheet = parser.parseStyleSheet(source, null, null);
        
        CSSStyleSheetImpl stylesheetImpl = (CSSStyleSheetImpl) stylesheet; 
        // probably need to wrap this in something which overrides importImports
        // public void importImports(final boolean recursive) throws DOMException {
        // or provide my own URI handler and set a weird base URI 
        // stylesheetImpl.importImports(true); // recursive = true
        return stylesheetImpl;
    }
    
    List<DagNode> selectDagNodes(Dag dag, String cssSelector) {
        List<DagNode> result = new ArrayList<>();
        for (DagNode n : dag.nodes) {
            if (n.classes.contains(cssSelector)) {
                result.add(n);
            }
        }
        return result;
    }
    
    

	// from http://stackoverflow.com/questions/4521557/automatically-convert-style-sheets-to-inline-style
    public void inlineStyles(Dag dag, CSSStyleSheet stylesheet) throws IOException {

        CSSRuleList ruleList = stylesheet.getCssRules();
        // Map<DagNode, Map<String, String>> allElementsStyles = new HashMap<Element, Map<String, String>>();
        for (int ruleIndex = 0; ruleIndex < ruleList.getLength(); ruleIndex++) {
            CSSRule item = ruleList.item(ruleIndex);
            if (item instanceof CSSStyleRule) {
                CSSStyleRule styleRule = (CSSStyleRule) item;
                String cssSelector = styleRule.getSelectorText(); // if I'm coding this myself, class only 
                // Elements elements = document.select(cssSelector);
                List<DagNode> nodes = selectDagNodes(dag, cssSelector);
                
                for (int i = 0; i < nodes.size(); i++) {
                    DagNode node = nodes.get(i);
                    Map<String, String> gvAttributes = node.gvAttributes;
                    CSSStyleDeclaration style = styleRule.getStyle();
                    for (int propertyIndex = 0; propertyIndex < style.getLength(); propertyIndex++) {
                        String propertyName = style.item(propertyIndex);
                        String propertyValue = style.getPropertyValue(propertyName);
                        gvAttributes.put(propertyName, propertyValue);
                    }
                }
            }
        }

    }
    
    /** Adds the edges for a DagNode into the Dag, and returns the edges leading out of that DagNode
     * (which may now be labelled)
     * 
     * @param dag
     * @param node
     * @param breakEdges if a 'break' is encountered, a collection to add the outgoing edge to
     * @param continueNode if a 'continue' is encountered, the node to continue to
     * @return
     */
	private List<ExitEdge> addEdges(Dag dag, DagNode node, List<ExitEdge> breakEdges, DagNode continueNode) {
	    if (node.type.equals("Block")) {
	        return addBlockEdges(dag, node, breakEdges, continueNode);
	    } else if (node.type.equals("If")) {
	        return addIfEdges(dag, node, breakEdges, continueNode);
        } else if (node.type.equals("Try")) {
            return addTryEdges(dag, node, breakEdges, continueNode);
        } else if (node.type.equals("For")) {
            return addForEdges(dag, node, breakEdges, continueNode);
        } else if (node.type.equals("EnhancedFor")) {
            return addEnhancedForEdges(dag, node, breakEdges, continueNode);
        } else if (node.type.equals("Switch")) {
            return addSwitchEdges(dag, node, breakEdges, continueNode);
        } else if (node.type.equals("SwitchCase")) {
            // hmm. these should be handled in addSwitchEdges
            throw new IllegalStateException("SwitchCase encountered outside Switch");
        } else if (node.type.equals("Break")) {
            return addBreakEdges(dag, node, breakEdges, continueNode);
        } else if (node.type.equals("While")) {
            return addWhileEdges(dag, node, breakEdges, continueNode);
        } else if (node.type.equals("Continue")) {
            return addContinueEdges(dag, node, breakEdges, continueNode);
            
        // @TODO
        // x Break
        // x Continue
        // Do
        // x EnhancedFor
        // x For
        // Return
        // x SwitchCase
        // x Switch
        // Throw
        // While
        
        // goto will be considered tedious if I have to do that. ho ho ho.
            
        } else if (node.type.equals("Assert") ||
          node.type.equals("ConstructorInvocation") ||
          node.type.equals("EmptyStatement") ||
          node.type.equals("Expression") ||
          node.type.equals("LabeledStatement") ||
          node.type.equals("SuperConstructorInvocation") ||
          node.type.equals("Synchronized") || // could be same as Block really
          node.type.equals("TypeDeclaration") ||
          node.type.equals("VariableDeclaration") ||
          node.type.equals("comment")) {
            // non-control flow statement
            ExitEdge e = new ExitEdge();
            e.n1 = node;
            return Collections.singletonList(e);
            
	    } else {
	        logger.warn("non-implemented control flow statement " + node.type);
	        ExitEdge e = new ExitEdge();
	        e.n1 = node;
	        return Collections.singletonList(e);
	    }
	    
	}
	
	// draw lines from each statement to each other
	// exit node is the last statement
	private List<ExitEdge> addBlockEdges(Dag dag, DagNode block, List<ExitEdge> breakEdges, DagNode continueNode) {
        // draw the edges from the block
        
	    ExitEdge start = new ExitEdge();
        start.n1 = block;
	    List<ExitEdge> prevNodes = Collections.singletonList(start);
	    
	    for (DagNode c : block.children) {
	        if (prevNodes != null) {
	            for (ExitEdge e : prevNodes) {
	                e.n2 = c;
	                dag.addEdge(e);
	            }
	            prevNodes = addEdges(dag, c, breakEdges, continueNode);
	        } else {
	            throw new IllegalStateException("I don't think this ever happens");
	            // maybe on the very first block ?
	            
	            // ExitEdge e = new ExitEdge();
	            // e.n1 = c;
	            // prevNodes = Collections.singletonList(e);
	        }
	    }
	    return prevNodes;
    }
	
	// a break will add an edge to breakEdges only 
	// (and returns an empty list as we won't have a normal exit edge)
	private List<ExitEdge> addBreakEdges(Dag dag, DagNode breakNode, List<ExitEdge> breakEdges, DagNode continueNode) {
	    if (breakEdges == null) { 
	        throw new IllegalStateException("break encountered outside of breakable section");
	    }
        ExitEdge e = new ExitEdge();
        e.n1 = breakNode;
        e.gvAttributes.put("color", "red");
        breakEdges.add(e);
        
        return Collections.emptyList();
    }

	// a break will add an edge back to the continueNode only 
    // (and returns an empty list as we won't have a normal exit edge)
    private List<ExitEdge> addContinueEdges(Dag dag, DagNode continueStatementNode, List<ExitEdge> breakEdges, DagNode continueNode) {
        if (continueNode == null) { 
            throw new IllegalStateException("continue encountered outside of continuable section");
        }
        DagEdge e = dag.addBackEdge(continueStatementNode, continueNode, "continue");
        e.gvAttributes.put("color", "red");
        return Collections.emptyList();
    }

    
	// draw branches from this block to then/else blocks
	// exit nodes are combined exit nodes of both branches
	private List<ExitEdge> addIfEdges(Dag dag, DagNode block, List<ExitEdge> breakEdges, DagNode continueNode) {
        // draw the edges
	    List<ExitEdge> prevNodes = new ArrayList<>();
	    if (block.children.size() == 1) {
	        DagNode c = block.children.get(0);
            dag.addEdge(block, c, "Y");
            List<ExitEdge> branchPrevNodes = addEdges(dag, c, breakEdges, continueNode);
            ExitEdge e = new ExitEdge();
            e.n1 = block;
            e.label = "N";
            
            prevNodes.addAll(branchPrevNodes); // Y branch
            prevNodes.add(e); // N branch
	       
	    } else if (block.children.size() == 2) {
	        DagNode c1 = block.children.get(0);
	        DagNode c2 = block.children.get(1);
            dag.addEdge(block, c1, "Y");
            dag.addEdge(block, c2, "N");
            List<ExitEdge> branch1PrevNodes = addEdges(dag, c1, breakEdges, continueNode);
            List<ExitEdge> branch2PrevNodes = addEdges(dag, c2, breakEdges, continueNode);
            prevNodes.addAll(branch1PrevNodes);
            prevNodes.addAll(branch2PrevNodes);
            
	    } else {
	        throw new IllegalStateException("expected 2 children, found " + block.children.size());
	    }
           
        return prevNodes;
    }
	
	// draw branches into and out of try body
	// TODO: catch block
    private List<ExitEdge> addTryEdges(Dag dag, DagNode tryNode, List<ExitEdge> breakEdges, DagNode continueNode) {
        // draw the edges
        TryStatement ts;
        DagNode body = null;
        DagNode catchNode = null;
        for (DagNode c : tryNode.children) {
            // logger.info("try child " + c.locationInParent);
            // both try and catch have a locationInParent of 'body'
            if (c.astNode.getParent() instanceof CatchClause) {
                catchNode = c;
            } else {
                body = c;
            }
        }
        if (body == null) {
            throw new IllegalStateException("try with no body");
        }
        
        dag.addEdge(tryNode,  body);
        List<ExitEdge> bodyPrevNodes = addEdges(dag, body, breakEdges, continueNode);
        return bodyPrevNodes;
        // return prevNodes;
    }
    
    // draw branches into and out of for body
    private List<ExitEdge> addForEdges(Dag dag, DagNode forNode, List<ExitEdge> breakEdges, DagNode _continueNode) {
        // draw the edges
        ForStatement fs;
        // so could draw this with branches leading back up to the for node from each exit node of repeating block
        // or branches down from each exit node to an artifical block at the bottom, with a branch up from that.
        if (forNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + forNode.children.size());
        }
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = forNode.children.get(0);
        dag.addEdge(forNode, repeatingBlock);
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, breakEdges, forNode);
        for (ExitEdge e : repeatingBlockPrevNodes) {
            dag.addBackEdge(e.n1, forNode, "for"); 
        }
        return repeatingBlockPrevNodes;
    }

    // draw branches into and out of extended for body
    private List<ExitEdge> addEnhancedForEdges(Dag dag, DagNode forNode, List<ExitEdge> breakEdges, DagNode _continueNode) {
        // draw the edges
        EnhancedForStatement fs;
        // so could draw this with branches leading back up to the for node from each exit node of repeating block
        // or branches down from each exit node to an artifical block at the bottom, with a branch up from that.
        if (forNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + forNode.children.size());
        }
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = forNode.children.get(0);
        dag.addEdge(forNode, repeatingBlock);
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, breakEdges, forNode);
        for (ExitEdge e : repeatingBlockPrevNodes) {
            dag.addBackEdge(e.n1, forNode, "for"); 
        }
        return repeatingBlockPrevNodes;
    }
    
    private List<ExitEdge> addWhileEdges(Dag dag, DagNode whileNode, List<ExitEdge> _breakEdges, DagNode _continueNode) {
        // draw the edges
        WhileStatement ws;
        
        if (whileNode.children.size() != 1) {
            throw new IllegalStateException("expected 1 child; found " + whileNode.children.size());
        }
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : forNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        DagNode repeatingBlock = whileNode.children.get(0);
        dag.addEdge(whileNode, repeatingBlock);
        
        List<ExitEdge> whileBreakEdges = new ArrayList<>();
        List<ExitEdge> repeatingBlockPrevNodes = addEdges(dag, repeatingBlock, whileBreakEdges, whileNode); // new continue node
        for (ExitEdge e : repeatingBlockPrevNodes) {
            dag.addBackEdge(e.n1, whileNode, "while"); 
        }
        
        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire switch
        prevNodes.addAll(repeatingBlockPrevNodes);
        prevNodes.addAll(whileBreakEdges);
        return prevNodes;
           
    }

           
    
    // ye olde switch, not whatever they're doing in java 16 these days
    // maybe I'm thinking of javascript.
    
    private List<ExitEdge> addSwitchEdges(Dag dag, DagNode switchNode, List<ExitEdge> _breakEdges, DagNode continueNode) {
     // draw the edges
        SwitchStatement ss;
        // org.eclipse.jdt.core.dom.Block in org.eclipse.jdt.core.dom.ForStatement
        // for (DagNode c : switchNode.children) { logger.info(c.astNode.getClass().getName() + " in " + c.astNode.getParent().getClass().getName()); }
        /*
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.IfStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.BreakStatement in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.SwitchCase in org.eclipse.jdt.core.dom.SwitchStatement
[JavaToGraphviz] 08:15:22,452 INFO  com.randomnoun.build.javaToGraphviz.JavaToGraphviz4 - org.eclipse.jdt.core.dom.ExpressionStatement in org.eclipse.jdt.core.dom.SwitchStatement
         */
        // ^ the children of a switchNode interleaves SwitchCases, statements, and break statements
        
        List<ExitEdge> prevNodes = new ArrayList<>(); // the entire switch
        
        List<ExitEdge> casePrevNodes = new ArrayList<>(); // a single case in the switch
        List<ExitEdge> caseBreakEdges = new ArrayList<>();
        boolean hasDefaultCase = false;
        for (DagNode c : switchNode.children) {
            if (c.type.equals("SwitchCase")) {
                // close off last case
                prevNodes.addAll(caseBreakEdges);
                
                // start a new one
                dag.addEdge(switchNode, c, "case"); // case expression
                for (ExitEdge e : casePrevNodes) { // fall-through edges. maybe these should be red instead of the break edges
                    e.n2 = c;
                    dag.addEdge(e);
                }
                casePrevNodes = new ArrayList<>();
                caseBreakEdges = new ArrayList<>();
                
                hasDefaultCase = ((SwitchCase) c.astNode).getExpression() == null;               
                
                ExitEdge e = new ExitEdge();
                e.n1 = c;
                casePrevNodes.add(e);
                
            } else {
                // any other statement is linked to the previous one, similar to a BlockStatement
                if (casePrevNodes.size() == 0) {
                    logger.warn("no edges leading to statement in case. Maybe statement after a breakStatement ? ");
                }
                for (ExitEdge e : casePrevNodes) {
                    e.n2 = c;
                    dag.addEdge(e);
                }
                casePrevNodes = addEdges(dag, c, caseBreakEdges, continueNode);
            }
        }
        
        // edges out of the final case are also edges out of the switch
        prevNodes.addAll(caseBreakEdges);
        prevNodes.addAll(casePrevNodes);
        
        // if there was no 'default' case, then also include an edge from the switchNode
        if (!hasDefaultCase) {
            ExitEdge e = new ExitEdge();
            e.n1 = switchNode;
            prevNodes.add(e);
        }

        return prevNodes;
    }    
    
	
	
    /** Traces through the DAG, storing in each node the most recently seen node that had 'keepNode' set to true
     * (the 'lastKeepNode').
     *  
     * @param dag dag we're traversing
     * @param node the node in the dag we're up to
     * @param lastKeepNode most recently seen node with keepNode set to true
     */
    private void setLastKeepNode(Dag dag, DagNode node, DagNode lastKeepNode) {
        List<DagEdge> outEdges = node.outEdges;
        if (node.lastKeepNode != null) {
            // rejoined with other edges
            // logger.warn("lastKeepNode already set on node " + node.name + " as " + node.lastKeepNode.name + ", node.keepNode=" + node.keepNode + ", lastKeepNode=" + lastKeepNode.name);
            return;
        } else if (node.keepNode) {
            lastKeepNode = node;
        }
        node.lastKeepNode = lastKeepNode;
        for (DagEdge e : outEdges) {
            setLastKeepNode(dag, e.n2, lastKeepNode);
        }
    }

    // from the fromNode ?
    /** Prune all edges and nodes from an edge (e) to a keepNode
     * 
     * @param dag
     * @param e
     * @param fromNode
     * @param keepNode
     */
    private void pruneEdges(Dag dag, DagEdge e, DagNode fromNode, DagNode keepNode) {
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
                dag.nodes.remove(prevNode);
                for (DagEdge prevEdge : new ArrayList<DagEdge>(prevNode.inEdges)) { // may be modified whilst iterating ?
                    if (dag.edges.contains(prevEdge)) { // prevNode.inEdges
                        
                        pruneEdges(dag, prevEdge, fromNode, keepNode);
                    }
                }
            }
        }
    }

    private void removeNodes(Dag dag, DagNode node) {
        removeNodes(dag, node, false);
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
	private void removeNodes(Dag dag, DagNode node, boolean mergeEdges) {
	    
        List<DagEdge> inEdges = node.inEdges;
        List<DagEdge> outEdges = node.outEdges;
        Set<DagNode> redoNodes = new HashSet<>();
        
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
                        // logger.info("on node " + node.name + ", merged edges back to " + inEdge1KeepNode.name + ", i=" + i + ", j=" + j);
                        DagEdge newEdge = new DagEdge();
                        newEdge.n1 = inEdge1KeepNode;
                        newEdge.n2 = node;
                        newEdge.label = "something";
                        // format the edge a bit ?
                        
                        pruneEdges(dag, inEdge1, node, inEdge1KeepNode);  
                        pruneEdges(dag, inEdge2, node, inEdge2KeepNode); 
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
            // if either edge was colored (break edges), the merged edge is as well
            String color = inEdge.gvAttributes.get("color") == null ? outEdge.gvAttributes.get("color") : inEdge.gvAttributes.get("color");
            if (color != null) { newEdge.gvAttributes.put("color", color); }
            
            dag.removeEdge(inEdge);
            dag.removeEdge(outEdge);
            if (!dag.hasEdge(newEdge.n1, newEdge.n2)) {
                dag.addEdge(newEdge);
            }
            
            // logger.info("removed node " + node.name);
            dag.nodes.remove(node);
            removeNodes(dag, newEdge.n2, mergeEdges);
        
        } else if (true) { // mergeEdges
            
            // trace down the outEdges
            
            // logger.warn(node.outEdges.size() + " outEdges from node " + node.name);
            for (DagEdge e : new ArrayList<DagEdge>(node.outEdges)) {
                if (dag.edges.contains(e)) {
                    if (dag.nodes.contains(e.n2)) { 
                        if (!e.back) {
                            // don't follow back edges
                            removeNodes(dag, e.n2);
                        }
                    } else {
                        logger.warn("subnode " + e.n2.name + " missing from " + node.name);
                        dag.removeEdge(e);
                    }
                } else {
                    // logger.warn("edge " + e + " missing from " + node.name);
                }
            }
        }
        
        // if we've merged edges, may need to shorten paths again from the keepNodes
        for (DagNode redo : redoNodes) {
            removeNodes(dag, redo, false); // false = shorten only
        }
	    
	}


    @SuppressWarnings("unchecked")
    private List<CommentText> getComments(CompilationUnit cu, String src) {

        // @TODO better regex
        // Pattern gvPattern = Pattern.compile("^gv(\\.[a-zA-Z]+)*:");  // gv.some.class.names:

        // probably do this right at the end as gv.literal affects how we parse it
        // Pattern valPattern = Pattern.compile("(([a-zA-Z]+)\\s*([^;]*);\\s*)*"); // things;separated;by;semicolons;
        
        List<CommentText> comments = new ArrayList<>();
        for (Comment c : (List<Comment>) cu.getCommentList()) {
            // comment.accept(cv);
            int start = c.getStartPosition();
            int end = start + c.getLength();
            String text = src.substring(start, end);
            if (c.isBlockComment()) {
                if (text.startsWith("/*") && text.endsWith("*/")) {
                    text = text.substring(2, text.length() - 2).trim();

                    if (text.startsWith("gv:style:")) {
                        String s = text.substring(9).trim();
                        if (s.startsWith("{") && s.endsWith("}")) {
                            s = s.substring(1, s.length() - 1).trim();
                            // here be the css
                            // remove inline comments
                            
                        } else {
                            throw new IllegalStateException("gv:style does not start with '{' and end with '}':  '" + text + "'");
                        }
                    }
                    
                    // hrm.
                    // thinking using classes to define styles is a bit odd
                    // so maybe 
                    //   gv:style: { styles } 
                    // instead of
                    //   gv.style: { styles }
                    // which should simplify this a bit

                        
                } else {
                    throw new IllegalStateException("Block comment does not start with '/*' and end with '*/':  '" + text + "'");
                }
            }

            if (c.isLineComment()) {
                if (text.startsWith("//")) {
                    text = text.substring(2).trim();
                    if (text.startsWith("gv:")) {
                        text = text.substring(3).trim();
                        // c.getAlternateRoot() is the CompilationUnit for all of these comments
                        // which isn't all that useful really
                        
                        

                        Pattern gvPattern = Pattern.compile("gv(\\.[a-zA-Z0-9-_]+)?(\\.[a-zA-Z0-9-_]+)*:");  // gv.some.class.names: -> .some
                        Pattern gvPattern2 = Pattern.compile("(\\.[a-zA-Z0-9-_]+)");  // the rest of them
                        Matcher fgm = gvPattern.matcher(text);
                        List<String> classes = new ArrayList<>();
                        if (fgm.find()) {
                            classes.add(fgm.group(1));
                            int pos = fgm.end(1);
                            Matcher gm = gvPattern2.matcher(fgm.group(0));
                            while (gm.find(pos)) {
                                classes.add(gm.group(1));
                                pos = gm.end(1);
                            }
                            text = text.substring(fgm.end());
                            
                            System.out.println("classes " + classes + " in " + text);
                        }
                        
                        comments.add(new CommentText(c, cu.getLineNumber(start), text));
                    }
                    
                } else {
                    throw new IllegalStateException("Line comment does not start with '//': '" + text + "'");
                }
            }
            
            
        }
        return comments;
    }



    /** A source-code comment */
    static class CommentText {
	    Comment comment;
	    int line;
	    String text;
	    
	    public CommentText(Comment c, int line, String text) {
	        this.comment = c;
	        this.line = line;
	        this.text = text;
	    }
	}
	
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
     * <p>NB: we can make a DAG look like a cyclical graph by reversing the arrows on the diagram, which
     * we might want to do for loops later on. Or I guess we could make it cyclical.
     * 
     * <p>Well, that was the idea, but that's too fiddly to manage, so the DAG is now cyclical.
     * 
     * @TODO rename everything
     */
	static class Dag {
	    List<DagNode> nodes = new ArrayList<>();
	    List<DagEdge> edges = new ArrayList<>();
	    Set<String> names = new HashSet<String>();
	    Logger logger = Logger.getLogger(Dag.class);
	    
	    Map<ASTNode, DagNode> astToDagNode = new HashMap<>();
	    public void addNode(DagNode n) {
	        nodes.add(n);
	        astToDagNode.put(n.astNode, n);
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
            e.gvAttributes.put("style", "dashed");
            return e;
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
	static class DagNode {
	    DagNode parentDagNode;
	    List<DagNode> children = new ArrayList<>();
	    
	    String dagLoc;
	    String locationInParent; 
	    
	    boolean keepNode = false;
	    DagNode lastKeepNode = null;
	    List<DagEdge> inEdges = new ArrayList<>();
	    List<DagEdge> outEdges = new ArrayList<>();

	    // graphviz formatting attributes
	    Map<String, String> gvAttributes = new HashMap<>();
	    Set<String> classes;
	    
	    String type;
	    int line;
	    String name;
	    String label;
	    
	    ASTNode astNode;
	    
	    public void addChild(DagNode node) {
	        children.add(node);
	    }
	    
	    public String toGraphviz() {
	        String labelText = line + ": " + Text.replaceString(label, "\"",  "\\\"") +
	            (lastKeepNode == null ? "" : ", lkn=" + lastKeepNode.name);
	        String a = "";
	        for (Entry<String, String> e : gvAttributes.entrySet()) {
	            a += e.getKey() + " = " + e.getValue() + "; ";
	        }
	        
	        return
	          (dagLoc == null ? "" : dagLoc) + 
	          name + " [\n" + 
              "  label = \"" + labelText + "\";\n" + 
	          a + 
              "];";	        
	    }
	}
	
	static class DagEdge {
	    DagNode n1; 
	    DagNode n2;
	    String n1Port;
	    String n2Port;
	    String label;
	    Map<String, String> gvAttributes = new HashMap<>();
	    boolean back = false;

	    public String toGraphviz() {
	        String labelText = label == null ? null : Text.replaceString(label, "\"",  "\\\"");
            String a = "";
            for (Entry<String, String> e : gvAttributes.entrySet()) {
                a += e.getKey() + " = " + e.getValue() + "; ";
            }

	        return n1.name + (n1Port == null ? "" : ":" + n1Port) + 
	            " -> " + 
	            n2.name + (n2Port == null ? "" : ":" + n2Port) + 
	            (label == null && gvAttributes.size() == 0? "" : " [" +
	            (label == null ? "" : "label=\"" + labelText + "\"; ") +
	            a +
	           "]") + ";";
        }
	}
	
	// an edge whose second node isn't known yet
	static class ExitEdge extends DagEdge {
	    
	}
	
	/** An ASTVisitor that constructs the Dag
	 * 
	 * <p>Each AstNode is converted into a DagNode, and extra nodes are created for comments, which 
	 * don't appears in the eclipse AST in the right place for some reason. 
	 * 
	 * <p>Comments that appear on the same line as a statement are associated with that statement.
	 *
	 * <p>If there are multiple statements on a line, it will be associated with the first one on the line.
	 */
	static class DagVisitor extends ASTVisitor {
	    
	    Logger logger = Logger.getLogger(DagVisitor.class);
	    
        int lastIdx = 0;
        String className; // SimpleName ?
        String methodName;
        String lastClass = null;
        String lastMethod = null;
        Dag dag;
        List<CommentText> comments;
        CompilationUnit cu;
        String src;
        
        public DagVisitor(CompilationUnit cu, String src, List<CommentText> comments) {
            super(true);
            this.cu = cu;
            this.comments = comments;
            this.src = src;
            dag = new Dag();
        }
        
        public Dag getDag() { 
            return dag;
        }
        
        /** Comments don't appear in the eclipse AST in the right place, so we call this periodically
         * to add in any comments that should have appeared in the Dag by now. 
         * <p>These comment nodes will be added to the previousDagNode's list of children. 
         * 
         * @param pdn previous DagNode
         * @param line
         */
        void createCommentNodesToLine(DagNode pdn, int line) {
            // DagNode lastNode = null;
            while (lastIdx < comments.size() && comments.get(lastIdx).line < line) {
                CommentText ct = comments.get(lastIdx);
                
                String dagLoc = "";
                
                DagNode dn = new DagNode();
                dn.keepNode = true; // always keep comments
                dn.dagLoc = dagLoc;
                dn.type = "comment";
                dn.line = ct.line;
                dn.name = dag.getUniqueName("c_" + ct.line);
                dn.label = ct.text;
                dn.astNode = null;
                
                dag.addNode(dn);
                if (pdn!=null) {
                    pdn.addChild(dn);
                } else {
                    logger.warn("null pdn on " + dn.type + " on line " + dn.line);
                }
                lastIdx ++; 
            }
        }
        
        public boolean preVisit2(ASTNode node) {
            DagNode pdn = getClosestDagNode(node);
            
            int line = cu.getLineNumber(node.getStartPosition());
            // writeCommentsToLine(line);

            if (node instanceof Statement) {
                
                createCommentNodesToLine(pdn, line);
                
                DagNode dn = new DagNode();
                String clazz = Text.getLastComponent(node.getClass().getName());
                if (clazz.endsWith("Statement")) {
                    clazz = clazz.substring(0, clazz.length() - 9);
                }
                String lp = "s"; // linePrefix
                if (clazz.equals("If")) { lp = "if"; }
                
                dn.type = clazz; // "if";
                dn.line = line;
                dn.name = dag.getUniqueName(lp + "_" + line); // "if_" + line;
                dn.label = clazz; // "if";
                dn.parentDagNode = pdn;
                dn.astNode = node;
                dn.locationInParent = node.getLocationInParent().getId();
                dag.addNode(dn);
                if (pdn!=null) {
                    pdn.addChild(dn);
                } else {
                    // logger.warn("null pdn on " + node);
                    logger.warn("preVisit: null pdn on " + dn.type + " on line " + dn.line);
                }
                
                if (lastIdx < comments.size() && comments.get(lastIdx).line == line) {
                    CommentText ct = comments.get(lastIdx);
                    dn.keepNode = true; // always keep comments
                    dn.label = ct.text;
                    lastIdx++;
                }
            }
            
            return true;
        }
        
        public void postVisit(ASTNode node) {
            
        }
        
        
        /** Navigate back up the AST tree until we find an ASTNode that's already in the Dag,
         * and then return that DagNode
         * 
         * @param node
         * @return
         */
        public DagNode getClosestDagNode(ASTNode node) {
            while (node != null) {
                if (dag.astToDagNode.get(node) != null) { 
                    return dag.astToDagNode.get(node);
                }
                ASTNode parent = node.getParent();
                node = parent;
            }
            return null;
        }
        
    }	
	
	
	public static void main(String args[]) throws Exception {
	    Log4jCliConfiguration lcc = new Log4jCliConfiguration();
	    lcc.init("[JavaToGraphviz]", null);
	    Logger logger = Logger.getLogger(JavaToGraphviz4.class);

        // InputStream is = JavaToGraphviz4.class.getResourceAsStream("/test.java");
	    InputStream is = JavaToGraphviz4.class.getResourceAsStream("/Statements.java");

		JavaToGraphviz4 javaToGraphviz = new JavaToGraphviz4();
		javaToGraphviz.test(is);
		is.close();
	}
	
	// yeah ok
	/*
	private void C(boolean condition, int thing) {
	    System.out.println("something");
        
        switch (thing) {
            case 1: System.out.println("something"); //gv: case 1
                if (condition) {
                    System.out.println("conditional");
                    break;
                }
                // conditional fallthrough
                
            case 2: // comment on case 2
                System.out.println("something"); // comment on case 2 body, fallthrough
                
            case 3:
                System.out.println("something");
                break;
                
            default:
                System.out.println("something"); // the default body
                
        }
        System.out.println("somethingElse");
	}
	*/
}