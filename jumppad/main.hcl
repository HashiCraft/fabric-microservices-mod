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

resource "container" "minecraft" {
  image {
    name = "hashicraft/minecraftservice:v0.0.3"
  }

  network {
    id = resource.network.local.id
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
    VAULT_ADDR                = "http://vault.container.jumppad.dev:8200"
    VAULT_TOKEN               = "root"
    HASHICRAFT_env            = "local"
    MICROSERVICES_db_host     = "postgres.container.jumppad.dev:5432"
    MICROSERVICES_db_password = "password"
    MICROSERVICES_db_database = "mydb"
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
    source      = "./config/world"
    destination = "/minecraft/world"
  }
  
  volume {
    source      = "./config/mods"
    destination = "/minecraft/mods"
  }

  volume {
    source      = "./config/databases.json"
    destination = "/minecraft/config/databases.json"
  }

  volume {
    source      = "./config/webservers.json"
    destination = "/minecraft/config/webservers.json"
  }
}

resource "container" "postgres" {
  image {
    name = "postgres:15.4"
  }

  port {
    local           = 5432
    remote          = 5432
    host            = 5432
    open_in_browser = ""
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