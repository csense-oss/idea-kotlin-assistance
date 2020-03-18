import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.4.15"
    kotlin("jvm") version "1.3.70"
    java
    id("org.owasp.dependencycheck") version "5.2.4"
}

group = "csense-idea"
version = "0.9.10"
// See https://github.com/JetBrains/gradle-intellij-plugin/

intellij {
    updateSinceUntilBuild = false //Disables updating since-build attribute in plugin.xml
    setPlugins("Kotlin", "java")
    version = "2019.2"
}

repositories {
    jcenter()
    //until ds is in jcenter
    maven(url = "https://dl.bintray.com/csense-oss/maven")
    maven(url = "https://dl.bintray.com/csense-oss/idea")
}

dependencies {
    implementation("csense.kotlin:csense-kotlin-jvm:0.0.31")
    implementation("csense.kotlin:csense-kotlin-annotations-jvm:0.0.17")
    implementation("csense.kotlin:csense-kotlin-ds-jvm:0.0.24")
    implementation("csense.idea.base:csense-idea-base:0.1.6")
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
        <ul>
            <li>removed dead code, as it caused compatibility issues.</li>
            <li>Updated usage after overwrite to handle multiple vars.</li>
         </ul>
      """)
}

tasks.getByName("check").dependsOn("dependencyCheckAnalyze")

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}