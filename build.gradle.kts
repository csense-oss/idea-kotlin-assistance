plugins {
    id("org.jetbrains.intellij") version "0.4.10"
    kotlin("jvm") version "1.3.41"
    java
    id("org.owasp.dependencycheck") version "5.1.0"
}

group = "csense-idea"
version = "0.3"
// See https://github.com/JetBrains/gradle-intellij-plugin/

intellij {
    updateSinceUntilBuild = false //Disables updating since-build attribute in plugin.xml
    setPlugins("kotlin")
    version = "2018.2"
}

repositories {
    jcenter()
}

dependencies {
    compile("csense.kotlin:csense-kotlin-jvm:0.0.20")
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
        <ul>
            <li> avoid repeating names in inspections.</li>
            <li> exclude synthetic properties for inheritance initialization order (only getter (optionally setter)) since they are only functions</li>
            <li> reference names in initializationInheritance order.</li>
            <li> ignore inspections added (WIP)</li>
            <li> handles getter only properties better & handles property setter better</li>
            <li> improved rearrange so that it only inspects the initializer, thus are able to work in more cases.</li>
         </ul>
      """)
}

tasks.getByName("check").dependsOn("dependencyCheckAnalyze")
