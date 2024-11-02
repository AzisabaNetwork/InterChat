repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    api(project(":api"))
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.0")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
}

tasks {
    shadowJar {
        exclude("org/slf4j/**")
        exclude("com/google/gson/**")
        relocate("org.mariadb.jdbc", "net.azisaba.interchat.libs.org.mariadb.jdbc")
        relocate("com.zaxxer.hikari", "net.azisaba.interchat.libs.com.zaxxer.hikari")
        relocate("redis.clients.jedis", "net.azisaba.interchat.libs.redis.clients.jedis")
        relocate("org.yaml", "net.azisaba.interchat.libs.org.yaml")
        relocate("org.json", "net.azisaba.interchat.libs.org.json")
        relocate("org.apache.commons", "net.azisaba.interchat.libs.org.apache.commons")
    }
}
