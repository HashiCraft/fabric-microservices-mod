resource "container" "postgres" {
  network {
    id = resource.network.local.meta.id
  }

  image {
    name = "postgres:15.4"
  }

  port {
    local = 5432
    host  = 5432
  }

  environment = {
    POSTGRES_PASSWORD = variable.postgres_password
    POSTGRES_DB       = variable.postgres_database
  }

  volume {
    source      = "./sql/setup.sql"
    destination = "/docker-entrypoint-initdb.d/setup.sql"
  }
}

resource "template" "db_username" {
  source      = variable.postgres_user
  destination = "${data("db_secrets")}/username"
}

resource "template" "db_password" {
  source      = variable.postgres_user
  destination = "${data("db_secrets")}/password"
}