plugins {
    kotlin("jvm") version "1.9.24" apply false
}

allprojects {
    group = property("GROUP").toString()
    version = property("VERSION_NAME").toString()

    repositories {
        mavenCentral()
    }
}
