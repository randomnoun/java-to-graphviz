package com.example.input;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** method reference and inner class test cases.
 * Same as 3a but with some style rules to draw the inner classes
 * (not enabled by default as it double and triple-spaces everything)
 * 
 */
public class Expressions3b {

    /* gv-style:{
     
    // just the inner classes
    node.typeDeclaration node.typeDeclaration { gv-newSubgraph: true; } 
    node.typeDeclaration node.typeDeclaration.class > subgraph {
      gv-idFormat : "cluster_c_${lineNumber}"; // subgraph ids must being with the text "cluster_" for graphviz to draw it as a subgraph 
      gv-labelFormat : "class ${className}";
      pencolor : gray;
    }
    node.typeDeclaration node.typeDeclaration.interface > subgraph {
      gv-idFormat : "cluster_i_${lineNumber}"; // subgraph ids must being with the text "cluster_" for graphviz to draw it as a subgraph
      gv-labelFormat : "interface ${interfaceName}";
      pencolor : gray;
    }
    }
     */
    
    public class MyNumber {
        long i;
        public MyNumber(Long i) {
            this.i = i;
        }
        public Long getNumber() { return i; }
    }

    public class MyOtherNumber extends MyNumber {
        public MyOtherNumber(Long i) {
            super(i);
        }
        public Long getNumber() { return i * 2; }
        public Long getOrigNumber() {
            Supplier<Long> thisGetter = this::getNumber;
            Supplier<Long> superGetter = super::getNumber;
            return ((Math.random() > 0.5) ? thisGetter : superGetter).get();
        }
        public void qualifiedThis() {
            Expressions3b.this.testExpressions();
        }
        public Long callSuperMethod() {
            return super.getNumber();
        }
        public Long getSuperField() {
            return super.i;
        }

    }

    public void testExpressions() {

       // references
       List<Long> numbers = Arrays.asList(3L, 2L, 1L);
       numbers.stream().sorted(Long::compareTo);
       
       MyNumber[] myNumbers = numbers.stream()
         .map(MyOtherNumber::new)
         .toArray(MyOtherNumber[]::new);
       
       Supplier<Long> aGetter = myNumbers[0]::getNumber;
       // aGetter.get();
       
       MyNumber arrayAccess = myNumbers[2];
       
    }
    
    

    
}
