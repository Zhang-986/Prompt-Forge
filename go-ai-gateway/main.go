package main

import (
	"fmt"
	"log"
	"os"

	"github.com/gin-gonic/gin"
	"go-ai-gateway/config"
	"go-ai-gateway/factory"
	"go-ai-gateway/grpcserver"
	"go-ai-gateway/handler"
	"go-ai-gateway/middleware"
	"go-ai-gateway/repository"
	"go-ai-gateway/strategy"
)

func main() {
	log.SetFlags(log.Ldate | log.Ltime | log.Lshortfile)
	log.Println("========================================")
	log.Println("  Go AI Gateway 启动中...")
	log.Println("========================================")

	// 1. 加载配置
	var cfg *config.Config
	if _, err := os.Stat("config.yaml"); err == nil {
		var loadErr error
		cfg, loadErr = config.Load("config.yaml")
		if loadErr != nil {
			log.Fatalf("加载配置失败: %v", loadErr)
		}
		log.Println("[Config] 从 config.yaml 加载配置")
	} else {
		cfg = config.LoadDefault()
		log.Println("[Config] 使用默认配置")
	}

	// 2. 初始化数据库
	repo, err := repository.NewConfigRepo(cfg)
	if err != nil {
		log.Fatalf("初始化数据库失败: %v", err)
	}
	defer repo.Close()
	log.Printf("[DB] 连接 MySQL 成功: %s:%d/%s",
		cfg.Database.Host, cfg.Database.Port, cfg.Database.DBName)

	// 3. 初始化工厂 (注册所有策略)
	llmFactory := factory.NewFactory(
		strategy.NewOpenAIStrategy(),
		strategy.NewClaudeStrategy(),
		strategy.NewGeminiStrategy(),
	)

	// 4. 启动 gRPC 服务 (后台 goroutine)
	grpcAddr := fmt.Sprintf(":%d", cfg.Server.GRPCPort)
	go func() {
		if err := grpcserver.StartGRPCServer(grpcAddr, llmFactory, repo); err != nil {
			log.Fatalf("gRPC 服务启动失败: %v", err)
		}
	}()

	// 5. 配置 HTTP 路由 (Gin)
	r := gin.Default()
	r.Use(middleware.CORS())

	// 公开端点
	r.GET("/health", handler.HealthHandler())

	// 需要鉴权的 AI 端点
	api := r.Group("/api/ai", middleware.Auth(cfg.JWT.Secret))
	{
		api.POST("/stream", handler.StreamHandler(llmFactory, repo))
		api.POST("/arena/compete", handler.ArenaHandler(llmFactory, repo))
	}

	// 6. 启动 HTTP 服务
	addr := fmt.Sprintf(":%d", cfg.Server.Port)
	log.Println("========================================")
	log.Printf("  HTTP 服务启动: %s", addr)
	log.Printf("  gRPC 服务启动: %s", grpcAddr)
	log.Println("========================================")
	log.Println("  端点:")
	log.Println("    POST /api/ai/stream          - SSE 流式生成")
	log.Println("    POST /api/ai/arena/compete    - 竞技场多模型对比")
	log.Println("    GET  /health                  - 健康检查")
	log.Println("========================================")

	if err := r.Run(addr); err != nil {
		log.Fatalf("HTTP 服务启动失败: %v", err)
	}
}
