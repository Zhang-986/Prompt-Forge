package com.zzk.infrastructure.aop;

import com.zzk.infrastructure.annotation.SensitiveCheck;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.infrastructure.sensitive.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 敏感词检测切面
 * 
 * <p>
 * 拦截带有 @SensitiveCheck 注解的方法，对指定字段进行敏感词检测
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SensitiveCheckAspect {

    private final SensitiveWordService sensitiveWordService;

    /**
     * 环绕通知：拦截带有 @SensitiveCheck 注解的方法
     */
    @Around("@annotation(sensitiveCheck)")
    public Object aroundSensitiveCheck(ProceedingJoinPoint joinPoint, SensitiveCheck sensitiveCheck) throws Throwable {
        log.debug("敏感词检测切面触发: {}", joinPoint.getSignature().getName());

        String[] fields = sensitiveCheck.fields();
        SensitiveCheck.Mode mode = sensitiveCheck.mode();
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        if (fields.length == 0) {
            // 如果没有指定字段，检测所有 String 类型参数
            checkAllStringArgs(args, paramNames, mode);
        } else {
            // 检测指定字段
            checkSpecifiedFields(args, paramNames, fields, mode);
        }

        return joinPoint.proceed();
    }

    /**
     * 检测所有 String 类型参数
     */
    private void checkAllStringArgs(Object[] args, String[] paramNames, SensitiveCheck.Mode mode) {
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof String text) {
                checkAndHandle(text, paramNames[i], mode);
            } else if (arg != null) {
                // 检测对象中的所有 String 字段
                checkObjectStringFields(arg, mode);
            }
        }
    }

    /**
     * 检测对象中的所有 String 字段
     */
    private void checkObjectStringFields(Object obj, SensitiveCheck.Mode mode) {
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == String.class) {
                field.setAccessible(true);
                try {
                    String value = (String) field.get(obj);
                    if (value != null) {
                        checkAndHandle(value, field.getName(), mode);
                    }
                } catch (IllegalAccessException e) {
                    log.warn("无法访问字段: {}", field.getName());
                }
            }
        }
    }

    /**
     * 检测指定字段
     */
    private void checkSpecifiedFields(Object[] args, String[] paramNames, String[] fields, SensitiveCheck.Mode mode) {
        for (String fieldPath : fields) {
            String[] parts = fieldPath.split("\\.");

            // 查找参数对象
            Object targetObj = null;
            int startIndex = 0;

            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    // 如果字段路径以参数名开头
                    if (parts[0].equals(paramNames[i])) {
                        targetObj = args[i];
                        startIndex = 1;
                        break;
                    }
                    // 尝试在参数对象中查找字段
                    Object fieldValue = getFieldValue(args[i], parts[0]);
                    if (fieldValue != null) {
                        targetObj = args[i];
                        startIndex = 0;
                        break;
                    }
                }
            }

            if (targetObj == null && args.length > 0 && args[0] != null) {
                targetObj = args[0];
                startIndex = 0;
            }

            // 递归获取字段值
            Object value = targetObj;
            for (int i = startIndex; i < parts.length && value != null; i++) {
                value = getFieldValue(value, parts[i]);
            }

            if (value instanceof String) {
                checkAndHandle((String) value, fieldPath, mode);
            }
        }
    }

    /**
     * 获取对象字段值
     */
    private Object getFieldValue(Object obj, String fieldName) {
        if (obj == null)
            return null;
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            log.debug("获取字段值失败: {}.{}", obj.getClass().getSimpleName(), fieldName);
        }
        return null;
    }

    /**
     * 在类及其父类中查找字段
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 检测并处理敏感词（优化版：短路返回）
     * 
     * <p>
     * 使用 containsSensitiveWord 进行快速判断，发现敏感词立即抛出异常，
     * 不再统计具体有哪些敏感词，提升检测性能
     */
    private void checkAndHandle(String text, String fieldName, SensitiveCheck.Mode mode) {
        if (text == null || text.isEmpty())
            return;

        // 短路优化：发现即返回，不统计具体有哪些
        if (sensitiveWordService.containsSensitiveWord(text)) {
            log.warn("检测到敏感词 - 字段: {}", fieldName);
            throw new BusinessException("内容包含敏感词，请修改后重试");
        }
    }
}
