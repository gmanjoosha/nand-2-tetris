plugins {
    application
}

group = "org.qualcomm"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

application {
    mainClassName = "org.qualcomm.manjoosha.JackCompiler"
    applicationDefaultJvmArgs = listOf("--enable-preview")
}

java {
    sourceCompatibility = JavaVersion.VERSION_14
    targetCompatibility = JavaVersion.VERSION_14
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}


