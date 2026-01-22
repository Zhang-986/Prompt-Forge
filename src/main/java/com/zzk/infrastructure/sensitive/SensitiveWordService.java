package com.zzk.infrastructure.sensitive;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 敏感词检测服务
 * 
 * <p>
 * 使用 DFA（确定有限自动机）算法实现高效的敏感词匹配
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
     * 敏感词目录
     */
    private static final String SENSITIVE_WORDS_DIR = "classpath:sensitive/*.txt";

    /**
     * 初始化敏感词库
     */
    @PostConstruct
    public void init() {
        // 加载外部敏感词文件
        loadExternalWords();
        // 构建 DFA
        buildDFA();
        log.info("敏感词库初始化完成，共加载 {} 个敏感词", sensitiveWords.size());
    }

    /**
     * 从 sensitive 目录加载所有敏感词文件
     */
    private void loadExternalWords() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SENSITIVE_WORDS_DIR);

            int fileCount = 0;
            for (Resource resource : resources) {
                if (resource.exists() && resource.isReadable()) {
                    loadWordsFromResource(resource);
                    fileCount++;
                }
            }
            log.info("从 sensitive 目录加载了 {} 个敏感词文件", fileCount);
        } catch (Exception e) {
            log.warn("加载敏感词目录失败: {}", e.getMessage());
        }
    }

    /**
     * 从单个资源文件加载敏感词
     * 
     * @param resource 资源文件
     */
    private void loadWordsFromResource(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 跳过空行、注释行、以及长度过短的词（避免常用词误判）
                if (!line.isEmpty() && !line.startsWith("#") && line.length() >= 3) {
                    sensitiveWords.add(line);
                    count++;
                }
            }
            log.debug("从 {} 加载了 {} 个敏感词", resource.getFilename(), count);
        } catch (Exception e) {
            log.warn("加载敏感词文件 {} 失败: {}", resource.getFilename(), e.getMessage());
        }
    }

    /**
     * 构建 DFA 状态机
     */
    @SuppressWarnings("unchecked")
    private void buildDFA() {
        sensitiveWordMap = new HashMap<>(); // 1️⃣ 创建根节点（空 Map）

        for (String word : sensitiveWords) { // 2️⃣ 遍历每个敏感词
            Map<Character, Object> currentMap = sensitiveWordMap; // 3️⃣ 从根节点开始

            for (int i = 0; i < word.length(); i++) { // 4️⃣ 遍历词的每个字符
                char c = word.charAt(i);
                Object obj = currentMap.get(c); // 5️⃣ 查找当前字符是否已存在

                if (obj == null) { // 6️⃣ 不存在，创建新节点
                    Map<Character, Object> newMap = new HashMap<>();
                    newMap.put(END_FLAG, false); // 默认不是词尾
                    currentMap.put(c, newMap); // 建立连接
                    currentMap = newMap; // 移动到新节点
                } else { // 7️⃣ 已存在，复用路径
                    currentMap = (Map<Character, Object>) obj;
                }

                if (i == word.length() - 1) { // 8️⃣ 如果是最后一个字符
                    currentMap.put(END_FLAG, true); // 标记为词尾
                }
            }
        }
    }

    /**
     * 外部调度器：全文扫描
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty())
            return false;

        // 像雷达扫描一样，以每一个字符作为“起点”去试探
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);

            // 只要这个起点出发能抓到一个词（长度 > 0），立刻收工返回
            if (length > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找第一个匹配的敏感词（用于调试和日志）
     * 
     * @param text 待检测文本
     * @return 匹配到的敏感词，未匹配返回 null
     */
    public String findFirstSensitiveWord(String text) {
        if (text == null || text.isEmpty())
            return null;

        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                return text.substring(i, i + length);
            }
        }
        return null;
    }

    /**
     * 核心匹配引擎：从 startIndex 开始往后“摸”，看能摸到多长的敏感词
     * 
     * @return 匹配到的最长合法词长度（如果没有合规词，返回 0）
     */
    @SuppressWarnings("unchecked")
    private int checkSensitiveWord(String text, int startIndex) {
        // 【初始化】指针回到 Trie 树的根节点
        Map<Character, Object> currentMap = sensitiveWordMap;

        int matchLength = 0; // 探路长度：只要树上有路，它就一直加
        int lastMatchLength = 0; // 确认长度：只有走到标记为 isEnd=true 的节点，才更新这个值

        // 【阶段 A：寻路】从指定的起点开始，一个字一个字往后读
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);

            // 1. 在当前节点的子节点中找这个字符
            Object obj = currentMap.get(c);

            // 如果找不到路了，直接断开（比如树里只有“王八蛋”，但你读到了“王八在”）
            if (obj == null) {
                break;
            }

            // 【阶段 B：状态转移】路是对的，跳进这个子节点（进到套娃的下一层）
            currentMap = (Map<Character, Object>) obj;
            matchLength++; // 探路步数 +1

            // 【阶段 C：标记确认】检查当前节点是不是一个词的终点
            Object endFlag = currentMap.get(END_FLAG);
            if (endFlag != null && (Boolean) endFlag) {
                // 只有这里才是“真匹配”！记录下当前的步数作为“最后的成功记录”
                // 之后还会继续循环，看后面有没有更长的词（贪婪匹配）
                lastMatchLength = matchLength;
            }
        }

        // 返回最后一次“打卡成功”的步数
        return lastMatchLength;
    }

}
