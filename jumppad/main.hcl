resource "network" "local" {
  subnet = "10.10.0.0/16"
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