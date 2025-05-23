// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "7.4.2" apply false
}

buildscript {
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.aliyun.com/repository/google") }
        maven { url = uri("https://mirrors.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
    }
}