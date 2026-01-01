package com.zzk.infrastructure.sensitive;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 敏感词检测服务
 * 
 * <p>使用 DFA（确定有限自动机）算法实现高效的敏感词匹配
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
public class SensitiveWordService {

    /**
     * 敏感词库（DFA 状态机）
     */
    private Map<Character, Object> sensitiveWordMap = new HashMap<>();
    
    /**
     * 敏感词集合（用于管理）
     */
    private Set<String> sensitiveWords = new HashSet<>();
    
    /**
     * DFA 结束标记
     */
    private static final Character END_FLAG = '\u0000';

    /**
     * 初始化敏感词库
     */
    @PostConstruct
    public void init() {
        // 加载内置敏感词
        loadBuiltInWords();
        // 加载外部敏感词文件
        loadExternalWords();
        // 构建 DFA
        buildDFA();
        log.info("敏感词库初始化完成，共加载 {} 个敏感词", sensitiveWords.size());
    }

    /**
     * 加载内置敏感词（政治敏感词示例）
     */
    private void loadBuiltInWords() {
        // 这里添加一些基础的政治敏感词示例
        // 实际项目中应该从配置文件或数据库加载完整词库
        String[] builtInWords = {
            // 政治相关（示例，实际词库应更完整）
            "法轮功", "藏独", "疆独", "台独", "港独",
            "反共", "反党", "颠覆政权", "推翻政府",
            // 违禁词
            "暴力", "恐怖主义", "极端主义",
            // 可以根据需要扩展
        };
        sensitiveWords.addAll(Arrays.asList(builtInWords));
    }

    /**
     * 从外部文件加载敏感词
     */
    private void loadExternalWords() {
        try {
            ClassPathResource resource = new ClassPathResource("sensitive-words.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            sensitiveWords.add(line);
                        }
                    }
                }
                log.info("从 sensitive-words.txt 加载敏感词成功");
            }
        } catch (Exception e) {
            log.warn("加载外部敏感词文件失败: {}", e.getMessage());
        }
    }

    /**
     * 构建 DFA 状态机
     */
    @SuppressWarnings("unchecked")
    private void buildDFA() {
        sensitiveWordMap = new HashMap<>();
        for (String word : sensitiveWords) {
            Map<Character, Object> currentMap = sensitiveWordMap;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                Object obj = currentMap.get(c);
                if (obj == null) {
                    Map<Character, Object> newMap = new HashMap<>();
                    newMap.put(END_FLAG, false);
                    currentMap.put(c, newMap);
                    currentMap = newMap;
                } else {
                    currentMap = (Map<Character, Object>) obj;
                }
                if (i == word.length() - 1) {
                    currentMap.put(END_FLAG, true);
                }
            }
        }
    }

    /**
     * 检测文本是否包含敏感词
     * 
     * @param text 待检测文本
     * @return true 包含敏感词，false 不包含
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文本中的所有敏感词
     * 
     * @param text 待检测文本
     * @return 敏感词列表
     */
    public List<String> findSensitiveWords(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                result.add(text.substring(i, i + length));
                i += length - 1;
            }
        }
        return result;
    }

    /**
     * 替换文本中的敏感词
     * 
     * @param text 待处理文本
     * @param replacement 替换字符
     * @return 替换后的文本
     */
    public String replaceSensitiveWords(String text, char replacement) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        for (int i = 0; i < result.length(); i++) {
            int length = checkSensitiveWord(result.toString(), i);
            if (length > 0) {
                for (int j = i; j < i + length; j++) {
                    result.setCharAt(j, replacement);
                }
                i += length - 1;
            }
        }
        return result.toString();
    }

    /**
     * 替换敏感词为 ***
     */
    public String replaceSensitiveWords(String text) {
        return replaceSensitiveWords(text, '*');
    }

    /**
     * 检查从指定位置开始是否存在敏感词
     * 
     * @param text 文本
     * @param startIndex 开始位置
     * @return 敏感词长度，0 表示不存在
     */
    @SuppressWarnings("unchecked")
    private int checkSensitiveWord(String text, int startIndex) {
        Map<Character, Object> currentMap = sensitiveWordMap;
        int matchLength = 0;
        int lastMatchLength = 0;
        
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            Object obj = currentMap.get(c);
            if (obj == null) {
                break;
            }
            currentMap = (Map<Character, Object>) obj;
            matchLength++;
            
            Object endFlag = currentMap.get(END_FLAG);
            if (endFlag != null && (Boolean) endFlag) {
                lastMatchLength = matchLength;
            }
        }
        return lastMatchLength;
    }

    /**
     * 动态添加敏感词
     */
    public void addSensitiveWord(String word) {
        if (word != null && !word.isEmpty()) {
            sensitiveWords.add(word);
            buildDFA();
            log.info("动态添加敏感词: {}", word);
        }
    }

    /**
     * 动态移除敏感词
     */
    public void removeSensitiveWord(String word) {
        if (sensitiveWords.remove(word)) {
            buildDFA();
            log.info("动态移除敏感词: {}", word);
        }
    }

    /**
     * 获取所有敏感词
     */
    public Set<String> getAllSensitiveWords() {
        return new HashSet<>(sensitiveWords);
    }
}
