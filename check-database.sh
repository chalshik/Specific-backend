#!/bin/bash

# Script to verify Supabase database connection

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Testing Supabase Database Connection ===${NC}"
echo

# Connection details - Direct connection (port 5432)
DB_HOST="aws-0-eu-north-1.pooler.supabase.com"
DB_PORT="5432"
DB_USER="postgres.omiupfslobnjbdrlvbkv"
DB_PASSWORD="SpecificPostgre"
DB_NAME="postgres"

echo -e "${YELLOW}Connection details (direct connection):${NC}"
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

# Try direct connection (recommended for Hibernate/JPA)
echo -e "${YELLOW}Testing direct connection (port 5432)...${NC}"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Connection successful!${NC}"
    
    # Additional database checks
    echo -e "\n${YELLOW}Checking user table...${NC}"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users');" 
    
    echo -e "\n${YELLOW}Testing transaction...${NC}"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "BEGIN; SELECT 1; COMMIT;" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Transaction test successful!${NC}"
    else
        echo -e "${RED}Transaction test failed!${NC}"
        PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "BEGIN; SELECT 1; COMMIT;" 2>&1
    fi
else
    echo -e "${RED}Connection failed!${NC}"
    echo "Error message:"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" 2>&1 | grep -v "connection to server"
fi
echo

# Test pgbouncer connection - not recommended for transactions, but could be for read-only
DB_PORT_PGBOUNCER="6543"
echo -e "${YELLOW}Testing connection with pgbouncer (port 6543) - for comparison...${NC}"
echo "Note: Not recommended for JPA transactions"
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT_PGBOUNCER -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Connection successful!${NC}"
else
    echo -e "${RED}Connection failed!${NC}"
    echo "Error message:"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT_PGBOUNCER -U $DB_USER -d $DB_NAME -c "SELECT 1 as connection_test;" 2>&1 | grep -v "connection to server"
fi

echo
echo -e "${YELLOW}=== Database Connection Test Complete ===${NC}" 