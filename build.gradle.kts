
plugins {
}

buildscript {
    repositories {
        google()
        // build error at start, resolved by replace jcenter() with following 2 lines:
        gradlePluginPortal()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(ClassPaths.gradlePlugin)
        classpath(ClassPaths.kotlinPlugin)
        classpath(ClassPaths.spotlessPlugin)
        classpath(ClassPaths.daggerPlugin)
        // classpath("com.google.dagger:hilt-android-gradle-plugin:4")
    }
}


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}