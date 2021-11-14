package com.randomnoun.build.javaToGraphviz;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jsoup.nodes.Document;
import org.w3c.dom.css.CSSStyleSheet;

import com.randomnoun.build.javaToGraphviz.astToDag.AstEdger;
import com.randomnoun.build.javaToGraphviz.astToDag.AstToDagVisitor;
import com.randomnoun.build.javaToGraphviz.astToDag.CommentExtractor;
import com.randomnoun.build.javaToGraphviz.astToDag.ControlFlowEdger;
import com.randomnoun.build.javaToGraphviz.astToDag.DagNodeFilter;
import com.randomnoun.build.javaToGraphviz.astToDag.DagStyleApplier;
import com.randomnoun.build.javaToGraphviz.astToDag.ExpressionEdger;
import com.randomnoun.build.javaToGraphviz.astToDag.LexicalScope;
import com.randomnoun.build.javaToGraphviz.comment.CommentText;
import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
import com.randomnoun.build.javaToGraphviz.dag.ExitEdge;
import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

// so the next bit is going to be styling
// and supporting more types of statements
// and user-defined subgraphs
// and then integrating with jacoco
// and AOP 

/** Convert an AST tree of a java class ( CompilationUnit, created by the eclipse ASTParser )
 * into a graphviz diagram ( Dag )
 * 
 * A complete standalone example of ASTParser
 * @see https://www.programcreek.com/2011/01/a-complete-standalone-example-of-astparser/
 *
 */
public class JavaToGraphviz {

    Logger logger = Logger.getLogger(JavaToGraphviz.class);

    CompilationUnit cu;
    List<CommentText> comments;
    CSSStyleSheet styleSheet;
    Dag dag;
    int rootGraphIdx;
    
    // options
    boolean removeNode = false;
    int astParserLevel = JLS11;
    boolean includeThrowEdges = true;
    boolean includeThrowNodes = true;
    String format = "dot";
    String baseCssUrl = "JavaToGraphviz-base.css";
    List<String> userCssUrls = null;
    List<String> userCssRules = null;
    List<String> edgerNames = Collections.singletonList("control-flow");
    
    // non-deprecated AST constants. 
    // there's an eclipse ticket to create an alternative non-deprecated interface for these which isn't complete yet.
    
    /** Java Language Specification, Second Edition (JLS2); <= J2SE 1.4 */
    @SuppressWarnings("deprecation")
    public static final int JLS2 = AST.JLS2; // 2

    /** Java LanguageSpecification, Third Edition (JLS3); <= J2SE 5 (aka JDK 1.5) */
    @SuppressWarnings("deprecation")
    public static final int JLS3 = AST.JLS3; // 3
    
    /** Java Language Specification, Java SE 7 Edition (JLS7) as specified by JSR336; Java SE 7 (aka JDK 1.7). */ 
    @SuppressWarnings("deprecation")
    public static final int JLS4 = AST.JLS4; // 4
    
    /** Java Language Specification, Java SE 8 Edition (JLS8) as specified by JSR337; Java SE 8 (aka JDK 1.8). */
    @SuppressWarnings("deprecation")
    public static final int JLS8 = AST.JLS8; // 8;

    /** Java Language Specification, Java SE 9 Edition (JLS9); Java SE 9 (aka JDK 9) */
    @SuppressWarnings("deprecation")
    public static final int JLS9 = AST.JLS9; // 9;

    /** Java Language Specification, Java SE 10 Edition (JLS10). Java SE 10 (aka JDK 10). */
    @SuppressWarnings("deprecation")
    public static final int JLS10 = AST.JLS10; // 10;

    /** Java Language Specification, Java SE 11 Edition (JLS11). Java SE 11 (aka JDK 11). */
    @SuppressWarnings("deprecation")
    public static final int JLS11 = AST.JLS11; // 11
    
    /** Java Language Specification, Java SE 12 Edition (JLS12). Java SE 12 (aka JDK 12). */
    @SuppressWarnings("deprecation")
    public static final int JLS12 = AST.JLS12; // 12;
    
    /** Java Language Specification, Java SE 13 Edition (JLS13). Java SE 13 (aka JDK 13). */
    @SuppressWarnings("deprecation")
    public static final int JLS13 = AST.JLS13; // 13;

    /** Java Language Specification, Java SE 14 Edition (JLS14). Java SE 14 (aka JDK 14). */
    @SuppressWarnings("deprecation")
    public static final int JLS14 = AST.JLS14; // 14;

    /** Java Language Specification, Java SE 15 Edition (JLS15). Java SE 15 (aka JDK 15). */
    @SuppressWarnings("deprecation")
    public static final int JLS15 = AST.JLS15; // 15;

    /** Java Language Specification, Java SE 16 Edition (JLS16). Java SE 16 (aka JDK 16). */
    public static final int JLS16 = AST.JLS16; // 16;

    
// see https://stackoverflow.com/questions/47146706/how-do-i-associate-svg-elements-generated-by-graphviz-to-elements-in-the-dot-sou
    
    // depending on how many of these I end up with, maybe bundle these into an options object
    // should be gv styles as well, probably
    public void setRemoveNode(boolean removeNode) {
        this.removeNode = removeNode;
    }
    
    public void setAstParserLevel(int astParserLevel) {
        this.astParserLevel = astParserLevel;
    }
    
    public void setIncludeThrowNodes(boolean includeThrowNodes) {
        this.includeThrowNodes = includeThrowNodes;
    }
    
    public void setIncludeThrowEdges(boolean includeThrowEdges) {
        this.includeThrowEdges = includeThrowEdges;
    }
    
    public void setBaseCssUrl(String baseCssUrl) {
        this.baseCssUrl = baseCssUrl;
    }

    public void setEdgerNames(List<String> edgerNames) {
        this.edgerNames = edgerNames;
    }

    public void setUserCssUrls(List<String> userCssUrls) {
        this.userCssUrls = userCssUrls;
    }

    public void setUserCssRules(List<String> userCssRules) {
        this.userCssRules = userCssRules;
    }
    
    
    
    /** Parse the java source code in the supplied inputstream.
     * 
     * <p>This method creates the CompilationUnit AST, collects comments, creates the stylesheet, and constructs the Dag
     * containin the graphviz nodes for the diagram. The edges are constructed later.  
     * 
     * @param is
     * @param charset
     * @throws IOException
     */
	public void parse(InputStream is, String charset) throws IOException  {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamUtil.copyStream(is, baos);
		
		String src = baos.toString(charset);
		ASTParser parser = ASTParser.newParser(astParserLevel); 
		parser.setSource(src.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		cu = (CompilationUnit) parser.createAST(null);
		
		CommentExtractor ce = new CommentExtractor();
		comments = ce.getComments(cu, src);
		styleSheet = ce.getStyleSheet(comments, baseCssUrl, userCssUrls, userCssRules);
		
		AstToDagVisitor dv = new AstToDagVisitor(cu, src, comments, includeThrowNodes);
        cu.accept(dv);
        dag = dv.getDag();
        
        rootGraphIdx = 0;
	}

	/** Generate a single graphviz diagram.
	 * 
	 * @param writer
	 * @return true if there are more diagrams, false otherwise
	 * @throws IOException
	 */
	public boolean writeGraphviz(Writer writer) throws IOException {
	    
	    // DagNode methodNode = dag.rootNodes.get(rootNodeIdx);
	    DagSubgraph rootGraph = dag.rootGraphs.get(rootGraphIdx);
	    
        PrintWriter pw = new PrintWriter(writer);

        dag.edges.clear();

        
        /*
        // clear edges and graphs from previous runs
        dag.edges.clear();
        dag.subgraphs.clear();
        dag.dagNodeToSubgraph.clear();
        
        // clear styles from previous runs
        // doesn't clear calculated labels though. maybe it should. maybe. it. should.
        dag.gvStyles.clear();
        dag.gvNodeStyles.clear();
        dag.gvEdgeStyles.clear();
        for (DagNode n : dag.nodes) {
            n.gvStyles.clear();
        }
        */
        
        for (DagNode methodNode : dag.rootNodes) {
            if (rootGraph.nodes.contains(methodNode)) {
                
                LexicalScope lexicalScope = new LexicalScope();

                for (String edgerName : edgerNames) {
                    if (edgerName.equals("control-flow")) {
                        ControlFlowEdger edger = new ControlFlowEdger(dag);
                        edger.setIncludeThrowEdges(includeThrowEdges);
                        edger.addEdges(dag, methodNode, lexicalScope);
                    } else if (edgerName.equals("ast")) {
                        AstEdger edger = new AstEdger(dag);
                        edger.addEdges(dag,  methodNode, lexicalScope);
                    }
                }

                if (removeNode) {
                    DagNodeFilter filter = new DagNodeFilter(dag);
                    methodNode.keepNode = false;
                    filter.setLastKeepNode(methodNode, methodNode);
                    filter.removeNodes(methodNode); // peephole node removal.
                }
            }
        }

        // subgraphs are now defined in the Dag from the stylesheet
        DagStyleApplier dsa = new DagStyleApplier(dag, rootGraph);
        Document doc = dsa.createDom();
        if (format.equals("dom1")) {
            pw.println(doc.toString());
        } else {
            dsa.inlineStyles(styleSheet);
        }
        
        
        

        // actually think I'm going to evaluate some of this css as the dag is being constructed.
        /*
        // find any DagNodes that have gv-fluent: true on them
        // and add some more edges into the graph
        ArrayList<DagNode> fluentNodes = new ArrayList<>();
        for (DagNode methodNode : dag.rootNodes) {
            if (rootGraph.nodes.contains(methodNode)) {
                findNodesWithGvStyle(fluentNodes, methodNode, "gv-fluent", "true");
            }
        }
        for (DagNode expressionNode : fluentNodes) {
            LexicalScope lexicalScope = new LexicalScope();
            ExpressionEdger edger = new ExpressionEdger(dag);
            edger.setIncludeThrowEdges(includeThrowEdges);
            edger.addEdges(dag, expressionNode, lexicalScope);
        }
        // probably need to restyle things again
         */
        
        if (format.equals("dom2")) {
            doc = dsa.getDocument();
            pw.println(doc.toString());
        } else {
            pw.println(rootGraph.toGraphviz(0));
        }
        
        
        /*
        pw.println(dag.toGraphvizHeader());
        for (DagNode node : dag.nodes) {
            // only draw nodes if they have an edge
            boolean hasEdge = false;
            for (DagEdge e : dag.edges) {
                if (e.n1 == node || e.n2 == node) { hasEdge = true; break; }
            }
            if (node != methodNode && hasEdge) {
                pw.println(node.toGraphviz(isDebugLabel));
            }
        }
        for (DagEdge edge : dag.edges) {
            if (edge.n1 != methodNode) {
                pw.println(edge.toGraphviz());
            }
        }

        // subgraphs
        for (DagSubgraph sg : dag.subgraphs) {
            pw.println(sg.toGraphviz(2));
        }
        pw.println(dag.toGraphvizFooter());
        pw.flush();
        */
        
        rootGraphIdx++; 
        return (rootGraphIdx < dag.rootGraphs.size());
	}

	private void findNodesWithGvStyle(ArrayList<DagNode> result, DagNode node, String gvStyleName, String value) {
        if (value.equals(node.gvStyles.get(gvStyleName))) {
            result.add(node);
        }
        for (DagNode n : node.children) {
            findNodesWithGvStyle(result, n, gvStyleName, value);
        }
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSourceVersion(String sourceVersion) {
        switch (sourceVersion) {
            // up until 1.5 sun maintained a fairly normal version numbering scheme
            case "1":
            case "1.0":
            case "1.1":
            case "1.2": 
            case "1.3":
            case "1.4":
                astParserLevel = JLS2; break;
            case "1.5":
                astParserLevel = JLS3; break;
                
            // then we started dropping the '1.' on occasion 
            case "6":
            case "1.6":
            case "7":
            case "1.7":
                astParserLevel = JLS4; break;
            case "1.8":
            case "8":
                astParserLevel = JLS8; break;
                
            // from 9 onwards we don't have the '1.' prefix any more    
            case "9":
                astParserLevel = JLS9; break;
            case "10":
                astParserLevel = JLS10; break;
            case "11":
                astParserLevel = JLS11; break;
            case "12":
                astParserLevel = JLS12; break;
            case "13":
                astParserLevel = JLS13; break;
            case "14":
                astParserLevel = JLS14; break;
            case "15":
                astParserLevel = JLS15; break;
            case "16":
                astParserLevel = JLS16; break;
            default:
                throw new IllegalArgumentException("Unknown source version '" + sourceVersion + "'");
        }
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