import java.net.URI
import java.util.Properties

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("org.openapi.generator") version "7.5.0"
    id("org.kordamp.gradle.jandex") version "2.0.0"
    id("com.github.gmazzo.buildconfig") version "5.6.2"
}

version = "1.5.1"

dependencies {
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("io.mikael:urlbuilder:2.0.9")

    testImplementation("org.wiremock:wiremock:3.5.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

buildConfig {
    packageName = "io.apistax.client"
    useJavaOutput()

    buildConfigField<String>("VERSION", version.toString())
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }

    withJavadocJar()
    withSourcesJar()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<Javadoc>("javadoc") {
    options {
        encoding = "UTF-8"
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated/sources/openapi/src/main/java")
        }
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to "APIstax Java Client",
            "Implementation-Vendor" to "instant:solutions OG",
            "Implementation-URL" to "https://apistax.io",
            "Bundle-License" to "https://raw.githubusercontent.com/APIstax/client-java/main/LICENSE",
            "Scm-Connection" to "scm:git:github.com/apistax/client-java.git",
            "Scm-Url" to "https://github.com/apistax/client-java",
            "Build-Jdk-Spec" to java.toolchain.languageVersion.get().asInt()
        )
    }
}

openApiGenerate {
    generatorName = "java"
    inputSpec = "$projectDir/api.yml"
    outputDir = "$buildDir/generated/sources/openapi"
    packageName = "io.apistax"
    modelPackage = "io.apistax.models"
    apiPackage = "io.apistax.apis"
    library = "native"

    globalProperties = mapOf(
        "apis" to "false",
        "models" to "",
        "modelDocs" to "false",
        "modelTests" to "false",
        "supportingFiles" to "false"
    )

    configOptions = mapOf(
        "dateLibrary" to "java8",
        "useRuntimeException" to "true",
        "useJakartaEe" to "true"
    )
}

val properties = Properties().apply {
    if (System.getenv().containsKey("OSSRH_USERNAME")) {
        put("ossrhUsername", System.getenv("OSSRH_USERNAME"))
        put("ossrhPassword", System.getenv("OSSRH_PASSWORD"))
    } else {
        load(project.rootProject.file("local.properties").inputStream())
    }
}

publishing {
    publications {
        create<MavenPublication>("apistax") {
            groupId = "io.apistax"
            artifactId = "apistax-client"
            from(components["java"])

            pom {
                name = "apistax-client"
                description = "Secure and reliable APIs for your common business needs."
                url = "https://apistax.io/"
                inceptionYear = "2022"

                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://raw.githubusercontent.com/APIstax/client-java/main/LICENSE"
                    }
                }

                organization {
                    name = "instant:solutions OG"
                    url = "https://instant-it.at/"
                }

                developers {
                    developer {
                        id = "holzleitner"
                        name = "Max Holzleitner"
                        email = "max.holzleitner@instant-it.at"
                        organization = "instant:solutions OG"
                        organizationUrl = "https://instant-it.at/"
                        timezone = "Europe/Vienna"
                    }

                    developer {
                        id = "andlinger"
                        name = "David Andlinger"
                        email = "david.andlinger@instant-it.at"
                        organization = "instant:solutions OG"
                        organizationUrl = "https://instant-it.at/"
                        timezone = "Europe/Vienna"
                    }
                }

                scm {
                    connection = "scm:git:github.com/apistax/client-java.git"
                    developerConnection = "scm:git:github.com/apistax/client-java.git"
                    url = "https://github.com/apistax/client-java"
                }
            }
        }
    }

    repositories {
        maven {
            name = "ossrh"
            url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = properties.getProperty("ossrhUsername")
                password = properties.getProperty("ossrhPassword")
            }
        }

        maven {
            name = "ossrhSnapshot"
            url = URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = properties.getProperty("ossrhUsername")
                password = properties.getProperty("ossrhPassword")
            }
        }
    }
}

signing {
    sign(publishing.publications["apistax"])
}

tasks["compileJava"].setDependsOn(listOf(tasks["openApiGenerate"]))
tasks["sourcesJar"].setDependsOn(listOf(tasks["openApiGenerate"]))
tasks["compileTestJava"].setDependsOn(listOf(tasks["jandex"]))
tasks["javadoc"].setDependsOn(listOf(tasks["jandex"]))