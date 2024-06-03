plugins {
    kotlin("jvm") version "1.9.23"
    id("nu.studer.jooq") version "9.0"
}

group = "org.wrongwrong"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jooq:jooq:3.19.9")
    implementation("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
    implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")

    // for codegen
    compileOnly("org.postgresql:postgresql:42.6.2")
    jooqGenerator("org.postgresql:postgresql:42.6.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

jooq {
    version.set("3.19.9")
    configurations {
        create("main") {
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/jooq-16669-db"
                    user = "jooq-16669-root"
                    password = "jooq-16669-root"
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        includes = ".*"
                        excludes = "flyway_schema_history"
                    }
                }
            }
        }
    }
}
