variable "vault_token" {
  default = "root"
}

variable "vault_addr" {
  default = "0.0.0.0:8200"
}

variable "postgres_user" {
  default = "postgres"
}

variable "postgres_password" {
  default = "password"
}

variable "postgres_database" {
  default = "minecraft"
}

resource "network" "local" {
  subnet = "10.5.0.0/16"
}

output "POSTGRES_ADDR" {
  value = "${resource.container.postgres.container_name}:${resource.container.postgres.port.0.host}"
}

output "POSTGRES_USER" {
  value = variable.postgres_user
}

output "POSTGRES_PASS" {
  value = variable.postgres_password
}

output "POSTGRES_DATABASE" {
  value = variable.postgres_database
}