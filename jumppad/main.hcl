variable "server_disabled" {
  description = "Start the minecraft server"
  default     = true
}

resource "network" "local" {
  subnet = "10.10.0.0/16"
}

resource "certificate_ca" "minecraft_ca" {
  output = data("certs")
}

resource "certificate_leaf" "minecraft_leaf" {
  ca_key  = resource.certificate_ca.minecraft_ca.private_key.path
  ca_cert = resource.certificate_ca.minecraft_ca.certificate.path

  ip_addresses = ["127.0.0.1"]

  dns_names = [
    "localhost",
    "minecraft.container.jumppad.dev",
  ]

  output = data("certs")
}

resource "build" "minecraft" {
  disabled = variable.server_disabled

  container {
    dockerfile = "Dockerfile"
    context    = "./server"
  }
}

resource "copy" "fabric_mod" {
  disabled = variable.server_disabled

  source      = "./minecraft/mods"
  destination = data("mods")
}

resource "copy" "microservice_mod" {
  disabled = variable.server_disabled

  source      = "../build/libs/fabric-microservices-mod-1.2.0.jar"
  destination = "${data("mods")}/fabric-microservices-mod-1.2.0.jar"
}

resource "container" "minecraft" {
  disabled = variable.server_disabled

  image {
    name = resource.build.minecraft.image
  }

  network {
    id = resource.network.local.meta.id
  }

  # Minecraft
  port {
    remote = 25565
    host   = 25565
    local  = 25565
  }

  # Microservice 
  port {
    remote = 8081
    host   = 8081
    local  = 8081
  }

  environment = {
    GAME_MODE                 = "creative"
    WHITELIST_ENABLED         = "false"
    ONLINE_MODE               = "false"
    RCON_ENABLED              = "true"
    RCON_PASSWORD             = "password"
    SPAWN_ANIMALS             = "true"
    SPAWN_NPCS                = "true"
    VAULT_ADDR                = "http://vault.container.local.jmpd.in:8200"
    VAULT_TOKEN               = "root"
    HASHICRAFT_env            = "local"
    MICROSERVICES_db_host     = "postgres.container.local.jmpd.in:5432"
    MICROSERVICES_db_password = resource.container.postgres.environment.POSTGRES_PASSWORD
    MICROSERVICES_db_database = resource.container.postgres.environment.POSTGRES_DB
    SRE_BOT_START             = "86,67,-64"
    SRE_BOT_END               = "86,67,-69"
  }

  # Mount the secrets that contain the certs
  volume {
    source      = data("certs")
    destination = "/etc/certs"
  }

  # Mount the secrets that contain the db connection info
  volume {
    source      = "./config/db_secrets"
    destination = "/etc/db_secrets"
  }

  # Mount the local world and config files 
  volume {
    source      = "./minecraft/world"
    destination = "/minecraft/world"
  }

  volume {
    source      = data("mods")
    destination = "/minecraft/mods"
  }

  volume {
    source      = "./config/webservers.json"
    destination = "/minecraft/config/webservers.json"
  }
}

resource "container" "postgres" {
  network {
    id = resource.network.local.meta.id
  }

  image {
    name = "postgres:15.4"
  }

  port {
    local  = 5432
    remote = 5432
    host   = 5432
  }

  environment = {
    POSTGRES_PASSWORD = "password"
    POSTGRES_DB       = "mydb"
  }

  volume {
    source      = "./sql/setup.sql"
    destination = "/docker-entrypoint-initdb.d/setup.sql"
  }
}