allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

// El plugin flutter_jailbreak_detection 1.10.0 (pub.dev) es viejo y no declara ni "namespace"
// ni targetCompatibility de Java/Kotlin, lo que exige el Android Gradle Plugin moderno. En vez
// de parchear el paquete en el pub cache (se perdería con cada `flutter pub get`), se corrige
// aquí, solo para ese módulo, sin tocar la config propia de otros plugins (p. ej.
// firebase_analytics, que ya declara Java 17 correctamente).
// Se usa plugins.withId (no afterEvaluate) porque, con evaluationDependsOn(":app") arriba,
// algunos subproyectos ya quedan evaluados antes de que afterEvaluate llegue a registrarse.
// El compileOptions de Java se fija a través de la extensión "android {}" (la vía que AGP
// realmente respeta) en vez de tasks.withType<JavaCompile>, que AGP termina sobreescribiendo.
subprojects {
    plugins.withId("com.android.library") {
        val androidExt = extensions.getByType<com.android.build.gradle.LibraryExtension>()
        if (androidExt.namespace == null) {
            androidExt.namespace = project.group.toString()
        }
        if (project.name == "flutter_jailbreak_detection") {
            androidExt.compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
