package middleware

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
)

// Auth JWT 鉴权中间件
// 共享 Java 侧的 JWT Secret，解析 token 中的 userId (Subject)
func Auth(secret string) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "缺少 Authorization 头"})
			c.Abort()
			return
		}

		tokenStr := strings.TrimPrefix(authHeader, "Bearer ")
		if tokenStr == authHeader {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization 格式错误，应为 Bearer <token>"})
			c.Abort()
			return
		}

		// 使用与 Java 相同的 HS256 + Secret 解析
		token, err := jwt.Parse(tokenStr, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, jwt.ErrSignatureInvalid
			}
			return []byte(secret), nil
		})

		if err != nil || !token.Valid {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Token 无效或已过期"})
			c.Abort()
			return
		}

		// 从 Subject 中提取 userId (Java 侧 setSubject(String.valueOf(userId)))
		subject, err := token.Claims.GetSubject()
		if err != nil || subject == "" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Token 中无用户信息"})
			c.Abort()
			return
		}

		// 转为 int64
		var userID int64
		if _, err := fmt.Sscanf(subject, "%d", &userID); err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "无效的用户 ID"})
			c.Abort()
			return
		}

		// 注入上下文，类似 Java 的 @RequestAttribute("userId")
		c.Set("userId", userID)
		c.Next()
	}
}
