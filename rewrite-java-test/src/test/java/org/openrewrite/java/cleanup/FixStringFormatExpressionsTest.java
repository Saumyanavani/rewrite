/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
public class FixStringFormatExpressionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FixStringFormatExpressions());
    }

    @DocumentExample
    @Test
    void newLineFormat() {
        rewriteRun(
          java(
            //language=java
            """
              class T {
                  static {
                      String s = String.format("hello world\\n%s", "again");
                      String s2 = "hello world\\n%s".formatted("again");
                  }
              }
              """, """
              class T {
                  static {
                      String s = String.format("hello world%n%s", "again");
                      String s2 = "hello world%n%s".formatted("again");
                  }
              }
              """
          )
        );
    }

    @Test
    void trimUnusedArguments() {
        rewriteRun(
          //language=java
          java(
            """
              class T {
                  static {
                      String s = String.format("count: %d, %d, %d, %d", 1, 3, 2, 4, 5);
                      String f = "count: %d, %d, %d, %d".formatted(1, 3, 2, 4, 5);
                  }
              }
              """,
            """
              class T {
                  static {
                      String s = String.format("count: %d, %d, %d, %d", 1, 3, 2, 4);
                      String f = "count: %d, %d, %d, %d".formatted(1, 3, 2, 4);
                  }
              }
              """
          )
        );
    }

    @Test
    void allArgsAreUsed() {
        rewriteRun(
          //language=java
          java(
            """
              class T {
                  static {
                      String s = String.format("count: %d, %d, %d, %d", 1, 3, 2, 4);
                      String f = "count: %d, %d, %d, %d".formatted(1, 3, 2, 4);
                  }
              }
              """
          )
        );
    }
}
