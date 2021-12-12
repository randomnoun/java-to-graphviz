package com.randomnoun.build.javaToGraphviz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.randomnoun.build.javaToGraphviz.astToDag.LexicalScope;
import com.randomnoun.build.javaToGraphviz.comment.CommentText;
import com.randomnoun.build.javaToGraphviz.dag.Dag;
import com.randomnoun.build.javaToGraphviz.dag.DagEdge;
import com.randomnoun.build.javaToGraphviz.dag.DagNode;
import com.randomnoun.build.javaToGraphviz.dag.DagSubgraph;
import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.Text;

// so the next bit is going to be 
// x styling
// x and supporting more types of statements
// x and user-defined subgraphs
// . and then integrating with jacoco
// . and AOP 

/** Convert an AST tree of a java class ( CompilationUnit, created by the eclipse ASTParser )
 * into a graphviz diagram ( Dag )
 * 
 * @see <a href="https://www.programcreek.com/2011/01/a-complete-standalone-example-of-astparser/">standalone ASTParser example</a>
 */
public class JavaToGraphviz {

    Logger logger = Logger.getLogger(JavaToGraphviz.class);

    CompilationUnit cu;
    List<CommentText> comments;
    CSSStyleSheet styleSheet;
    Dag dag;
    int rootGraphIdx;
    
    // options
    Map<String, String> options; // @TODO enum -> string 
    int astParserLevel = JLS11;
    String format = "dot";
    String baseCssUrl = "JavaToGraphviz-base.css";
    List<String> userCssUrls = null;
    List<String> userCssRules = null;
    
    // non-deprecated AST constants. 
    // there's an eclipse ticket to create an alternative non-deprecated interface for these which isn't complete yet.
    
    /** Java Language Specification, Second Edition (JLS2); &lt;= J2SE 1.4 */
    @SuppressWarnings("deprecation")
    public static final int JLS2 = AST.JLS2; // 2

    /** Java LanguageSpecification, Third Edition (JLS3); &lt;= J2SE 5 (aka JDK 1.5) */
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
    
    public JavaToGraphviz() {
        options = new HashMap<>();
        options.put("edgerNamesCsv", "control-flow");
        options.put("enableKeepNodeFilter", "false");
        options.put("defaultKeepNode", "true");
    }
   
    public void setOptions(Map<String, String> options) {
        this.options.putAll(options);
    }
    
    public void setAstParserLevel(int astParserLevel) {
        this.astParserLevel = astParserLevel;
    }
    
    public void setBaseCssUrl(String baseCssUrl) {
        this.baseCssUrl = baseCssUrl;
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
		
		AstToDagVisitor dv = new AstToDagVisitor(cu, src, comments, options);
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
	    
	    DagSubgraph rootGraph = dag.rootGraphs.get(rootGraphIdx);
        PrintWriter pw = new PrintWriter(writer);
   
        // remove edges from previous runs
        dag.edges.clear(); 
        
        // a rootGraph can now contain multiple rootNodes
        // but a rootNode can only be in a single rootGraph
        int c = 0;
        for (DagNode rootNode : dag.rootNodes) {
            c++;
            if (rootGraph.nodes.contains(rootNode)) {
                logger.info("including rootNode " + c);
                
                String edgerNamesCsv = rootNode.options.get("edgerNamesCsv");
                if (Text.isBlank(edgerNamesCsv)) { edgerNamesCsv = "control-flow"; }
                boolean enableKeepNodeFilter = "true".equals(rootNode.options.get("enableKeepNodeFilter"));
                
                List<String> edgerNames;
                try {
                    edgerNames = Text.parseCsv(edgerNamesCsv);
                } catch (ParseException e1) {
                    throw new IllegalArgumentException("edgerNamesCsv is not valid CSV", e1);
                }
                rootNode.options.get("removeNodes");
                
                LexicalScope lexicalScope = new LexicalScope();

                for (String edgerName : edgerNames) {
                    if (edgerName.equals("control-flow")) {
                        ControlFlowEdger edger = new ControlFlowEdger(dag);
                        edger.addEdges(dag, rootNode, lexicalScope);
                    
                    } else if (edgerName.equals("ast")) {
                        AstEdger edger = new AstEdger(dag);
                        edger.addEdges(dag, rootNode, lexicalScope);
                    
                    // @TODO data-flow
                        
                    } else {
                        throw new IllegalArgumentException("unknown edgerName '" + edgerName + "'");
                    }
                }

                // the edgers now move things around so should probably recalc the inEdges and outEdges
                // as the keepNode thingy relies those on being accurate.
                // although we've got subgraphs now which is going to make things more exciting, probably.
                // going to assume that edges don't cross rootNodes, which they don't yet either.
                for (DagNode n : dag.nodes) {
                    n.inEdges = new ArrayList<>();
                    n.outEdges = new ArrayList<>();
                }
                for (DagEdge e : dag.edges) {
                    e.n1.outEdges.add(e);
                    e.n2.inEdges.add(e);
                }
                
                if (enableKeepNodeFilter) {
                    DagNodeFilter filter = new DagNodeFilter(dag);
                    if (rootNode.keepNodeMatcher.matches("startNode")) {
                        rootNode.keepNode = true;
                        filter.setLastKeepNode(rootNode, rootNode);
                    } else {
                        rootNode.keepNode = false;
                        
                    }
                    filter.removeNodes(rootNode);
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
        
        if (format.equals("dom1")) {
            // already generated output
        } else if (format.equals("dom2")) {
            doc = dsa.getDocument();
            pw.println(doc.toString());
        } else {
            pw.println(rootGraph.toGraphviz(0));
        }
        
        rootGraphIdx++; 
        return (rootGraphIdx < dag.rootGraphs.size());
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
    

}