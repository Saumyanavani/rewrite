package org.openrewrite.java.interview;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ArrayHashCodeAndToStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ArrayHashCodeAndToString());
    }

    @Test
    void replace_Array_hashCode_with_Arrays_hashCode() {
        rewriteRun(
          java(
            """
              class A {
                  public static void main(String[] args) {
                      int argHash = args.hashCode();
                  }
              }
              """,
            """
              import java.util.Arrays;
              
              class A {
                  public static void main(String[] args) {
                      int argHash = Arrays.hashCode(args);
                  }
              }
              """
          )
        );
    }

    @Test
    void check_Array_Object_Type() {
        rewriteRun(
          java(
            """
              class A {
                  public static void main(Object args) {
                      int argHash = args.hashCode();
                  }
              }
              """
          )
        );
    }

    @Test
    void simple_Test() {
        rewriteRun(
          java(
            """
              class A {
                  public static void main(Object args) {
//                      int argHash = args.hashCode();
                      foo(args);
                  }
                  
                  public static void foo(Object args){
                    System.out.println(args);
                  }
              }
              """
          )
        );
    }

    @Test
    void replace_Array_toString_with_Arrays_toString() {
        rewriteRun(
          java(
            """
              class A {
                  public static void main(String[] args) {
                      String argStr = args.toString();
                  }
              }
              """,
            """
              import java.util.Arrays;
              
              class A {
                  public static void main(String[] args) {
                      String argStr = Arrays.toString(args);
                  }
              }
              """
          )
        );
    }

    @Test
    void replace_Array_toString_with_Arrays_toString_toString() {
        rewriteRun(
          java(
            """
              class A {
                  public static void main(String[] args) {
                      String argStr = args.toString().toString();
                  }
              }
              """,
            """
              import java.util.Arrays;
              
              class A {
                  public static void main(String[] args) {
                      String argStr = Arrays.toString(args).toString();
                  }
              }
              """
          )
        );
    }
}
