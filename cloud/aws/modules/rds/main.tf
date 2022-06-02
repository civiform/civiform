resource "aws_db_parameter_group" "civiform" {
  name   = "civiform"
  family = "postgres12"

  parameter {
    name  = "log_connections"
    value = "1"
  }
}

resource "aws_db_instance" "civiform" {
  identifier             = "civiform"
  instance_class         = "db.t3.micro"
  allocated_storage      = 5
  engine                 = "postgres"
  engine_version         = "12"
  username               = "db_user_name"
  password               = "CHANGE_ME"
  db_subnet_group_name   = aws_db_subnet_group.civiform.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.civiform.name
  publicly_accessible    = false
  skip_final_snapshot    = true
}