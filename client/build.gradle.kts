import net.thebugmc.gradle.sonatypepublisher.PublishingType
import java.util.Properties

plugins {
    id("java-library")
    id("org.openapi.generator") version "7.17.0"
    id("org.kordamp.gradle.jandex") version "2.3.0"
    id("com.github.gmazzo.buildconfig") version "6.0.6"
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.4"
}

group = "io.apistax"
version = "1.6.0"

dependencies {
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.20.1"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.openapitools:jackson-databind-nullable:0.2.8")
    implementation("io.mikael:urlbuilder:2.0.9")

    testImplementation("org.wiremock:wiremock:3.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.1")
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
            srcDir("${layout.buildDirectory.get()}/generated/sources/openapi/src/main/java")
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
    outputDir = "${layout.buildDirectory.get()}/generated/sources/openapi"
    packageName = "io.apistax"
    modelPackage = "io.apistax.models"
    apiPackage = "io.apistax.apis"
    library = "native"

    globalProperties = mapOf(
        "apis" to "false",
        "models" to "",
        "modelDocs" to "false",
        "modelTests" to "false",
        "supportingFiles" to ""
    )

    configOptions = mapOf(
        "dateLibrary" to "java8",
        "useRuntimeException" to "true",
        "useJakartaEe" to "true"
    )
}

val properties = Properties().apply {
    if (System.getenv().containsKey("PORTAL_USERNAME")) {
        put("portalUsername", System.getenv("PORTAL_USERNAME"))
        put("portalPassword", System.getenv("PORTAL_PASSWORD"))
    } else {
        load(project.rootProject.file("local.properties").inputStream())
    }
}

centralPortal {
    username = properties.getProperty("portalUsername")
    password = properties.getProperty("portalPassword")

    publishingType = PublishingType.AUTOMATIC

    name = "apistax-client"

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

        issueManagement {
            system = "GitHub"
            url = "https://github.com/apistax/client-java/issues"
        }
    }
}

tasks["compileJava"].setDependsOn(listOf(tasks["openApiGenerate"]))
tasks["sourcesJar"].setDependsOn(listOf(tasks["openApiGenerate"]))
tasks["compileTestJava"].setDependsOn(listOf(tasks["jandex"]))
tasks["javadoc"].setDependsOn(listOf(tasks["jandex"]))