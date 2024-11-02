repositories {
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.viaversion.com")
}

dependencies {
    api(project(":api"))
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.0")
    implementation("redis.clients:jedis:5.2.0")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        exclude("org/slf4j/**")
        exclude("com/google/gson/**")
        relocate("org.mariadb.jdbc", "net.azisaba.interchat.libs.org.mariadb.jdbc")
        relocate("com.zaxxer.hikari", "net.azisaba.interchat.libs.com.zaxxer.hikari")
        relocate("redis.clients.jedis", "net.azisaba.interchat.libs.redis.clients.jedis")
        relocate("org.json", "net.azisaba.interchat.libs.org.json")
        relocate("org.apache.commons", "net.azisaba.interchat.libs.org.apache.commons")
    }
}
