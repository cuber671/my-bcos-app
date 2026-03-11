#!/bin/bash
cd /data
# Use expect-like behavior or pipe input
{
    echo "deploy WarehouseReceiptCore"
    sleep 1
    echo "quit"
} | java -cp "apps/*:conf/:lib/*:classes/" console.Console group0
