val exposed_version: String by project
val h2_version: String by project
val java_jwt_version: String by project
val jbcrypt_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val swagger_ui_version: String by project

plugins {
    kotlin("jvm") version "2.3.10"
    id("io.ktor.plugin") version "3.4.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
    id("com.github.bjornvester.wsdl2java") version "2.0.2"
    jacoco
}

group = "at.ac.hcw.se"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "org/tempuri/**",
                    "com/microsoft/**",
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "org/tempuri/**",
                    "com/microsoft/**",
                )
            }
        })
    )
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

wsdl2java {
    wsdlDir.set(layout.projectDirectory.dir("src/main/resources/wsdl"))
}

dependencies {
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.github.smiley4:ktor-swagger-ui:$swagger_ui_version")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("com.auth0:java-jwt:$java_jwt_version")
    implementation("org.mindrot:jbcrypt:$jbcrypt_version")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.2")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api:4.0.3")
    runtimeOnly("com.sun.xml.ws:jaxws-rt:4.0.4")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-client-core")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    // Source: https://mvnrepository.com/artifact/com.azure/azure-storage-blob
    implementation("com.azure:azure-storage-blob:12.33.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
}
