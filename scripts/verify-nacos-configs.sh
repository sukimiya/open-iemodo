#!/bin/bash

# Nacos 配置验证脚本
# 使用方式: ./verify-nacos-configs.sh [nacos地址] [用户名] [密码]

set -e

NACOS_URL=${1:-"http://localhost:8848"}
NACOS_USER=${2:-"nacos"}
NACOS_PASS=${3:-"nacos"}
GROUP="IEMODO"

echo "======================================"
echo "Nacos 配置验证工具"
echo "======================================"
echo "Nacos地址: $NACOS_URL"
echo "Group: $GROUP"
echo "======================================"
echo ""

# 配置列表
CONFIGS=(
    "payment-service.yaml"
    "product-service.yaml"
    "inventory-service.yaml"
    "file-service.yaml"
    "tenant-management.yaml"
    "api-gateway.yaml"
    "user-service.yaml"
    "order-service.yaml"
)

echo "检查配置是否存在..."
echo "======================================"

all_exist=true
for config in "${CONFIGS[@]}"; do
    echo -n "检查 $config ... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        "$NACOS_URL/nacos/v1/cs/configs?dataId=$config&group=$GROUP" \
        -u "$NACOS_USER:$NACOS_PASS" 2>/dev/null || echo "000")
    
    if [ "$response" = "200" ]; then
        echo "✓ 存在"
    else
        echo "✗ 不存在 (HTTP $response)"
        all_exist=false
    fi
done

echo "======================================"
echo ""

if [ "$all_exist" = true ]; then
    echo "✓ 所有配置已正确导入!"
    echo ""
    echo "可以启动服务了:"
    echo "  mvn spring-boot:run -pl payment-service"
    echo "  mvn spring-boot:run -pl product-service"
    echo "  ..."
else
    echo "✗ 部分配置缺失，请运行导入脚本:"
    echo "  ./import-nacos-configs.sh"
fi

echo ""
echo "查看所有配置:"
echo "  访问: $NACOS_URL/nacos"
echo "  用户名: $NACOS_USER"
echo "  密码: $NACOS_PASS"
