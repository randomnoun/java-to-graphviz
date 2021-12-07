
# java-to-graphviz

**java-to-graphviz** takes your java source code and converts it into graphviz diagrams.

This is the sort of thing you might want to do when you take a look at some piece of code and realise it's a completely 
unmaintainable mess but it still needs to be documented somehow.

It uses the eclipse parser, so should be able to handle the newer java language features, 
such as lambdas and inner classes. I'm aware inner classes were added back in the java 1.2 era so 
probably isn't considered a new language feature any more.

Anyway. You can annotate the generated diagram using specially-formatted comments, because I think annotations
are an abomination. You can probably use annotations as well later on once I've implemented that.

So anyway here's the type of output it can produce:

|  AST node |  Sample code | Diagram |
|--|--|:--:|
| Block | <pre>// gv-graph<br/>{ // gv: Block edges<br/>    a();<br/>    b();<br/>    c();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-0.png) | 
| If | <pre>// gv-graph<br/>{ // gv: If edges<br/>    before();<br/>    if (condition) {<br/>        truePath();<br/>    } else {<br/>        falsePath();<br/>    }<br/>    after();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-1.png) |
| For | <pre>// gv-graph<br/>{ // gv: For edges<br/>    before();<br/>    for (i = 0; i &lt; 10; i++) {<br/>        println(i);<br/>    }<br/>    after();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-2.png) |
| EnhancedFor | <pre>// gv-graph<br/>{ // gv: EnhancedFor edges<br/>    before();<br/>    for (int e : elements) {<br/>        println(e);<br/>    }<br/>    after();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-3.png) |
| While | <pre>// gv-graph<br/>{ // gv: While Edges<br/>    before();<br/>    while (condition) {<br/>        println(i);<br/>    }<br/>    after();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-4.png) |
| Do | <pre>// gv-graph<br/>{ // gv: Do Edges<br/>    before();<br/>    do {<br/>        println(i);<br/>    } while (condition);<br/>    after();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-5.png) |
| Switch | <pre>// gv-graph<br/>{ // gv: Switch Edges<br/>    before();<br/>    switch(i) {<br/>        case 0: println(); // fallthrough<br/>        case 1: println(); break; <br/>        default: println();<br/>    }<br/>    after();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-6.png) |
| Switch<br/>(alternate)| <pre>// gv-graph<br/>// gv-option: centralSwitch=true<br/>{ // gv: Switch Edges (alternate)<br/>    before();<br/>    switch(i) {<br/>        case 0: println(); // fallthrough<br/>        case 1: println(); break; <br/>        default: println();<br/>    }<br/>    after();<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-7.png) |
| InfixExpression | <pre>// gv-graph<br/>{ // gv: InfixExpression edges<br/>    println(1 + 2 / 3);<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-8.png) |
| UnaryExpression | <pre>// gv-graph<br/>{ // gv: UnaryExpression edges<br/>    i++;<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-9.png) |
| TernaryExpression | <pre>// gv-graph<br/>{ // gv: TernaryExpression edges<br/>    println(condition ? i : j);<br/>}</pre> | ![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/com.example.input.AllTheControlFlowNodes-10.png) |

[//]: # (The markdown source for this table is an abomination. I blame github.)

and this is what it looks like when you throw that all together:


![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/example-complicated.png)

You obviously won't want that, so it suppresses nodes by default, to give you just the nodes you're interested in.

![](http://gitlab.dev.randomnoun/randomnoun/java-to-graphviz/raw/master/src/site/readme/example-simple.png)

Here's a maven plugin to generate these things during your build process. Include them in your javadoc via

    /** Something like this <img src="the-diagram.png"/> */
    
Here's another plugin which colours in the nodes using jacoco output.

Here's another eclipse plugin which hooks in via JVMTI to do that in realtime.

Here's another plugin which annotates the nodes with generated bytecodes.

Here's another plugin which adds in AOP nodes.

And so on.

# Syntax

The general gist of the thing is that you can add comments inside your source code in order to shape the diagram that's generated.

Those comments start with "gv:" ( or variations on that ) to differentiate them from your normal run-of-the-mill commentary.

To change the label on the diagram, supply some text after the "gv:", e.g. "gv: order some donuts" would appear as

[ order some donuts ]

You can include individual style rules on the "gv:" comment by putting them in curly braces; e.g. "gv: order some donuts { color: blue }" would appear as 

[ order some donuts ]

You can also apply styles to various nodes using the graphviz equivalent of CSS, in which style rules are defined in a "gv-style:" block,
and then you can assign classes and IDs in your gv comments to apply those rules. 

I'm using a real CSS parser here, so all the fiddly bits around specificity should apply to those rules, assuming you know them, 
which you should if you're born any time past the 1990s.

/* gv-style: {
      // some rules 
      .something { color: blue; }
      .special { color: red; }
      #unique { color: green; }
  } 
*/

begin(); // gv: the beginning { fillcolor: something; } 
someCode(); // gv.something: well hello there
someOtherCode(true); // gv.something.special: well hello there again
someOtherCode(false); // gv#unique: righteo then

Those styles are applied to a pretend DOM that is created separate from the graphviz diagram; the style rules you create are applied to the imaginary DOM and then the calculated styles are used in the generated diagram.

The DOM looks a little bit like the AST of the program, but different.

[ the dom ]

There are some elements and attributes in that DOM which are created automatically from the Java source, those are:

[ list of those ]

Meaning you can turn all your 'if' statements to diamonds via

/* gv-style: { .if { shape: diamond; } } */

You can create multiple graphs per source file, and multiple subgraphs per graph, by using some special CSS rules which affect the DOM. 

So you could put all your try/catch statements in subgraphs via

/* gv-style: { .try { something; } } */

Or a subgraph around a particularly exciting bit of code via

// that

CSS doesn't normally do this ( unless you don't include ::content pseudo-CSS ), so in order to be able to style the DOM elements that are created by these CSS rules,
there are multiple passes of the CSS. 




# Features

DONE
* style rules
* external style refs (classpath, TODO urls, files)
* label formats
* css-defined subgraphs
* comment-defined subgraphs
* a single .java source file can contain multiple graphs and subgraphs
* can put literal gv into the diagram
* few builtin methods/style properties/declarations to make this a bit easier to generate diagrams; e.g. wordwrap, flip logic on if edge labels, probably others
* comment attribution 

TODO

So a few things I want to be able to support

* bit of flexibility when it comes to how the diagram is generated ( node suppression , artificial nodes, that sort of thing )
* seeing I"m going to the trouble of creating a DOM to apply styles maybe dump that as well, &/or the AST tree. apply styles to AST nodes vs dag nodes ?
* bunch up the exit nodes if there's lots of them
* lambdas, fluent methods

URLs to throw around in the description:

programcreek sample code to extract comments: 
https://www.programcreek.com/2013/03/get-internal-comments-by-using-eclipse-jdt-astparser/ 
the good ol' java 1.5 javacc grammar :
https://github.com/javacc/javacc/blob/master/examples/JavaGrammars/1.5/Java1.5.jj
a wayback copy of grammars of yesteryear:
https://web.archive.org/web/20130128033907/http://java.net/projects/javacc/downloads?page=2&path%5B%5D=contrib&path%5B%5D=grammars&theme=java.net
sketchviz: https://sketchviz.com/new



