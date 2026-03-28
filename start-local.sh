#!/bin/bash

# ============================================================
# iemodo 微服务系统本地启动脚本
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Java 配置
export JAVA_HOME=/Users/breanna/Library/Java/JavaVirtualMachines/openjdk-22.0.1/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# 基础目录
BASE_DIR="/Users/breanna/Documents/code/iemodo"
LOG_DIR="$BASE_DIR/logs"

# 创建日志目录
mkdir -p $LOG_DIR

# 服务配置
INFRA_SERVICES=("postgres" "redis" "nacos")
MICRO_SERVICES=("user-service" "product-service" "order-service" "inventory-service" "payment-service" "pricing-service" "tax-service" "map-service" "marketing-service" "file-service" "tenant-management-service" "fulfillment-service")
GATEWAY="api-gateway"

# 端口映射
PORTS=(
    "5433:PostgreSQL"
    "6379:Redis"
    "8848:Nacos"
    "8080:API Gateway"
    "8081:User Service"
    "8082:Product Service"
    "8083:Order Service"
    "8084:Inventory Service"
    "8085:Payment Service"
    "8086:Pricing Service"
    "8087:Map Service"
    "8088:Tax Service"
    "8089:Marketing Service"
    "8090:File Service"
    "8091:Tenant Management Service"
    "8092:Fulfillment Service"
)

print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -i :$port > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 等待服务启动
wait_for_service() {
    local port=$1
    local service_name=$2
    local max_attempts=${3:-30}
    local attempt=1
    
    print_info "等待 $service_name 启动 (端口 $port)..."
    
    while ! check_port $port; do
        if [ $attempt -ge $max_attempts ]; then
            print_error "$service_name 启动超时"
            return 1
        fi
        sleep 2
        attempt=$((attempt + 1))
        echo -n "."
    done
    
    echo ""
    print_success "$service_name 已启动"
    return 0
}

# 检查 Docker 容器状态
check_docker_container() {
    local container=$1
    if docker ps --format "table {{.Names}}" | grep -q "^${container}$"; then
        return 0
    else
        return 1
    fi
}

# 启动基础设施
start_infrastructure() {
    print_header "启动基础设施"
    
    # PostgreSQL
    if check_docker_container "iemodo-postgres"; then
        print_success "PostgreSQL 已在运行"
    else
        print_info "启动 PostgreSQL..."
        docker start iemodo-postgres 2>/dev/null || {
            print_info "创建 PostgreSQL 容器..."
            docker run -d \
                --name iemodo-postgres \
                -p 5433:5432 \
                -e POSTGRES_USER=iemodo \
                -e POSTGRES_PASSWORD=iemodo \
                -e POSTGRES_DB=iemodo \
                postgres:15
        }
        sleep 5
    fi
    
    # Redis
    if check_docker_container "iemodo-redis"; then
        print_success "Redis 已在运行"
    else
        print_info "启动 Redis..."
        docker start iemodo-redis 2>/dev/null || {
            print_info "创建 Redis 容器..."
            docker run -d \
                --name iemodo-redis \
                -p 6379:6379 \
                redis:7-alpine \
                redis-server --requirepass redis123
        }
        sleep 3
    fi
    
    # Nacos
    if check_docker_container "iemodo-nacos"; then
        print_success "Nacos 已在运行"
    else
        print_info "启动 Nacos..."
        docker start iemodo-nacos 2>/dev/null || {
            print_info "创建 Nacos 容器..."
            docker run -d \
                --name iemodo-nacos \
                -p 8848:8848 \
                -e MODE=standalone \
                nacos/nacos-server:v2.3.0
        }
        sleep 10
    fi
    
    print_success "基础设施启动完成"
}

# 停止微服务
stop_microservices() {
    print_header "停止微服务"
    
    for service in "${MICRO_SERVICES[@]}" $GATEWAY; do
        local port_file="/tmp/iemodo_${service}_pid"
        if [ -f "$port_file" ]; then
            local pid=$(cat "$port_file")
            if kill -0 $pid 2>/dev/null; then
                print_info "停止 $service (PID: $pid)..."
                kill $pid 2>/dev/null || true
                rm -f "$port_file"
            fi
        fi
    done
    
    print_success "微服务已停止"
}

# 启动单个服务
start_service() {
    local service=$1
    local port=$2
    local log_file="$LOG_DIR/${service}.log"
    local pid_file="/tmp/iemodo_${service}_pid"
    
    # 检查是否已经在运行
    if [ -f "$pid_file" ]; then
        local old_pid=$(cat "$pid_file" 2>/dev/null || echo "")
        if [ -n "$old_pid" ] && kill -0 $old_pid 2>/dev/null; then
            print_success "$service 已在运行 (PID: $old_pid)"
            return 0
        fi
    fi
    
    # 检查端口是否被占用
    if check_port $port; then
        print_error "端口 $port 已被占用，无法启动 $service"
        return 1
    fi
    
    print_info "启动 $service (端口 $port)..."
    
    cd "$BASE_DIR/$service"
    nohup mvn spring-boot:run -DskipTests -q > "$log_file" 2>&1 &
    local pid=$!
    echo $pid > "$pid_file"
    
    # 等待服务启动
    if wait_for_service $port "$service" 60; then
        return 0
    else
        print_error "$service 启动失败，查看日志: $log_file"
        return 1
    fi
}

# 启动所有微服务
start_all_services() {
    print_header "启动微服务"
    
    # 先启动基础服务（无依赖）
    print_info "启动基础服务..."
    start_service "user-service" 8081 &
    start_service "product-service" 8082 &
    start_service "tenant-management-service" 8091 &
    sleep 15
    
    # 启动依赖服务
    print_info "启动业务服务..."
    start_service "inventory-service" 8084 &
    start_service "pricing-service" 8086 &
    start_service "tax-service" 8088 &
    start_service "map-service" 8087 &
    start_service "marketing-service" 8089 &
    start_service "file-service" 8090 &
    start_service "fulfillment-service" 8092 &
    sleep 10
    
    # 启动核心服务
    print_info "启动核心服务..."
    start_service "order-service" 8083 &
    start_service "payment-service" 8085 &
    sleep 10
    
    # 最后启动网关
    print_info "启动 API Gateway..."
    start_service "$GATEWAY" 8080 &
    sleep 5
    
    wait
    print_success "所有服务启动完成"
}

# 快速启动模式（跳过测试编译）
fast_start() {
    print_header "快速启动模式"
    print_info "跳过测试，直接启动..."
    start_all_services
}

# 完整启动模式（编译后启动）
full_start() {
    print_header "完整启动模式"
    
    print_info "编译所有服务..."
    cd "$BASE_DIR"
    mvn clean compile -DskipTests -q
    
    start_all_services
}

# 显示服务状态
show_status() {
    print_header "服务状态"
    
    echo -e "${BLUE}基础设施:${NC}"
    for container in iemodo-postgres iemodo-redis iemodo-nacos; do
        if check_docker_container "$container"; then
            print_success "$container - 运行中"
        else
            print_error "$container - 未运行"
        fi
    done
    
    echo ""
    echo -e "${BLUE}微服务:${NC}"
    for service in "${MICRO_SERVICES[@]}" "$GATEWAY"; do
        local pid_file="/tmp/iemodo_${service}_pid"
        local port=$(echo "$service" | grep -o '[0-9]\+' | head -1 || echo "8080")
        
        if [ -f "$pid_file" ]; then
            local pid=$(cat "$pid_file")
            if kill -0 $pid 2>/dev/null; then
                if check_port $port 2>/dev/null; then
                    print_success "$service (端口 $port) - 运行中 (PID: $pid)"
                else
                    print_error "$service (端口 $port) - 启动中..."
                fi
            else
                print_error "$service - 已停止"
            fi
        else
            print_error "$service - 未启动"
        fi
    done
}

# 停止所有服务
stop_all() {
    print_header "停止所有服务"
    
    stop_microservices
    
    print_info "停止基础设施..."
    docker stop iemodo-nacos iemodo-redis iemodo-postgres 2>/dev/null || true
    
    print_success "所有服务已停止"
}

# 查看日志
tail_logs() {
    local service=$1
    if [ -z "$service" ]; then
        print_info "可用日志:"
        ls -1 $LOG_DIR/*.log 2>/dev/null | xargs -n1 basename || echo "无日志文件"
        return
    fi
    
    local log_file="$LOG_DIR/${service}.log"
    if [ -f "$log_file" ]; then
        tail -f "$log_file"
    else
        print_error "日志文件不存在: $log_file"
    fi
}

# 帮助信息
show_help() {
    cat << EOF
iemodo 本地启动脚本

用法: ./start-local.sh [命令] [选项]

命令:
  infra           只启动基础设施 (PostgreSQL, Redis, Nacos)
  start           快速启动所有服务 (已编译)
  full            完整启动 (编译 + 启动)
  stop            停止所有服务
  status          查看服务状态
  restart         重启所有服务
  logs [服务名]   查看服务日志
  help            显示帮助

示例:
  ./start-local.sh infra       # 只启动数据库等基础设施
  ./start-local.sh full        # 完整启动
  ./start-local.sh start       # 快速启动
  ./start-local.sh status      # 查看状态
  ./start-local.sh logs user-service  # 查看 user-service 日志

访问地址:
  API Gateway: http://localhost:8080
  Nacos:       http://localhost:8848/nacos (nacos/nacos)
  PostgreSQL:  localhost:5433
  Redis:       localhost:6379 (密码: redis123)

服务端口:
  8080 - API Gateway
  8081 - User Service
  8082 - Product Service
  8083 - Order Service
  8084 - Inventory Service
  8085 - Payment Service
  8086 - Pricing Service
  8087 - Map Service
  8088 - Tax Service
  8089 - Marketing Service
  8090 - File Service
  8091 - Tenant Management Service
  8092 - Fulfillment Service

EOF
}

# 主函数
main() {
    case "${1:-help}" in
        infra)
            start_infrastructure
            ;;
        start)
            start_infrastructure
            fast_start
            show_status
            ;;
        full)
            start_infrastructure
            full_start
            show_status
            ;;
        stop)
            stop_all
            ;;
        restart)
            stop_all
            sleep 3
            start_infrastructure
            fast_start
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            tail_logs "$2"
            ;;
        help|*)
            show_help
            ;;
    esac
}

main "$@"
