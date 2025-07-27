#!/bin/bash

# 사용법 출력 함수
usage() {
    echo "사용법: $0 [-d] [-b]"
    echo "  -d: Docker Compose로 인프라 실행 (MySQL, Redis)"
    echo "  -b: 애플리케이션 빌드 및 실행"
    echo "  예시: $0 -d -b  # 인프라 실행 후 애플리케이션 빌드 및 실행"
    echo "  예시: $0 -d     # 인프라만 실행"
    echo "  예시: $0 -b     # 애플리케이션만 빌드 및 실행"
}

# 기본값 설정
DOCKER_FLAG=false
BUILD_FLAG=false

# 인자 파싱
while getopts "db" opt; do
    case $opt in
        d)
            DOCKER_FLAG=true
            ;;
        b)
            BUILD_FLAG=true
            ;;
        *)
            usage
            exit 1
            ;;
    esac
done

echo "=== 뉴스 피드 시스템 빌드 및 실행 스크립트 ==="
echo "Docker 인프라 실행: $DOCKER_FLAG"
echo "애플리케이션 빌드 및 실행: $BUILD_FLAG"
echo ""

# Docker Compose로 인프라 실행
if [ "$DOCKER_FLAG" = true ]; then
    echo "=== Docker 인프라 실행 중 ==="
    echo "MySQL과 Redis 컨테이너를 시작합니다..."
    
    docker-compose up -d
    
    if [ $? -ne 0 ]; then
        echo "Docker 인프라 실행 실패!"
        exit 1
    fi
    
    echo "Docker 인프라 실행 완료!"
    echo "MySQL: localhost:3306"
    echo "Redis: localhost:6379"
    echo ""
    
    # 데이터베이스 준비 대기
    echo "데이터베이스 준비 대기 중..."
    sleep 10
fi

# 애플리케이션 빌드 및 실행
if [ "$BUILD_FLAG" = true ]; then
    echo "=== Spring Boot 애플리케이션 빌드 및 실행 ==="
    
    # Gradle 래퍼 권한 확인
    if [ ! -x "./gradlew" ]; then
        echo "Gradle 래퍼에 실행 권한을 부여합니다..."
        chmod +x ./gradlew
    fi
    
    echo "애플리케이션 빌드 중..."
    ./gradlew clean build
    
    if [ $? -ne 0 ]; then
        echo "애플리케이션 빌드 실패!"
        exit 1
    fi
    
    echo "애플리케이션 빌드 완료!"
    echo ""
    
    echo "애플리케이션 실행 중..."
    echo "서버가 시작되면 http://localhost:8080 에서 접근할 수 있습니다."
    echo "테스트 사용자: user1/password123, user2/password123, user3/password123"
    echo ""
    
    ./gradlew bootRun
fi

echo ""
echo "=== 완료 ==="
if [ "$DOCKER_FLAG" = true ]; then
    echo "Docker 인프라: 실행 중"
fi
if [ "$BUILD_FLAG" = true ]; then
    echo "Spring Boot 애플리케이션: 실행 중 (포트 8080)"
fi 
