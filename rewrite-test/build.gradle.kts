plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-core"))
    compileOnly("io.micrometer:micrometer-core:latest.release")
    api("org.junit.jupiter:junit-jupiter-api:latest.release")
    api("org.junit.jupiter:junit-jupiter-params:latest.release")

    implementation("org.assertj:assertj-core:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation("org.slf4j:slf4j-api:1.7.36")

    // NoClassDefFoundError jgit FileMode in RewriteTest diff() https://github.com/openrewrite/rewrite/issues/3243
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.+")
}
