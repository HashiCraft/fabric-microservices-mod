resource "build" "minecraft_dev" {
  container {
    dockerfile = "Dockerfile"
    context    = "./minecraft/Docker"
  }
}

resource "container" "minecraft" {
  image {
    name = resource.build.minecraft_dev.image
  }

  network {
    id = resource.network.local.meta.id
  }

  # Minecraft port
  port {
    host  = 25566
    local = 25565
  }

  # Microservice port
  port {
    host  = 8082
    local = 8082
  }

  environment = {
    GAME_MODE                 = "creative"
    WHITELIST_ENABLED         = "false"
    ONLINE_MODE               = "false"
    RCON_ENABLED              = "true"
    RCON_PASSWORD             = "password"
    SPAWN_ANIMALS             = "true"
    SPAWN_NPCS                = "true"
    MICROSERVICES_db_host     = "postgres.container.local.jmpd.in:5432"
    MICROSERVICES_db_username = variable.postgres_user
    MICROSERVICES_db_password = variable.postgres_password
    MICROSERVICES_db_database = variable.postgres_database
    SRE_BOT_START             = "-870,93,31"
    SRE_BOT_END               = "-866,90,36"
  }

  # Mount the secrets that contain the db connection info
  volume {
    source      = data("db_secrets")
    destination = "/etc/db_secrets"
  }

  # Mount the local world and config files 
  volume {
    source      = "./minecraft/world"
    destination = "/minecraft/world"
  }

  volume {
    source      = "./minecraft/mods"
    destination = "/minecraft/mods"
  }

  volume {
    source      = "./minecraft/config/ops.json"
    destination = "/minecraft/config/ops.json"
  }
}