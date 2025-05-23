pluginManagement {
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.aliyun.com/repository/google") }
        maven { url = uri("https://mirrors.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.aliyun.com/repository/google") }
        maven { url = uri("https://mirrors.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}

rootProject.name = "LPlayer"
include(":app")
