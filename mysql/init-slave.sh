#!/bin/bash
set -e

until mysql -h mysql-master -u root -pminatoaqua3710 -e "SELECT 1" &>/dev/null; do
  echo "Waiting for master..."
  sleep 2
done

echo "Master is ready. Configuring replication..."

mysql -u root -pminatoaqua3710 <<EOF
CHANGE MASTER TO
  MASTER_HOST='mysql-master',
  MASTER_USER='root',
  MASTER_PASSWORD='minatoaqua3710',
  MASTER_AUTO_POSITION=1;

START SLAVE;
EOF

echo "Replication started."
mysql -u root -pminatoaqua3710 -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running"
