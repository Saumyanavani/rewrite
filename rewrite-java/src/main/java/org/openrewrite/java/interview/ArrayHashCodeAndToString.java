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
package org.openrewrite.java.interview;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;


import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

public class ArrayHashCodeAndToString extends Recipe {
    @Override
    public String getDisplayName() {
        return "Correct `hashCode` and `toString` for arrays";
    }

    @Override
    public String getDescription() {
        return "The `hashCode` and `toString` methods of arrays are not implemented correctly. " +
                "Use `Arrays.hashCode` and `Arrays.toString` instead.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2116");
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(15);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ArrayHashCodeAndToStringVisitor<>();
    }

    private static class ArrayHashCodeAndToStringVisitor<P> extends JavaIsoVisitor<P> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
            if (method.getSelect() != null){
                if (method.getSelect().getType() instanceof JavaType.Array){
                    if (hashCodeMethodMatcher.matches(method)){
                        maybeAddImport("java.util.Arrays");
                        JavaTemplate hashCodeTemplate = JavaTemplate.builder(this::getCursor, "Arrays.hashCode(#{anyArray()})").imports("java.util.Arrays").build();
                        return method.withTemplate(hashCodeTemplate, method.getCoordinates().replace(), method.getSelect());
                    } else if (toStringMethodMatcher.matches(method)) {
                        maybeAddImport("java.util.Arrays");
                        JavaTemplate toStringTemplate = JavaTemplate.builder(this::getCursor, "Arrays.toString(#{anyArray()})").imports("java.util.Arrays").build();
                        return method.withTemplate(toStringTemplate, method.getCoordinates().replace(), method.getSelect());
                    }
                }
            }

            return super.visitMethodInvocation(method, p);
        }

        private static final MethodMatcher hashCodeMethodMatcher = new MethodMatcher("Object hashCode()");
        private static final MethodMatcher toStringMethodMatcher = new MethodMatcher("Object toString()");
    }
}
