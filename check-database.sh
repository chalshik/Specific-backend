#!/bin/bash

# Script to verify Supabase database connection

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Testing Supabase Database Connection ===${NC}"
echo

# Connection details
DB_HOST="aws-0-eu-north-1.pooler.supabase.com"
DB_PORT="6543"
DB_USER="postgres.omiupfslobnjbdrlvbkv"
DB_PASSWORD="SpecificPostgre"
DB_NAME="postgres"

echo -e "${YELLOW}Connection details:${NC}"
echo "Host: $DB_HOST"
echo "Port: $DB_PORT"
echo "User: $DB_USER"
echo "Database: $DB_NAME"
echo

# Check if psql is installed
if ! command -v psql &> /dev/null; then
    echo -e "${RED}PostgreSQL client (psql) is not installed.${NC}"
    echo "Please install it with: sudo apt-get install postgresql-client"
    exit 1
fi

# Try connection with pgbouncer
echo -e "${YELLOW}Testing connection with pgbouncer (port 6543)...${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Connection successful!${NC}"
else
    echo -e "${RED}Connection failed!${NC}"
    echo "Error message:"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" 2>&1 | grep -v "connection to server"
fi
echo

# Try direct connection
echo -e "${YELLOW}Testing direct connection (port 5432)...${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p 5432 -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Connection successful!${NC}"
else
    echo -e "${RED}Connection failed!${NC}"
    echo "Error message:"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p 5432 -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" 2>&1 | grep -v "connection to server"
fi

echo
echo -e "${YELLOW}=== Database Connection Test Complete ===${NC}" 