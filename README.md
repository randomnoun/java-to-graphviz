[![Maven Central](https://img.shields.io/maven-central/v/com.randomnoun.build/java-to-graphviz.svg)](https://search.maven.org/artifact/com.randomnoun.build/java-to-graphviz)

# java-to-graphviz

**java-to-graphviz** takes your java source code and converts it into graphviz diagrams.

This is the sort of thing you might want to do when you take a look at some piece of code and realise it's a completely 
unmaintainable mess but it still needs to be documented somehow.

You can annotate the generated diagram using specially-formatted comments. 

Here's the type of output it can produce:

|  AST node |  Sample code | Diagram |
|--|--|:--:|
| Block | <pre>// gv-graph<br/>{ <br/>    a();<br/>    b();<br/>    c();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-0.png) | 
| If | <pre>// gv-graph<br/>{ <br/>    before();<br/>    if (condition) {<br/>        truePath();<br/>    } else {<br/>        falsePath();<br/>    }<br/>    after();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-1.png) |
| For | <pre>// gv-graph<br/>{ <br/>    before();<br/>    for (i = 0; i &lt; 10; i++) {<br/>        println(i);<br/>    }<br/>    after();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-2.png) |
| EnhancedFor | <pre>// gv-graph<br/>{ <br/>    before();<br/>    for (int e : elements) {<br/>        println(e);<br/>    }<br/>    after();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-3.png) |
| While | <pre>// gv-graph<br/>{ <br/>    before();<br/>    while (condition) {<br/>        println(i);<br/>    }<br/>    after();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-4.png) |
| Do | <pre>// gv-graph<br/>{ <br/>    before();<br/>    do {<br/>        println(i);<br/>    } while (condition);<br/>    after();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-5.png) |
| Switch | <pre>// gv-graph<br/>{ <br/>    before();<br/>    switch(i) {<br/>        case 0: println(); // fallthrough<br/>        case 1: println(); break; <br/>        default: println();<br/>    }<br/>    after();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-6.png) |
| Switch<br/>(alternate)| <pre>// gv-graph<br/>// gv-option: centralSwitch=true<br/>{ <br/>    before();<br/>    switch(i) {<br/>        case 0: println(); // fallthrough<br/>        case 1: println(); break; <br/>        default: println();<br/>    }<br/>    after();<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-7.png) |
| InfixExpression | <pre>// gv-graph<br/>{ <br/>    println(1 + 2 / 3);<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-8.png) |
| UnaryExpression | <pre>// gv-graph<br/>{ <br/>    i++;<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-9.png) |
| TernaryExpression | <pre>// gv-graph<br/>{ <br/>    println(condition ? i : j);<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-10.png) |
| InfixExpression (boolean shortcut) | <pre>// gv-graph<br/>{<br/>    println(a \|\| (b && c));<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-11.png) |
| AnonymousClassDeclaration | <pre>// gv-graph<br/>{<br/>    println(new Predicate<Integer>() {<br/>        @Override<br/>        public boolean test(Integer n) {<br/>            return n % 2 == 0;<br/>        }<br/>        <br/>    }.test(99));<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-12.png) |
| LamdaExpression | <pre>// gv-graph<br/>{<br/>    Predicate<Integer> isEven = (Integer n) -> n % 2 == 0;<br/>    println(isEven.test(99));<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-13.png) |
| TryStatement (with resources) | <pre>// gv-graph<br/>{<br/>    try (InputStream is =<br/>      new FileInputStream("test-in.txt")) {<br/>        OutputStream os =<br/>          new FileOutputStream("test-out.txt");<br/>    } catch (IOException ioe) {<br/>        println("ioexception");<br/>    } finally {<br/>        println("finally");<br/>    }<br/>}</pre> | ![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/com.example.input.AllTheControlFlowNodes-15.png) |


[//]: # (The markdown source for this table is an abomination. I blame github.)

and this is what it looks like when you string that all together:

![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/example-complicated.png)

You obviously won't want that, so it suppresses nodes by default, to give you just the nodes you're interested in.

![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/example-simple.png)

# Processing

Theres' a few different stages in the pipeline.

1. **Parse source file into an Abstract Syntax Tree (AST)**. 
This uses [the eclipse parser](https://git.eclipse.org/r/plugins/gitiles/jdt/eclipse.jdt.core/+/master/org.eclipse.jdt.core/dom/org/eclipse/jdt/core/dom/ASTParser.java), so should be able to handle the newer java language features, such as lambdas and inner classes. I'm aware inner classes were added back in the java 1.2 era so probably isn't considered a new language feature any more.

2. **Extract comments**. For reasons of efficiency, the eclipse parser doesn't store comments in the AST tree where the comments actually appear, they're in a separate list.
We scan this list for "gv" comments, and attribute the comment to the AST node it refers to.

3. **Generate Directed Acyclic Graph (DAG)**. This will store our graphviz representation of the AST, and initially looks very similar to the AST, with some additional metadata.
Note it's not strictly a DAG, as we can get loops due to for/while/do constructs, but any edges leading backwards are marked as 'back' edges so we can still treat it as a DAG later by ignoring these edges.

4. **Generate edges**. This recurses through the DAG and adds control flow edges to it. This may involve rejigging the DAG so that nodes that execute before others are located before them in the diagram.

5. **Filter nodes**. This recurses through the DAG and removes unnecessary nodes; e.g. "A -> B -> C" segments can be reduced to "A -> C". The filtering occurs around 'keepNodes' which must be kept in the final diagram. Users can control how nodes are filtered and kept using special "gv" comments in the source code.

6. **Create DOM**. The filtered DAG is converted into a HTML-like document object model (DOM). This represents the DAG structure in terms of "graph", "node" and "edge" elements, with attributes for user-supplied styles and labels, also supplied using special "gv" comments.

7. **Apply styles**. The DOM has a CSS stylesheet applied to it. **JavaToGraphviz** comes with a few builtin stylesheets, but you can replace or override these in a number of ways. During this step,
final node and edge IDs, labels, and formatting are determined. Additionally, extra subgraphs can be added into the diagram, which restructures the DOM and adds subgraphs into the DAG.

8. **Extract styles**. The applied styles in the DOM are copied back into the DAG.

9. **Create DOT**. The DAG is converted into graphviz "dot" syntax, which can then be used as input to the graphviz "dot" program to create the final diagram.


# Syntax

The general gist of the thing is that you can add comments inside your source code in order to shape the diagram that's generated.

Those comments start with "gv:" ( or variations on that ) to differentiate them from your normal run-of-the-mill commentary.

The gv directives can be one of the following:

| Syntax |  Example | Description |
|--|--|--|
| // gv: | <pre>// gv: open box</pre> | override the label for a node.<br/>If on it's own line can add additional nodes to the diagram |
| // gv: { css } | <pre>// gv: open box { color: green; shape: oval; }</pre> | override the label for a node, and supply additional formatting.<br/>The formatting is expressed as a CSS rule, but uses [graphviz attribute names](https://graphviz.org/doc/info/attrs.html) rather than the usual CSS property names |
| // gv#id: | <pre>// gv#openBox: open box </pre> | override the ID for a node.<br/>This can be used to make the graphviz output more understandable, or to apply styles in a separate stylesheet |
| // gv.class: | <pre>// gv#openBox.green.oval: open box </pre> | add CSS classes to a node.<br/>This can be used to make the graphviz output more understandable, or to apply styles in a separate stylesheet. Many classes may be assigned to a node, but only a single ID. Additional classes may be added by other parts of **JavaToGraphviz** (e.g. a class representing the node type is added when the DAG is constructed, so that all "if" nodes can later be shaped as diamonds ) |
| // gv-style: { rules } | <pre>/* gv-style: {<br/>  // CSS properties applied to all 'if' nodes<br/>  node.if {<br/>    gv-idFormat: "if_${lineNumber}";<br/>    gv-wordwrap: 20;<br/>    shape : diamond; <br/>  }<br/>  edge.if.true { label: "Y"; }<br/>  edge.if.false { label: "N"; }<br/>} */</pre> | defines CSS style rules.<br/>Most CSS property names correspond to graphviz attribute names, but those beginning with "gv-" are processed within **JavaToGraphviz** instead. |
| // gv-style: { @import } | <pre>// gv-style: { @import "JavaToGraphviz.css"; }</pre> | import a CSS stylesheet.<br/>To find the stylesheet, the classpath is searched first, then the local filesystem |
| // gv-keepNode: spec | <pre>// gv-keepNode: expressionStatement block</pre> | Changes the 'keepNode' flags when creating DAG nodes.<br/>If the default keepNode value is true, then individual nodes can be excluded by prefixing them with a minus sign; e.g. <pre>// gv-keepNode: -expressionStatement</pre> Also note that any node that has a <pre>// gv: comment</pre> will be kept, regardless of the keepNode flags. |
| // gv-literal: dot | <pre>// gv-literal: { rank = same; case1; case2; }</pre> | Adds graphviz 'dot' code directly into the final graphviz diagram |
| // gv-subgraph | <pre>// gv-subgraph: something noteworthy</pre> | Starts a subgraph in the diagram.<br/>Subgraphs can have borders and other formatting applied, and can be used to highlight different parts of the code. Subgraphs can be nested. |
| // gv-endSubgraph | <pre>// gv-endSubgraph</pre> | Closes a subgraph<br/>You probably want to do this at the same depth in the AST tree; e.g. in the same block that opened the subgraph. |
| // gv-graph | <pre>// gv-graph: something separate</pre> | Starts a new graphviz diagram.<br/>The first time this is encountered, everything before this line is discarded, and this line marks the beginning of a new diagram.<br/>If omitted, the entire class/interface is drawn. |
| // gv-endGraph | <pre>// gv-endGraph</pre> | Closes a graph.<br/>Nodes will not be processed until a new graph is created with gv-graph. |
| // gv-option key=value | <pre>// gv-option: centralSwitch=true</pre> | Options can be used to change how **JavaToGraphviz** creates nodes and edges |

Here's all that again in a bit more detail.

## Labels

To change the label on the diagram, supply some text after the "gv:", e.g. 
<pre>
orderDonuts();  // gv: order some donuts
</pre>
would appear as

![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/style-1.png)

## Styles

You can include individual style rules on the "gv:" comment by putting them in curly braces; e.g. 

<pre>
orderDonuts();  // gv: order some donuts { shape: oval; color: blue; fontcolor: blue; }
</pre>

![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/style-2.png)

## Style sheets

You can also apply styles to various nodes using the graphviz equivalent of CSS, in which style rules are defined in a "gv-style:" block, and then classes and IDs are assigned to AST nodes in your gv comments to apply those rules. 

I'm using a real CSS parser here, so all the [fiddly bits around specificity](https://developer.mozilla.org/en-US/docs/Web/CSS/Specificity) should apply to those rules, assuming you know them, which you should if you're born any time past the 1990s. 

e.g.
<pre>
/* gv-style: {
       // some rules 
       .something { color: red; }
       .special { color: green; }
       #unique { shape: hexagon; }
   } 
*/

before(); // gv: the beginning { fillcolor: grey; } 
someCode(); // gv.something: well hello there
someOtherCode(true); // gv.something.special: well hello there again
someOtherCode(false); // gv#unique.something: righteo then
</pre>
would appear as:

![](https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/site/readme/style-3.png)

Note `!important` rules aren't implemented yet.

## Style DOM

Those styles are applied to a pretend DOM that is created separate from the graphviz diagram; the style rules you create are applied to the imaginary DOM and then the calculated styles are used in the generated diagram.

The DOM looks a little bit like the AST of the program, but different. Here's a snippet **before** styles are applied.

<pre>
&lt;html&gt;
 &lt;head&gt;&lt;/head&gt;
 &lt;body&gt;
  &lt;graph&gt;
   &lt;graphNode&gt;&lt;/graphNode&gt;
   &lt;graphEdge&gt;&lt;/graphEdge&gt;
   &lt;node id label classname="ForStatement" class="typeDeclaration class"&gt;
    &lt;node id label class="simpleName"&gt;&lt;/node&gt;
    &lt;node id label methodname="testFor" class="methodDeclaration"&gt;
     &lt;node id label class="simpleName"&gt;&lt;/node&gt;
     &lt;node id label class="block"&gt;
      &lt;node id label=" start of method" class="comment"&gt;&lt;/node&gt;
      &lt;edge&gt;&lt;/edge&gt;
      &lt;node id label class="for"&gt;
       &lt;node id label type="int" class="initialiser variableDeclarationExpression"&gt;
        &lt;node id label variablename="i" type="int" class="variableDeclarationFragment"&gt;
         &lt;node id label class="simpleName"&gt;&lt;/node&gt;
         &lt;node id label literalvalue="0" class="numberLiteral literal"&gt;&lt;/node&gt;
         &lt;edge&gt;&lt;/edge&gt;
        &lt;/node&gt;
        &lt;edge&gt;&lt;/edge&gt;
       &lt;/node&gt;
       &lt;edge&gt;&lt;/edge&gt;
       &lt;node id label operatortoken="&lt;" operatorname="InfixExpression$Operator" class="expression infixExpression"&gt;
        &lt;node id label name="i" class="simpleName"&gt;&lt;/node&gt;
        &lt;edge&gt;&lt;/edge&gt;
        &lt;node id label literalvalue="12" class="numberLiteral literal"&gt;&lt;/node&gt;
        &lt;edge&gt;&lt;/edge&gt;
       &lt;/node&gt;
</pre>

There are some elements and attributes in that DOM which are created automatically from the Java source, those are:

| Element | Description |
|--|--|
| html, head, body | These HTML elements exist solely so that I can use the [jsoup CSS selector code](https://jsoup.org/cookbook/extracting-data/selector-syntax) to apply CSS rules to the DOM |
| graph | The `graph` element represents an entire graphviz diagram. Styles applied to this node are set on the graphviz `digraph` |
| graphNode | The `graphNode` element represents the [default node styles](https://graphviz.org/doc/info/lang.html). Styles applied to this node are set on a `node` object in the `digraph`. <br/>Note you could also apply styles to all nodes by using a `.node` selector in a CSS rule. There's pros and cons to each approach. |
| graphEdge | The `graphEdge` element represents the [default edge styles](https://graphviz.org/doc/info/lang.html). Styles applied to this node are set on an `edge` object in the `digraph`.  <br/>Note you could also apply styles to all edges by using an `.edge` selector in a CSS rule. There's pros and cons to each approach. |
| node | Each `node` represents a node in the DAG, which usually represents a node in the AST. `node`s created from AST nodes will have a class attribute containing the AST type ( e.g. `methodDeclaration` for a [MethodDeclaration](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/MethodDeclaration.html) ) |
| edge | Each `edge` represents an edge in the DAG. Edges may have other classes assigned by the specific Edger implementation ( e.g. the ControlFlowEdger will add 'true' and 'false' classes to edges leading out of 'if' nodes ) |

There are a few **standard attributes** on every node / edge:

| Attribute | Description |
|--|--|
| id | Each node has an id attribute which can be used to select that node from external CSS. If blank, it will be set using `gv-idFormat` rules in the CSS |
| label | Each node has a label attribute which populates the graphviz 'label' attribute. If blank, it will be set using `gv-labelFormat` rules in the CSS |
| class | Each node and edge may have additional classes defined by the Edger or by the user in gv comments |
| style | Each node and edge may have additional styles defined by the user in gv comments |

**Attributes** added to various **nodes** by the ControlFlowEdger. 

These attributes can also be referenced within `gv-idFormat` and `gv-labelFormat` style rules using `${xxx}` syntax.

| nodeType className | Attribute | Attribute value |
|--|--|--|
| typeDeclaration | className | The name of the Java class |
| typeDeclaration | interfaceName | The name of the Java interface |
| methodDeclaration | methodName | The name of the method |
| catchClause | exceptionSpec | The type and name of the caught exception |
| variableDeclaration | type | The type of the variable ( is repeated on each variableDeclarationFragment ) |
| variableDeclaration | variableName | The name of the variable |
| singleVariableDeclaration | type | The type of the variable |
| singleVariableDeclaration | variableName | The name of the variable |
| booleanLiteral, numberLiteral, characterLiteral, nullLiteral, typeLiteral, stringLiteral  | literalValue | The literal value in the source code |
| constructorInvocation | methodName | "this" |
| superConstructorInvocation | methodName | "super" |
| methodInvocation | methodName | the method name |
| superMethodInvocation | methodName | qualifier + ".super." + method name |
| infixExpression | operatorToken | the operator token ( wip, will be "*", "/", "\|\|" etc ) |
| infixExpression | operatorName |  a more css-friendly operator name ( wip, will be "times", "divide", "conditionalOr" etc  ) |
| postfixExpression | operatorToken | the operator token ( wip, will be "++" etc ) |
| postfixExpression | operatorName |  a more css-friendly operator name ( wip, will be "increment" etc  ) |
| prefixExpression | operatorToken | the operator token ( wip, will be "++" etc ) |
| prefixExpression | operatorName |  a more css-friendly operator name ( wip, will be "increment" etc  ) |
| castExpression | type | the type being cast to |
| instanceofExpression | type | the right operand type name |
| assignment | operatorToken | the operator token ( wip, will be "=", "+=" etc ) |
| assignment | operatorName | a more css-friendly operator name ( wip, will be "assign", "plusAssign" etc ) |
| fieldAccess | fieldName | the name of the field |
| superFieldAccess | fieldName | qualified super field |
| classInstanceCreation | type | the type of the class being created |
| arrayCreation | type | the type of the array being created |
| variableDeclarationExpression | type | The type of the variable |
| name | name | the fully qualified name |
| thisExpression | name | qualified this |
| creationReference, expressionMethodReference, superMethodReference, typeMethodReference | name | reference name |

**Attributes** added to various **edges** by the ControlFlowEdger. 

These attributes can also be referenced within `gv-labelFormat` and `gv-xlabelFormat` style rules using `${xxx}` syntax.

| nodeType className | Attribute | Attribute value |
|--|--|--|
| break | breakLabel | if a target label was included in the `break` statement, that target label, otherwise an empty string  |
| continue | continueLabel | if a target label was included in the `continue` statement, that target label, otherwise an empty string  |

**Classes** added to various **nodes** and **edges** by the ControlFlowEdger:

| nodeType className | Element | className | Description |
|--|--|--|--|
| typeDeclaration | node | interface | The type declaration is for an interface |
| typeDeclaration | node | class | The type declaration is for a class |
| - | node | method, end | An artifical node is created at the end of each methodDeclaration ( so that all the 'return' edges have something to flow to ), which has `method` and `end` classes  |
| break | edge | break | The edge from a break statement will have the break class |
| continue | edge | continue | The edge from a break statement will have the continue class |
| return | edge | return | The edge from a return statement will have the return class |
| throw | edge | throw | The edge from a throw statement will have the throw class |
| if | edge | if, true | The edge connecting an if expression to the if body will have `if` and `true` classes |
| if | edge | if, false | The edge connecting an if expression to the else body (or the next statement if there is no else) will have `if` and `false` classes |
| for | edge | for, true | The edge connecting a for expression to the repeating block will have `for` and `true` classes |
| for | edge | for, false | The edge connecting a for expression to the next statement will have `for` and `false` classes |
| for | edge | for, back | The back edge connecting the end of a repeating block back to the loop updater nodes will have `for` and `back` classes |
| enhancedFor | edge | enhancedFor, true | The edge connecting an enhancedFor expression to the repeating block `enhancedFor` and `true` classes |
| enhancedFor | edge | enhancedFor, false | The edge connecting an enhancedFor expression to the next statement will have `enhancedFor` and `false` classes |
| enhancedFor | edge | enhancedFor, back | The back edge connecting the end of a repeating block back to the loop iterator will have `enhancedFor` and `back` classes |
| while | edge | while, true | The edge connecting a while expression to the repeating block will have `while` and `true` classes |
| while | edge | while, false | The edge connecting a while expression to the next statement will have `while` and `false` classes |
| while | edge | while, back | The back edge connecting the end of a repeating block back to the while condition will have `while` and `back` classes |
| do | edge | do, back, true | The back edge connecting a do expresson to the start of the repeating block will have `do`, `back` and `true` classes |
| do | edge | do, false | The edge connecting the end of a repeating block to the next statement will have `do` and `false` classes |`back` and `true` classes |
| switch | node | centralSwitch | If the 'centralSwitch' option is true, then the switch node will have the `centralSwitch` classes.<br/>When this option is in effect, all switch cases originate from the switch node, otherwise a series of if-like case nodes are created. |
| switchCase | node | centralSwitch | Is set if 'centralSwitch' is true |
| switchCase | edge | switchCase | If the 'centralSwitch' option is true, the edge from the switch node to the switchCase node will have the `switchCase` class |
| switchCase | edge | switchCase, true | If the 'centralSwitch' option is false, the edge from the switchCase node to the switchCase body will have the `switchCase` and `true` classes |
| switchCase | edge | switchCase, false | If the 'centralSwitch' option is false, the edge from the switchCase node to the next switchCase node will have the `switchCase` and `false` classes |
| switchCase | edge | switchCase, default | The default case will have `switchCase` and `default` classes |
| switchCase | edge | switch, fallthrough | A fallthrough edge from one switch case to the next will have `switch` and `fallthrough` classes |
| switch | edge | switch, false | If the switch does not have a default case, an edge from the switch node to the next statement will have the `switch` and `false` classes |
| lambdaExpression | edge | lambdaEntry | The edge from the lambdaExpression node to the block will have a `lambdaEntry` class |
| - | node | lambdaExpression, end | An artifical node is created at the end of each lambdaExpression ( so that all the 'return' edges have something to flow to ), which has `lambdaExpression` and `end` classes  |
| booleanLiteral, numberLiteral, characterLiteral, nullLiteral, typeLiteral, stringLiteral  | node | literal | All literalExpression subclasses have a `literal` class |
| constructorInvocation | edge | invocationArgument | the edges between argument expressions in a constructor will have an `invocationArgument` class |
| superConstructorInvocation | edge | invocationArgument | the edges between argument expressions in a super constructor will have an `invocationArgument` class |
| methodInvocation | edge | invocationArgument | the edges between argument expressions in a method call will have an `invocationArgument` class |
| superMethodInvocation | edge | invocationArgument | the edges between argument expressions in a super method call will have an `invocationArgument` class |
| infixExpression ( &&, \|\| ) | node | infixConditional | shortcut infix expressions (that may not evaluate the second expression) have an `infixConditional` class |
| infixExpression ( \|\| ) | edge | infixConditional, true | for shortcut 'or' expressions, the 'true' edge that shortcuts the next expression |
| infixExpression ( \|\| ) | edge | infixConditional, false | for shortcut 'or' expressions, the 'false' edge that evaluates the next expression |
| infixExpression ( && ) | edge | infixConditional, true | for shortcut 'and' expressions, the 'true' edge that evaluates the next expression |
| infixExpression ( && ) | edge | infixConditional, false | for shortcut 'and' expressions, the 'false' edge that shortcuts the next expression |
| conditionalExpression ( ? ) | edge | conditionalExpression, true | for '?' expressions, the 'true' edge connecting the condition to the true expression |
| conditionalExpression ( ? ) | edge | conditionalExpression, false | for '?' expressions, the 'false' edge connecting the condition to the false expression |
| classInstanceCreation | edge | invocationArgument | the edges between argument expressions in a 'new' expression will have an `invocationArgument` class |
| arrayInitializer | edge | invocationArgument | the edges between array element expressions in an array initializer |
| - | node | anonymousClassDeclaration, end | An artifical node is created at the end of each anonymousClassDeclaration, so that we can draw a transparent edge from the end of each method to that node, which helps make the diagram look pretty. |
| - | edge | anonymousClassDeclarationEnd | The artificial edge from the end of each method to the artifical node. This isn't a control flow edge, so should be transparent in the CSS. |
| anonymousClassDeclaration | edge | anonymousClassDeclarationBegin | The edge from the anonymous class declaration node to the start of each method/field declaration in that class |
| ast | node | ast | All unrecognised nodes will be edged by the AstEdger, and have an 'ast' class. You shouldn't see any of these in the ControlFlowEdger any more |

classNames in the DOM that are generated from AST nodes are listed below. These classNames are the eclipse parser AST class name name with the word "Statement" removed and their first letter lower-cased; e.g. IfStatement nodes are given the className 'if'. ExpressionStatements retain the "Statement" suffix, to differentiate ExpressionStatements from Expressions.

* [`typeDeclaration`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/TypeDeclaration.html)
* [`methodDeclaration`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/MethodDeclaration.html)
* [`variableDeclarationFragment`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/VariableDeclarationFragment.html)
* [`block`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/Block.html)
* [`synchronized`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/Synchronized.html)
* [`if`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/IfStatement.html)
* [`try`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/TryStatement.html)
* [`for`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ForStatement.html)
* [`enhancedFor`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/EnhancedForStatement.html)
* [`switch`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SwitchStatement.html)
* [`switchCase`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SwitchCase.html)
* [`break`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/BreakStatement.html)
* [`while`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/WhileStatement.html)
* [`continue`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ContinueStatement.html)
* [`do`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/DoStatement.html)
* [`return`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ReturnStatement.html)
* [`throw`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ThrowStatement.html)
* [`catchClause`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/CatchClause.html)
* [`labeled`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/LabeledStatement.html)
* [`expressionStatement`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ExpressionStatement.html)
* [`variableDeclaration`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/VariableDeclaration.html)
* [`singleVariableDeclaration`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SingleVariableDeclaration.html)
* [`constructorInvocation`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ConstructorInvocation.html)
* [`superConstructorInvocation`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SuperConstructorInvocation.html)
* [`assert`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/AssertStatement.html)
* [`empty`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/EmptyStatement.html)
* `comment`


All expressions are subclasses of Expression. The expression classNames are:

* literal-ish
    * [`booleanLiteral`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/BooleanLiteral.html)
    * [`characterLiteral`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/CharacterLiteral.html)
    * [`numberLiteral`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/NumberLiteral.html)
    * [`nullLiteral`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/NullLiteral.html)
    * [`typeLiteral`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/TypeLiteral.html)
    * [`stringLiteral`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/StringLiteral.html)
* name-ish
    * [`simpleName`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SimpleName.html)
    * [`qualifiedName`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/QualifiedName.html)
    * [`thisExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ThisExpression.html)
    * [`creationReference`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/CreationReference.html)
    * [`expressionMethodReference`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ExpressionMethodReference.html)
    * [`superMethodReference`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SuperMethodReference.html)
    * [`typeMethodReference`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/TypeMethodReference.html)
* control flow-ish
    * [`methodInvocation`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/MethodInvocation.html)
    * [`superMethodInvocation`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SuperMethodInvocation.html)
    * [`conditionalExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ConditionalExpression.html)
    * [`lambdaExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/LambdaExpression.html)
* evaluation-ish    
    * [`infixExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/InfixExpression.html)
    * [`parenthesizedExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ParenthesizedExpression.html)
    * [`prefixExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/PrefixExpression.html)
    * [`postfixExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/PostfixExpression.html)
    * [`instanceofExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/InstanceofExpression.html)
    * [`castExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/CastExpression.html)
* creation-ish
    * [`variableDeclarationExpression`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/VariableDeclarationExpression.html)
    * [`arrayCreation`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ArrayCreation.html)
    * [`arrayInitializer`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ArrayInitializer.html)
    * [`classInstanceCreation`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ClassInstanceCreation.html) ( also control flow-ish )
* access-ish    
    * [`fieldAccess`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/FieldAccess.html)
    * [`arrayAccess`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/ArrayAccess.html)
    * [`superFieldAccess`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/SuperFieldAccess.html)
* modification-ish
    * [`assignment`](https://www.ibm.com/docs/en/rational-soft-arch/9.5?topic=SS8PJ7_9.5.0/org.eclipse.jdt.doc.isv/reference/api/org/eclipse/jdt/core/dom/Assignment.html)


Note this still isn't a complete list of all AST nodes yet, but it's most of them.

Artifical nodes created by the ControlFlowEdger are:

| className | Description |
|--|--|
| methodDeclarationEnd | added to the end of methodDeclaration blocks, all return and throw edges will connect to this node |
| lambdaExpressionEnd | added to the end of lambdaExpression blocks, all return and throw edges will connect to this node |
| anonymousClassDeclarationEnd | added to the end of anonymousClassDeclaration blocks, all methods within the anonymous class will have an invisible edge connected to this node for layout purposes  ( these should be styled as transparent or invisible as they're not part of the control flow diagram ) |
| infixExpressionCondition | added within shortcut infixExpression nodes to branch the control flow based on the value of the left-hand-side operand |

**Debug attributes** added to various **nodes** by the DagStyleApplier. 

These attributes can be referenced within `gv-idFormat`, `gv-labelFormat` and `gv-xlabelFormat` style rules using `${xxx}` syntax, but can't be used in CSS selectors. Most of these are only appear in the [JavaToGraphviz-debug.css](src/main/resources/JavaToGraphviz-debug.css) stylesheet, apart from ${lineNumber} which is used in all stylesheets.

| Attribute | Description |
|--|--|
| lineNumber | The starting line number of this node in the source code. As these line numbers are used to generate default IDs, they are suffixed with an incrementing integer to allow multiple nodes on the same source line. e.g. "100", "100_2", "100_3" etc |
| nodeType | The AST node type |
| keepNode | "true" if this node is a keeper, "false" otherwise |
| lastKeepNodeId | the id of the last seen node connected to this node with keepNode="true" |
| numComments | the number of gv comments attributed to this node |

## Style properties

Users can create CSS rules based on the values of these elements, attributes and classes, which are then applied to the DOM and merged with the any 'style' attributes.

Most properties correspond to graphviz attributes; see the [Graphviz documentation](https://graphviz.org/doc/info/attrs.html) for a complete list of these.

Some additional properties are handled by **JavaToGraphviz** itself, these are all namespaced with a "gv-" prefix.

Prefixed properties include:

| Property | Description |
|--|--|
| gv-idFormat | The `gv-idFormat` is used to assign IDs to nodes. These IDs are visible in the DOM, and in the generated graphviz diagram. <br/>Other attributes can be included in the idFormat by specifying their name in curly brackets, <br/>For example, the stylesheet <pre>node {<br/>  gv-idFormat: "s_${lineNumber}";<br/>}<br/>node.if {<br/>  gv-idFormat: "if_${lineNumber}";<br/>}</pre> will assign an ID of `"s_"` followed by the line number of the node, or `"if_"` for nodes representing 'if' statements. <br/>As these `${lineNumber}`s are used to generate default IDs, they are suffixed with an incrementing integer to allow multiple nodes on the same source line. |
| gv-labelFormat | The `gv-labelFormat` is used to assign labels to nodes. These IDs are visible in the DOM, and in the generated graphviz diagram. <br/>Other attributes can be included in the idFormat by specifying their name in curly brackets, similar to the gv-idFormat. <br/>The labelFormat may include the id, or the id may include the label, but they may not include each other.
| gv-wordwrap | The `gv-wordWrap` property can be used to force newlines to be added into the node label. <br/>This is useful for some graphviz shape types ( e.g. diamond ), which become overly stretched if their label is wide. |
| gv-newSubgraph | Create a new subgraph 'underneath' the selected element |
| gv-beginOuterSubgraph | Create a new subgraph 'above' the selected element, then move the selected element and all nodes to gv-endSubgraph into that subgraph |
| gv-endOuterSubgraph | Marks the end of the subgraph created by `gv-beginOuterSubgraph`. |
| gv-truncateEdges | Either "incoming", "outgoing", "none" or "both". Will truncate the edges leading into or out of a subgraph. Must be applied to the same element that had the gv-newSubgraph property. Edges will begin/end outside the subgraph boundary |
| gv-xlabelFormat | Similar to `gv-labelFormat` but sets the [`xlabel`](https://graphviz.org/docs/attrs/xlabel/) attribute, which causes the label to be ignored during graphviz edge routing |

Note that the `gv-newSubgraph` style property ( defined in the CSS ) is not the same as the `gv-subgraph` directive discussed earlier ( defined in the source code ), although they perform similar tasks. The `gv-newSubgraph` CSS property allows subgraphs to be created by matching style rules ( e.g. enclosing all try/catch/finallys in a subgraph rectangle ) whereas the `gv-subgraph` directive allows the user to specify one-off subgraphs in their source code to highlight specific blocks of code.

## Style examples

This combination of elements, attributes and properties means you can turn all your 'if' statements to diamonds via:

<pre>
/* gv-style: { .if { shape: diamond; } } */
</pre>

## Subgraphs

Typically each method in a class is graphed as it's own subgraph. 

You can also construct subgraphs within methods to highlight or separate various nodes in the diagram. These subgraphs are created using some special CSS rules which affect the DOM. 

So for example, you could put all your try/catch statements in subgraphs via

<pre>
/* gv-style: { 
    node.try { gv-newSubgraph: true; }
    node.try > subgraph {
      // subgraph ids must being with the text "cluster_" for graphviz to draw it as a subgraph
      gv-idFormat : "cluster_t_${lineNumber}";
      gv-labelFormat : "try";
      pencolor : black; 
      labeljust : "l"; 
    }
} */
</pre>

but you probably want to put the 'tryResources', 'tryBody', 'catchClause' and 'finally' classes in subgraphs instead; see [JavaToGraphviz.css](src/main/resources/JavaToGraphviz.css)

You can also create subgraphs to highlight a particularly exciting bit of Java source code; e.g. 

<pre>
System.out.println("I like traffic lights");
System.out.println("I like traffic lights");
System.out.println("I like traffic lights");

// gv-subgraph: full orchestral backing
        
System.out.println("I like traffic lights");
System.out.println("I like traffic lights");
System.out.println("I like");
        
// gv-endSubgraph
        
System.out.println("traffic lights");
</pre>

CSS doesn't normally allow you do modify the DOM ( ignoring ::content pseudo-elements ), so in order to be able to style the DOM elements that are created by these CSS rules,
there are multiple passes of the CSS. 

# Builtin CSS

* [JavaToGraphviz-base.css](src/main/resources/JavaToGraphviz-base.css) - base CSS ; bare minimum to prevent graphviz errors
* [JavaToGraphviz-debug.css](src/main/resources/JavaToGraphviz-debug.css) - debugging CSS ; all node labels include lineNumber, nodeType and lastKeepNodeId
* [JavaToGraphviz.css](src/main/resources/JavaToGraphviz.css) - default CSS ; uses most of the classes and attributes listed above

# CLI

The java-to-graphviz-cli.jar can be used to generate diagrams from a command line. There's a few options you can supply:

<pre>C:\>java -jar java-to-graphviz-cli.jar
Usage:
  java -jar java-to-graphviz-cli.jar [options] filename filename ...

where [options] are:
 -h -?                       display this help text
 --verbose  -v               increase verbosity ( info level )
 --verbose2 -vv              increase verbosity a bit more ( debug level )
 --format   -f dot|dom1|dom2 output format: DOT diagram (default), pre-styled DOM, post-styled DOM
 --output   -o filename      send output to filename
                             For multiple output files, filename can contain
                               {basename} for base filename, {index} for diagram index
                             e.g. "{basename}-{index}.dot" for dot output,
                                  "{basename}.html" for dom output
                             default is stdout
 --source   -s version       set Java source language version ( 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7,
                               8, 9, 10, 11, 12, 13, 14, 15, 16 )

 --base-css -b url           replace base css with url ( relative to classpath, then file:///./ )
                               default = JavaToGraphviz.css
 --user-css -u url           add url to list of user css imports
 --css      -c {rules}       add css rules

 --option   -p key=value     set initial option; can be modified in source by 'gv-option' and 'gv-keepNode' directives

Options keys and defaults:
 edgerNamesCsv=control-flow  csv list of edger names
                             possible edger names: control-flow, ast
 enableKeepNodeFilter=false  if true, will perform node filtering
 defaultKeepNode=true        if true, filter will keep nodes by default
 keepNode=                   space-separated nodeTypes for fine-grained keepNode control
                             prefix nodeType with '-' to exclude, '+' or no prefix to include
                               e.g. "-expressionStatement -block -switchCase"
                               to exclude those nodeTypes when defaultKeepNode=true
                             NB: any node with a 'gv' comment will always be kept
</pre>

e.g. 

<pre>C:\>java -jar java-to-graphviz-cli.jar --base-css https://raw.githubusercontent.com/randomnoun/java-to-graphviz/master/src/main/resources/JavaToGraphviz.css src\test\java\com\example\input\ForStatement.java
digraph G {
  node [
    shape = rect;
    fontname = "Handlee";
  ]
  edge [
    fontname = "Handlee";
  ]
  bgcolor = transparent;
  fontname = "Handlee";
  compound = true;
  s_19 [
    class = "methodDeclaration";
    label = "MethodDeclaration";
    fillcolor = white;
    style = filled;
  ];
  s_19_3 [
    class = "block";
    label = "Block";
    fillcolor = white;
    style = filled;
  ];
...
</pre>

# Maven

To incorporate **java-to-graphviz** into your maven build, use the [java-to-graphviz-maven-plugin](https://github.com/randomnoun/java-to-graphviz-maven-plugin)

# Features

DONE
* style rules
* external style refs (classpath, TODO urls, files)
* label formats
* css-defined subgraphs
* comment-defined subgraphs
* a single .java source file can contain multiple graphs and subgraphs
* can put literal gv into the diagram
* few builtin methods/style properties/declarations to make this a bit easier to generate diagrams; e.g. wordwrap, flip logic on if edge labels, alternate representations, probably others
* comment attribution 
* bit of flexibility when it comes to how the diagram is generated ( node suppression , artificial nodes, that sort of thing )
* seeing I"m going to the trouble of creating a DOM to apply styles maybe dump that as well, &/or the AST tree. apply styles to AST nodes vs dag nodes ?
* lambdas, anonymous classes, and example images in the readme
* a maven plugin to generate these things during your build process. 

TODO

So a few things that'd be nice to implement if I get around to it:

* bunch up the exit nodes if there's lots of them
* a maven plugin which colours in the nodes using jacoco output.
* another eclipse plugin which hooks in via JVMTI to do that in realtime.
* another plugin which annotates the nodes with generated bytecodes.
* another plugin which adds in AOP nodes.
* create a stylesheet with no labels and shaded nodes. Maybe use whatever colouring scratch uses.
* and so on.

# More examples

Some example Java sourcecode, and the generated graphviz DOT and PNG output, from the unit tests:

The compact graphviz and PNG outputs are generated from the same source file, with the `defaultKeepNode` option set to `false`.

|  Java source | Graphviz | PNG | Compact Graphviz | Compact PNG |
|--|:--:|:--:|:--:|:--:|
| [CommentAttribution.java](src/test/java/com/example/input/CommentAttribution.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.CommentAttribution-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.CommentAttribution-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.CommentAttribution-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.CommentAttribution-0-compact.png) | 
| [Constructor.java](src/test/java/com/example/input/Constructor.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Constructor-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Constructor-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Constructor-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Constructor-0-compact.png) | 
| [ContinueBreakLabelA.java](src/test/java/com/example/input/ContinueBreakLabelA.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ContinueBreakLabelA-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ContinueBreakLabelA-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ContinueBreakLabelA-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ContinueBreakLabelA-0-compact.png) | 
| [ContinueBreakLabelB.java](src/test/java/com/example/input/ContinueBreakLabelB.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ContinueBreakLabelB-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ContinueBreakLabelB-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ContinueBreakLabelB-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ContinueBreakLabelB-0-compact.png) | 
| [ContinueBreakRank.java](src/test/java/com/example/input/ContinueBreakRank.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ContinueBreakRank-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ContinueBreakRank-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ContinueBreakRank-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ContinueBreakRank-0-compact.png) | 
| [DoStatement.java](src/test/java/com/example/input/DoStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.DoStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.DoStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.DoStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.DoStatement-0-compact.png) | 
| [Expressions.java](src/test/java/com/example/input/Expressions.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions-0-compact.png) | 
| [Expressions1.java](src/test/java/com/example/input/Expressions1.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions1-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions1-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions1-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions1-0-compact.png) | 
| [Expressions2.java](src/test/java/com/example/input/Expressions2.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions2-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions2-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions2-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions2-0-compact.png) | 
| [Expressions3a.java](src/test/java/com/example/input/Expressions3a.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions3a-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions3a-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions3a-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions3a-0-compact.png) | 
| [Expressions3b.java](src/test/java/com/example/input/Expressions3b.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions3b-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions3b-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions3b-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions3b-0-compact.png) | 
| [Expressions4.java](src/test/java/com/example/input/Expressions4.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions4-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions4-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions4-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions4-0-compact.png) | 
| [Expressions5.java](src/test/java/com/example/input/Expressions5.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions5-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions5-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions5-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions5-0-compact.png) | 
| [Expressions6.java](src/test/java/com/example/input/Expressions6.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions6-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions6-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.Expressions6-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.Expressions6-0-compact.png) | 
| [ForStatement.java](src/test/java/com/example/input/ForStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ForStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ForStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.ForStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.ForStatement-0-compact.png) | 
| [IfStatement.java](src/test/java/com/example/input/IfStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.IfStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.IfStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.IfStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.IfStatement-0-compact.png) | 
| [IfStatementExternalCss.java](src/test/java/com/example/input/IfStatementExternalCss.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.IfStatementExternalCss-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.IfStatementExternalCss-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.IfStatementExternalCss-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.IfStatementExternalCss-0-compact.png) | 
| [IfStatementFlip.java](src/test/java/com/example/input/IfStatementFlip.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.IfStatementFlip-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.IfStatementFlip-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.IfStatementFlip-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.IfStatementFlip-0-compact.png) | 
| [MethodChain.java](src/test/java/com/example/input/MethodChain.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.MethodChain-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.MethodChain-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.MethodChain-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.MethodChain-0-compact.png) | 
| [MultipleGraphs.java](src/test/java/com/example/input/MultipleGraphs.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.MultipleGraphs-0.dot)<br/>[graphviz](src/test/resources/expected-output/dot/com.example.input.MultipleGraphs-1.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.MultipleGraphs-0.png)<br/>[PNG](src/test/resources/expected-output/png/com.example.input.MultipleGraphs-1.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.MultipleGraphs-0-compact.dot)<br/>[graphviz](src/test/resources/expected-output/dot/com.example.input.MultipleGraphs-1-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.MultipleGraphs-0-compact.png)<br/>[PNG](src/test/resources/expected-output/png/com.example.input.MultipleGraphs-1-compact.png) | 
| [SwitchStatement.java](src/test/java/com/example/input/SwitchStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.SwitchStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.SwitchStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.SwitchStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.SwitchStatement-0-compact.png) | 
| [SwitchStatement2.java](src/test/java/com/example/input/SwitchStatement2.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.SwitchStatement2-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.SwitchStatement2-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.SwitchStatement2-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.SwitchStatement2-0-compact.png) | 
| [SynchronizeStatement.java](src/test/java/com/example/input/SynchronizeStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.SynchronizeStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.SynchronizeStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.SynchronizeStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.SynchronizeStatement-0-compact.png) | 
| [TryCatchFinallyStatement.java](src/test/java/com/example/input/TryCatchFinallyStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.TryCatchFinallyStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.TryCatchFinallyStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.TryCatchFinallyStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.TryCatchFinallyStatement-0-compact.png) | 
| [TryCatchStatement.java](src/test/java/com/example/input/TryCatchStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.TryCatchStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.TryCatchStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.TryCatchStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.TryCatchStatement-0-compact.png) | 
| [TryWithReourcesStatement.java](src/test/java/com/example/input/TryWithReourcesStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.TryWithReourcesStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.TryWithReourcesStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.TryWithReourcesStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.TryWithReourcesStatement-0-compact.png) | 
| [UserDefinedSubgraphs.java](src/test/java/com/example/input/UserDefinedSubgraphs.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.UserDefinedSubgraphs-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.UserDefinedSubgraphs-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.UserDefinedSubgraphs-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.UserDefinedSubgraphs-0-compact.png) | 
| [WhileStatement.java](src/test/java/com/example/input/WhileStatement.java) | [graphviz](src/test/resources/expected-output/dot/com.example.input.WhileStatement-0.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.WhileStatement-0.png) | [graphviz](src/test/resources/expected-output/dot/com.example.input.WhileStatement-0-compact.dot) | [PNG](src/test/resources/expected-output/png/com.example.input.WhileStatement-0-compact.png) | 

## More reading

[Here's the writeup on the blog](http://www.randomnoun.com/wp/2021/12/11/flowcharts-r-us/). 

## Licensing

java-to-graphviz is licensed under the BSD 2-clause license.

