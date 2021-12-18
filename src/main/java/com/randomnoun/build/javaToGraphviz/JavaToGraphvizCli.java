package com.randomnoun.build.javaToGraphviz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.randomnoun.common.Text;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

/** CLI for JavaToGraphviz
 * 
 */
public class JavaToGraphvizCli {

    Logger logger = Logger.getLogger(JavaToGraphvizCli.class);


    public static String usage() {
        return 
          "Usage: \n" +
          "  java -jar java-to-graphviz-cli.jar [options] filename filename ... \n" +
          // "  java " + JavaToGraphvizCli.class.getName() + " [options] filename filename ... \n" +
          "\n" + 
          "where [options] are:\n" +
          " -h -?                       display this help text\n" +
          " --verbose  -v               increase verbosity ( info level )\n" +
          " --verbose2 -vv              increase verbosity a bit more ( debug level )\n" +
          " --format   -f dot|dom1|dom2 output format: DOT diagram (default), pre-styled DOM, post-styled DOM\n" +
          " --output   -o filename      send output to filename\n" +
          "                             For multiple output files, filename can contain\n" +
          "                               {basename} for base filename, {index} for diagram index\n" +
          "                             e.g. \"{basename}-{index}.dot\" for dot output,\n" +
          "                                  \"{basename}.html\" for dom output\n" +
          "                             default is stdout\n" +
          " --source   -s version       set Java source language version ( 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7,\n" +
          "                               8, 9, 10, 11, 12, 13, 14, 15, 16 )\n" +
          "\n" +
          " --base-css -b url           replace base css with url ( relative to classpath, then file:///./ )\n" +
          "                               default = JavaToGraphviz.css\n" +
          " --user-css -u url           add url to list of user css imports\n" +
          " --css      -c {rules}       add css rules\n" +
          "\n" +
          " --option   -p key=value     set initial option; can be modified in source by 'gv-option' and 'gv-keepNode' directives\n" +
          "\n" +
          "Options keys and defaults:\n" +
          " edgerNamesCsv=control-flow  csv list of edger names\n" +
          "                             possible edger names: control-flow, ast\n" +
          " enableKeepNodeFilter=false  if true, will perform node filtering\n" +
          " defaultKeepNode=true        if true, filter will keep nodes by default\n" +
          " keepNode=                   space-separated nodeTypes for fine-grained keepNode control\n" +
          "                             prefix nodeType with '-' to exclude, '+' or no prefix to include\n" +
          "                               e.g. \"-expressionStatement -block -switchCase\"\n" +
          "                               to exclude those nodeTypes when defaultKeepNode=true\n" +
          "                             NB: any node with a 'gv' comment will always be kept\n";
    }
    
    public static void main(String args[]) throws IOException {
        int verbose = 0;
        String format = "dot";
        String outputTemplate = null;
        
        String sourceVersion = "11";
        String baseCssUrl = "JavaToGraphviz.css";
        List<String> userCssUrls = new ArrayList<>();
        List<String> userCssRules = new ArrayList<>();
        List<File> inputFiles = new ArrayList<>();
        Map<String, String> options = new HashMap<>();
        
        if (args.length < 1) { 
            System.out.println(usage());
            throw new IllegalArgumentException("Expected input filename or options");
        }
        int argIndex = 0;
        while (argIndex < args.length && args[argIndex].startsWith("-")) {
            String a = args[argIndex];
            if (a.equals("--verbose") || a.equals("-v")) {
                verbose = 1;
                argIndex ++;
            } else if (a.equals("--verbose2") || a.equals("-vv")) {
                verbose = 2;
                argIndex ++;
            } else if (a.equals("--format") || a.equals("-f")) {
                format = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--output") || a.equals("-o")) {
                outputTemplate = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--source") || a.equals("-s")) {
                sourceVersion = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--options") || a.equals("-p")) {
                String t = args[argIndex + 1];
                argIndex += 2;
                
                // same as AstToDagVisitor.newOptions()
                Pattern optionPattern = Pattern.compile("([a-zA-Z0-9-_]+)\\s*=\\s*([a-zA-Z0-9-_]+)\\s*");
                Matcher m = optionPattern.matcher(t);
                while (m.find()) {
                    String k = m.group(1);
                    String v = m.group(2);
                    if (v.equals("unset")) {
                        options.remove(k);
                    } else {
                        options.put(k, v);
                    }
                }
                

            } else if (a.equals("--base-css") || a.equals("-b")) {
                baseCssUrl = args[argIndex + 1];
                argIndex += 2;
            } else if (a.equals("--user-css") || a.equals("-u")) {
                userCssUrls.add(args[argIndex + 1]);
                argIndex += 2;
            } else if (a.equals("--css") || a.equals("-c")) {
                userCssRules.add(args[argIndex + 1]);
                argIndex += 2;
            } else if (args[argIndex].equals("-h") || args[argIndex].equals("-?")) {
                System.out.println(usage());
                System.exit(0);
            } else {
                // here beginneth the filenames
                break;
            }
        }
        if (argIndex == args.length) {
            System.out.println(usage());
            throw new IllegalArgumentException("Expected input filenames");
        }
        while (argIndex < args.length) {
            String f = args[argIndex];
            argIndex++;
            File file = new File(f);
            if (!file.exists()) {
                throw new IllegalArgumentException("Input file '" + f + "' does not exist");
            } else if (!file.canRead()) {
                throw new IllegalArgumentException("Input file '" + f + "' is not readable");
            }
            inputFiles.add(file);
        }
        // option defaults and validation
        if (!(format.equals("dot") || format.equals("dom1") || format.equals("dom2"))) {
            throw new IllegalArgumentException("Unknown format '" + format + "'; expected 'dot', 'dom1' or 'dom2'");
        }
        /*
        if (outputTemplate == null) {
            if (format.equals("dot")) {
                outputTemplate = "{f}-{i}.dot";
            } else {
                outputTemplate = "{f}-{i}.html";
            }
        }
        */
        

        Log4jCliConfiguration lcc = new Log4jCliConfiguration();
        Properties props = new Properties();
        props.put("log4j.rootCategory", "FATAL, CONSOLE");
        if (verbose == 1) {
            props.put("log4j.rootCategory", "INFO, CONSOLE");
        } else if (verbose == 2) {
            props.put("log4j.rootCategory", "DEBUG, CONSOLE");
        }
        lcc.init("[JavaToGraphviz]", props);
        Logger logger = Logger.getLogger(JavaToGraphvizCli.class);

        
        for (File f : inputFiles) {
            JavaToGraphviz javaToGraphviz = new JavaToGraphviz();
            javaToGraphviz.setFormat(format);
            javaToGraphviz.setSourceVersion(sourceVersion);
            
            if (baseCssUrl != null) {
                javaToGraphviz.setBaseCssUrl(baseCssUrl);
            }
            javaToGraphviz.setUserCssUrls(userCssUrls);
            javaToGraphviz.setUserCssRules(userCssRules);
            
            // String className = "com.example.input.SwitchStatement2";
            // File f = new File("src/test/java/" + Text.replaceString(className,  ".",  "/") + ".java");
            logger.debug("Reading " + f.getCanonicalPath());            
            FileInputStream is = new FileInputStream(f);
            javaToGraphviz.parse(is, "UTF-8");
            is.close();
            String n = f.getName();
            if (n.indexOf(".") != -1) { n = n.substring(0, n.lastIndexOf(".")); }
            
            int diagramIndex = 0;
            boolean hasNext;
            do {
                String on = outputTemplate;
                if (on == null) {
                    StringWriter sw = new StringWriter();
                    hasNext = javaToGraphviz.writeGraphviz(sw);
                    System.out.println(sw.toString());
                    if (hasNext) { System.out.println("==========================="); }
                } else {
                    on = Text.replaceString(on, "{basename}", n);
                    on = Text.replaceString(on, "{index}", String.valueOf(diagramIndex));
                    logger.debug("Writing " + on);   
                    FileOutputStream fos = new FileOutputStream(on);
                    PrintWriter pw = new PrintWriter(fos);
                    hasNext = javaToGraphviz.writeGraphviz(pw);
                    pw.flush();
                    fos.close();
                }
                diagramIndex++;
            } while (hasNext);
        }
        
    }
   
	

}