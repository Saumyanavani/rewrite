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
package org.openrewrite.gradle;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.groovy.Assertions.srcMainGroovy;
import static org.openrewrite.java.Assertions.*;

class AddDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
          .classpath("junit-jupiter-api", "guava", "jackson-databind", "jackson-core", "lombok"));
    }

    @Language("java")
    private final String usingGuavaIntMath = """
            import com.google.common.math.IntMath;
            public class A {
                boolean getMap() {
                    return IntMath.isPrime(5);
                }
         
            }
      """;

    @ParameterizedTest
    @ValueSource(strings = {"com.google.common.math.*", "com.google.common.math.IntMath"})
    void onlyIfUsingTestScope(String onlyIfUsing) {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing)),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              "",
              """
                dependencies {
                    testImplementation "com.google.guava:guava:29.0-jre"
                }
                """
            )
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.google.common.math.*", "com.google.common.math.IntMath"})
    void onlyIfUsingCompileScope(String onlyIfUsing) {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing)),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "com.google.guava:guava:29.0-jre"
                }
                """
            ),
            settingsGradle(
              """
                rootProject.name = "project"
                """
            )
          )
        );
    }

    @Test
    void addDependencyWithClassifier() {
        AddDependency addDep = new AddDependency("io.netty", "netty-tcnative-boringssl-static", "2.0.54.Final", null, "implementation", "com.google.common.math.IntMath", "linux-x86_64", null, null);
        rewriteRun(
          spec -> spec.recipe(addDep),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              "",
              """
                dependencies {
                    implementation "io.netty:netty-tcnative-boringssl-static:2.0.54.Final:linux-x86_64"
                }
                """
            )
          )
        );
    }

    @Test
    void addDependencyWithoutVersion() {
        AddDependency addDep = new AddDependency("io.netty", "netty-tcnative-boringssl-static", null, null, "implementation", "com.google.common.math.IntMath", null, null, null);
        rewriteRun(
          spec -> spec.recipe(addDep),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              "",
              """
                dependencies {
                    implementation "io.netty:netty-tcnative-boringssl-static"
                }
                """
            )
          )
        );
    }

    @Test
    void addDependencyWithoutVersionWithClassifier() {
        // Without a version, classifier must not be present in the result
        AddDependency addDep = new AddDependency("io.netty", "netty-tcnative-boringssl-static", null, null, "implementation", "com.google.common.math.IntMath", "linux-x86_64", null, null);
        rewriteRun(
          spec -> spec.recipe(addDep),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              "",
              """
                dependencies {
                    implementation "io.netty:netty-tcnative-boringssl-static"
                }
                """
            )
          )
        );
    }


    @Test
    void notUsingType() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.collect.ImmutableMap")),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              ""
            )
          )
        );
    }

    @Test
    void addInOrder() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "commons-lang:commons-lang:1.0"
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "com.google.guava:guava:29.0-jre"
                    implementation "commons-lang:commons-lang:1.0"
                }
                """
            )
          )
        );
    }

    @Test
    void addTestDependenciesAfterCompile() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "commons-lang:commons-lang:1.0"
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "commons-lang:commons-lang:1.0"
                    
                    testImplementation "com.google.guava:guava:29.0-jre"
                }
                """
            )
          )
        );
    }

    @Test
    void addDependenciesKeepFormatting() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "com.example:example:1.0"
                    testImplementation "junit:junit:4.12"
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "com.example:example:1.0"
                    implementation "com.google.guava:guava:29.0-jre"
                    testImplementation "junit:junit:4.12"
                }
                """
            )
          )
        );
    }

    @Test
    void addDependencyToNewGrouping() {
        rewriteRun(
          spec -> spec.recipe(addDependency("org.projectlombok:lombok:1.0", "lombok.Value", "annotationProcessor")),
          mavenProject("project",
            srcMainJava(
              java("""
                import lombok.Value;
                                
                @Value
                class A {
                    String b;
                }
                """
              )
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "commons-lang:commons-lang:1.0"

                    testImplementation "junit:junit:4.13"
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    annotationProcessor "org.projectlombok:lombok:1.0"
                    
                    implementation "commons-lang:commons-lang:1.0"
                    
                    testImplementation "junit:junit:4.13"
                }
                """
            )
          )
        );
    }

    @Test
    void addDependenciesToExistingGrouping() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    testImplementation group: "com.google.guava", name: "guava", version: "29.0-jre"
                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """
            )
          )
        );
    }

    @Test
    void addDependenciesWithoutVersionToExistingGrouping() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    testImplementation group: "com.google.guava", name: "guava"
                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """
            )
          )
        );
    }

    @Test
    void addDependenciesWithClassifierToExistingGrouping() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre:test", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    testImplementation group: "com.google.guava", name: "guava", version: "29.0-jre", classifier: "test"
                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """
            )
          )
        );
    }

    @Test
    void addDependenciesWithoutVersionWithClassifierToExistingGrouping() {
        AddDependency addDep = new AddDependency("io.netty", "netty-tcnative-boringssl-static", null, null, "testImplementation", "com.google.common.math.IntMath", "linux-x86_64", null, null);
        rewriteRun(
          spec -> spec.recipe(addDep),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                                
                repositories {
                    mavenCentral()
                }
                                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    testImplementation group: "io.netty", name: "netty-tcnative-boringssl-static", classifier: "linux-x86_64"
                    def junitVersion = "4.12"
                    testImplementation group: "junit", name: "junit", version: junitVersion
                }
                """
            )
          )
        );
    }

    @Test
    void matchesDependencyDeclarationStyle() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"
                }
                """,
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation group: "commons-lang", name: "commons-lang", version: "1.0"

                    testImplementation group: "com.google.guava", name: "guava", version: "29.0-jre"
                }
                """
            )
          )
        );
    }

    @Test
    void addDependencyDoesntAddWhenExistingDependency() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "com.google.guava:guava:28.0-jre"
                }
                """
            )
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"api", "compileOnly", "testRuntimeOnly"})
    void addDependencyToConfiguration(String configuration) {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.fasterxml.jackson.core:jackson-core:2.12.0", "com.fasterxml.jackson.core.*", configuration)),
          mavenProject("project",
            srcMainJava(
              java(
                """
                  public class A {
                      com.fasterxml.jackson.core.Versioned v;
                  }
                  """
              )
            ),
            buildGradle(
              "",
              """
                dependencies {
                    %s "com.fasterxml.jackson.core:jackson-core:2.12.0"
                }
                """.formatted(configuration)
            )
          )
        );
    }

    @Test
    void addDependencyToProjectsThatNeedIt() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath", "implementation")),
          mavenProject("root",
            buildGradle(
              ""
            ),
            settingsGradle(
              """
                include "project1"
                include "project2"
                """
            )
          ),
          mavenProject("project1",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              "",
              """
                dependencies {
                    implementation "com.google.guava:guava:29.0-jre"
                }
                """
            )
          ),
          mavenProject("project2",
            buildGradle(
              ""
            )
          )
        );
    }

    @Test
    void addDependency() {
        rewriteRun(
          spec -> spec.recipe(addDependency("org.openrewrite:rewrite-core:1.0.0", "java.util.Date", "implementation")),
          mavenProject("project",
            srcMainGroovy(
              groovy(
                """
                  import java.util.*;

                  class MyClass {
                      static void main(String[] args) {
                          Date date = new Date();
                          System.out.println("Hello world");
                      }
                  }
                  """
              )
            ),
            buildGradle(
              "",
              """
                dependencies {
                    implementation "org.openrewrite:rewrite-core:1.0.0"
                }
                """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3209")
    @Test
    void addDependencyWithVariable() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:${guavaVersion}", "com.google.common.math.IntMath", "implementation")),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            buildGradle(
              """
                def gauvaVersion = "29.0-jre"
                """,
              """
                def gauvaVersion = "29.0-jre"

                dependencies {
                    implementation "com.google.guava:guava:${guavaVersion}"
                }
                """
            )
          )
        );
    }

    private AddDependency addDependency(String gav, String onlyIfUsing) {
        return addDependency(gav, onlyIfUsing, null);
    }

    private AddDependency addDependency(String gav, String onlyIfUsing, @Nullable String configuration) {
        String[] gavParts = gav.split(":");
        return new AddDependency(
          gavParts[0], gavParts[1], (gavParts.length < 3) ? null : gavParts[2], null, configuration, onlyIfUsing,
          (gavParts.length < 4) ? null : gavParts[3], null, null
        );
    }
}
