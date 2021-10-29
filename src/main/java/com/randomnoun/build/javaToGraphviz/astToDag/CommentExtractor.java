package com.randomnoun.build.javaToGraphviz.astToDag;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleSheet;

import com.randomnoun.build.javaToGraphviz.comment.CommentText;
import com.randomnoun.build.javaToGraphviz.comment.GvComment;
import com.randomnoun.build.javaToGraphviz.comment.GvGraphComment;
import com.randomnoun.build.javaToGraphviz.comment.GvStyleComment;
import com.randomnoun.build.javaToGraphviz.comment.GvSubgraphComment;
import com.randomnoun.build.javaToGraphviz.dom.RestrictedCssStyleSheetImpl;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;

public class CommentExtractor {

    Logger logger = Logger.getLogger(CommentExtractor.class);
    
	/** Return a list of processed comments from the source file. 
	 * 
	 * GvStyleComment:    "// gv-style: { xxx }"
	 * GvDigraphComment:  "// gv-digraph: xxx"
	 * GvSubgraphComment: "// gv-subgraph: xxx"
	 * GvComment:         "// gv.className.className.className: xxx { xxx }"
	 * 
	 * @param cu
	 * @param src
	 * @return
	 */
    @SuppressWarnings("unchecked") 
    public List<CommentText> getComments(CompilationUnit cu, String src) {

        // @TODO better regex
        // Pattern gvPattern = Pattern.compile("^gv(\\.[a-zA-Z]+)*:");  // gv.some.class.names:

        // probably do this right at the end as gv.literal affects how we parse it
        // Pattern valPattern = Pattern.compile("(([a-zA-Z]+)\\s*([^;]*);\\s*)*"); // things;separated;by;semicolons;
        
        Pattern gvClassPattern = Pattern.compile("^gv(\\.[a-zA-Z0-9-_]+)?(\\.[a-zA-Z0-9-_]+)*:");  // gv.some.class.names: -> .some
        Pattern gvGraphClassPattern = Pattern.compile("^gv-graph(\\.[a-zA-Z0-9-_]+)?(\\.[a-zA-Z0-9-_]+)*:");  // gv.some.class.names: -> .some
        Pattern gvNextClassPattern = Pattern.compile("(\\.[a-zA-Z0-9-_]+)");  // the rest of them
        Pattern curlyPattern = Pattern.compile("\\{(.*)\\}");  // @TODO quoting/escaping rules
        
        List<CommentText> comments = new ArrayList<>();
        for (Comment c : (List<Comment>) cu.getCommentList()) {
            // comment.accept(cv);
            int start = c.getStartPosition();
            int end = start + c.getLength();
            String text = src.substring(start, end);
            if (c.isBlockComment()) {
                if (text.startsWith("/*") && text.endsWith("*/")) {
                    text = text.substring(2, text.length() - 2).trim();
                } else {
                    throw new IllegalStateException("Block comment does not start with '/*' and end with '*/':  '" + text + "'");
                }
            }
            if (c.isLineComment()) {
                if (text.startsWith("//")) {
                    text = text.substring(2).trim();
                } else {
                    throw new IllegalStateException("Line comment does not start with '//': '" + text + "'");
                }
            }
            

            if (text.startsWith("gv-style:")) {
                String s = text.substring(9).trim();
                if (s.startsWith("{") && s.endsWith("}")) {
                    s = s.substring(1, s.length() - 1).trim();
                    // here be the css
                    // remove inline comments
                    // logger.info("maybe here ? " +  s);
                    comments.add(new GvStyleComment(c, cu.getLineNumber(start), text, s));
                    
                } else {
                    throw new IllegalStateException("gv-style does not start with '{' and end with '}':  '" + text + "'");
                }

            } else if (text.startsWith("gv-subgraph:")) {
                String s = text.substring(11).trim();
                comments.add(new GvSubgraphComment(c, cu.getLineNumber(start), s));
            } else {
                List<String> classes = new ArrayList<>();

                Matcher fgm = gvGraphClassPattern.matcher(text);
                if (fgm.find()) {
                    // logger.info("groupCount " + fgm.groupCount());
                    if (fgm.group(1)!=null) {
                        classes.add(fgm.group(1).substring(1));
                    }
                    int pos = fgm.end(1);
                    Matcher gm = gvNextClassPattern.matcher(fgm.group(0));
                    logger.info(pos);
                    while (pos!=-1 && gm.find(pos)) {
                        classes.add(gm.group(1).substring(1));
                        pos = gm.end(1);
                        logger.info(pos);
                    }
                    text = text.substring(fgm.end());
                    // System.out.println("classes " + classes + " in " + text); 

                    // if there's anything in curly brackets remaining, then that's a style rule.
                    // @TODO handle curlies outside of style rules somehow; quoted/escaped
                    String inlineStyleString = null;
                    Matcher cm = curlyPattern.matcher(text);
                    if (cm.find()) {
                        inlineStyleString = cm.group(1).trim();
                        text = text.substring(0, cm.start()) + text.substring(cm.end());
                    }
                    
                    comments.add(new GvGraphComment(c, cu.getLineNumber(start), classes, text, inlineStyleString));
                    
                } else {
                    
                    fgm = gvClassPattern.matcher(text);
                    if (fgm.find()) {
                        // logger.info("groupCount " + fgm.groupCount());
                        if (fgm.group(1)!=null) {
                            classes.add(fgm.group(1).substring(1));
                        }
                        int pos = fgm.end(1);
                        Matcher gm = gvNextClassPattern.matcher(fgm.group(0));
                        logger.info(pos);
                        while (pos!=-1 && gm.find(pos)) {
                            classes.add(gm.group(1).substring(1));
                            pos = gm.end(1);
                            logger.info(pos);
                        }
                        text = text.substring(fgm.end());
                        // System.out.println("classes " + classes + " in " + text); 

                        // if there's anything in curly brackets remaining, then that's a style rule.
                        // @TODO handle curlies outside of style rules somehow; quoted/escaped
                        String inlineStyleString = null;
                        Matcher cm = curlyPattern.matcher(text);
                        if (cm.find()) {
                            inlineStyleString = cm.group(1).trim();
                            text = text.substring(0, cm.start()) + text.substring(cm.end());
                        }
                        
                        comments.add(new GvComment(c, cu.getLineNumber(start), classes, text, inlineStyleString));
                        
                    }
                    
                }
            }
         
        }
        return comments;
    }
    
 // when we construct the DagNodes, automatically add classes based on AST type
    // and line number, which will make colouring these things in from jacoco output that much simpler
    // and whatever JVMTI uses, which is probably line numbers as well
    
    public CSSStyleSheet getStyleSheet(List<CommentText> comments, String baseCssHref) throws IOException {
        String css = "@import \"" + baseCssHref + "\";\n";
        for (CommentText c : comments) {
            // String t = c.text;
            if (c instanceof GvStyleComment) {
                // remove non-standard css comments
                String[] ss = ((GvStyleComment) c).style.split("\n");
                for (int i=0; i<ss.length; i++) {
                    String s = ss[i];
                    if (s.contains("//")) {
                        s = s.substring(0, s.indexOf("//"));
                    }
                    css += (i==0 ? "" : "\n") + s;
                }
            
            } else if (c instanceof GvComment) {
                // could check inline styles here maybe
                
            }
        }
        
        
        logger.info("here's the css: " + css);
        
        // pre-process the // comments out of the block comments, which isn't standard CSS
        
        CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
        InputSource source = new InputSource(new StringReader(css));
        CSSStyleSheet stylesheet = parser.parseStyleSheet(source, null, null);
        
        CSSStyleSheetImpl stylesheetImpl = (CSSStyleSheetImpl) stylesheet;
        RestrictedCssStyleSheetImpl restrictedStylesheetImpl = new RestrictedCssStyleSheetImpl(stylesheetImpl); 
        // probably need to wrap this in something which overrides importImports
        // public void importImports(final boolean recursive) throws DOMException {
        // or provide my own URI handler and set a weird base URI 
        // stylesheetImpl.importImports(true); // recursive = true
        restrictedStylesheetImpl.importImports(true);
        logger.info("CSS rules: " + restrictedStylesheetImpl.getCssText());
        
        return restrictedStylesheetImpl;
    }
    

}