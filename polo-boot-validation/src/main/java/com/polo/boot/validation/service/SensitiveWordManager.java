package com.polo.boot.validation.service;

import com.polo.boot.validation.annotation.InputContent;
import com.polo.boot.validation.model.DynamicWordEntry;
import com.polo.boot.validation.model.SensitiveWordDefinition;
import com.polo.boot.validation.properties.ValidationProperties;
import com.polo.boot.validation.provider.SensitiveWordProvider;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class SensitiveWordManager {
    private final ValidationProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final List<SensitiveWordProvider> sensitiveWordProviders;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Character, Node> wordTree = new HashMap<>();
    private final Map<String, Set<String>> categoryWords = new HashMap<>();
    private int maxWordLength;

    public SensitiveWordManager(ValidationProperties properties,
                                StringRedisTemplate redisTemplate,
                                List<SensitiveWordProvider> sensitiveWordProviders) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.sensitiveWordProviders = sensitiveWordProviders == null ? List.of() : List.copyOf(sensitiveWordProviders);
    }

    @PostConstruct
    public void init() {
        reloadAllWords();
    }

    private static class Node {
        private final Map<Character, Node> next = new HashMap<>();
        private boolean isEnd;
        private String category;
        private int level;
    }

    public void addWord(String word, String category, int level) {
        String normalizedWord = normalizeWord(word);
        String normalizedCategory = normalizeCategoryCode(category);
        if (normalizedWord == null || normalizedCategory == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            addWordInternal(normalizedWord, normalizedCategory, normalizeLevel(level));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<MatchResult> check(String text) {
        List<MatchResult> results = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return results;
        }

        lock.readLock().lock();
        try {
            char[] chars = text.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                Node node = wordTree.get(chars[i]);
                if (node == null) {
                    continue;
                }

                StringBuilder word = new StringBuilder().append(chars[i]);
                int endPos = i;
                if (node.isEnd) {
                    results.add(new MatchResult(i, endPos, word.toString(), node.category, node.level));
                }

                int upperBound = Math.min(chars.length, i + maxWordLength);
                for (int j = i + 1; j < upperBound; j++) {
                    node = node.next.get(chars[j]);
                    if (node == null) {
                        break;
                    }
                    word.append(chars[j]);
                    if (node.isEnd) {
                        endPos = j;
                        results.add(new MatchResult(i, endPos, word.toString(), node.category, node.level));
                    }
                }
            }
            return results;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String replace(String text, String maskChar) {
        List<MatchResult> matches = check(text);
        if (matches.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text);
        for (int i = matches.size() - 1; i >= 0; i--) {
            MatchResult match = matches.get(i);
            String replacement = maskChar.repeat(match.getWord().length());
            result.replace(match.getStart(), match.getEnd() + 1, replacement);
        }
        return result.toString();
    }

    public boolean contains(String text) {
        return !check(text).isEmpty();
    }

    @Data
    public static class MatchResult {
        private final int start;
        private final int end;
        private final String word;
        private final String category;
        private final int level;
    }

    @Scheduled(
            initialDelayString = "${polo.validation.input-content.word-library.refresh-interval:600000}",
            fixedDelayString = "${polo.validation.input-content.word-library.refresh-interval:600000}")
    public void refreshDynamicWords() {
        ValidationProperties.WordLibraryProperties wordLibrary = properties.getInputContent().getWordLibrary();
        if (!wordLibrary.isDynamicEnabled() && !wordLibrary.isDatabaseEnabled()) {
            return;
        }
        reloadAllWords();
    }

    public int refreshNow() {
        reloadAllWords();
        return getTotalWordCount();
    }

    public DynamicWordEntry addDynamicWord(String word, String category, int level) {
        ensureDynamicWordManagementAvailable();
        String normalizedWord = normalizeWord(word);
        String normalizedCategory = normalizeCategoryCode(category);
        int normalizedLevel = normalizeLevel(level);
        redisTemplate.opsForHash().put(
                properties.getInputContent().getWordLibrary().getRedisKey(),
                normalizedWord,
                normalizedCategory + "|" + normalizedLevel
        );
        addWord(normalizedWord, normalizedCategory, normalizedLevel);
        log.info("动态词库新增敏感词成功: word={}, category={}, level={}", normalizedWord, normalizedCategory, normalizedLevel);
        return new DynamicWordEntry(normalizedWord, normalizedCategory, normalizedLevel);
    }

    public boolean removeDynamicWord(String word) {
        ensureDynamicWordManagementAvailable();
        String normalizedWord = normalizeWord(word);
        Long removed = redisTemplate.opsForHash().delete(
                properties.getInputContent().getWordLibrary().getRedisKey(),
                normalizedWord
        );
        refreshNow();
        boolean removedSuccessfully = removed != null && removed > 0;
        if (removedSuccessfully) {
            log.info("动态词库删除敏感词成功: word={}", normalizedWord);
        }
        return removedSuccessfully;
    }

    public List<DynamicWordEntry> listDynamicWords() {
        ensureDynamicWordManagementAvailable();
        Map<Object, Object> words = redisTemplate.opsForHash().entries(properties.getInputContent().getWordLibrary().getRedisKey());
        List<DynamicWordEntry> entries = new ArrayList<>();
        words.forEach((key, value) -> entries.add(parseDynamicWordEntry(String.valueOf(key), String.valueOf(value))));
        entries.sort(Comparator
                .comparing(DynamicWordEntry::category)
                .thenComparing(DynamicWordEntry::level)
                .thenComparing(DynamicWordEntry::word));
        return entries;
    }

    public boolean isDynamicWordManagementAvailable() {
        return properties.getInputContent().getWordLibrary().isDynamicEnabled() && redisTemplate != null;
    }

    public boolean isDatabaseWordManagementAvailable() {
        return properties.getInputContent().getWordLibrary().isDatabaseEnabled() && !sensitiveWordProviders.isEmpty();
    }

    public String getDynamicWordRedisKey() {
        return properties.getInputContent().getWordLibrary().getRedisKey();
    }

    public int getRegisteredProviderCount() {
        return sensitiveWordProviders.size();
    }

    private void reloadAllWords() {
        lock.writeLock().lock();
        try {
            wordTree.clear();
            categoryWords.clear();
            maxWordLength = 0;
            loadBuiltInWords();
            loadDynamicWords();
            loadDatabaseWords();
            log.info("敏感词词库重载完成，共 {} 个词", getTotalWordCount());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadBuiltInWords() {
        ValidationProperties.WordLibraryProperties wordLibrary = properties.getInputContent().getWordLibrary();
        if (!wordLibrary.isBuiltInEnabled()) {
            return;
        }

        String basePath = wordLibrary.getBuiltInPath();
        int loadedCount = 0;
        String normalizedBasePath = basePath == null ? "" : basePath.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        String pattern = "classpath*:" + (normalizedBasePath.isEmpty() ? "" : normalizedBasePath + "/") + "*.txt";
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(pattern);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String categoryCode = resolveCategoryCodeFromFilename(filename);
                if (categoryCode == null) {
                    continue;
                }
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        String[] parts = line.split("\\|");
                        String word = parts[0].trim();
                        int level = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 3;
                        addWordInternal(word, categoryCode, normalizeLevel(level));
                        loadedCount++;
                    }
                } catch (IOException e) {
                    log.warn("加载内置词库失败: {}", filename, e);
                }
            }
        } catch (IOException e) {
            log.warn("扫描内置词库目录失败: {}", normalizedBasePath, e);
        }
        log.info("内置词库加载完成，共 {} 个词", loadedCount);
    }

    private void loadDynamicWords() {
        ValidationProperties.WordLibraryProperties wordLibrary = properties.getInputContent().getWordLibrary();
        if (!wordLibrary.isDynamicEnabled() || redisTemplate == null) {
            return;
        }

        try {
            Map<Object, Object> words = redisTemplate.opsForHash().entries(wordLibrary.getRedisKey());
            words.forEach((key, value) -> {
                DynamicWordEntry entry = parseDynamicWordEntry(String.valueOf(key), String.valueOf(value));
                addWordInternal(entry.word(), entry.category(), entry.level());
            });
            log.info("Redis 动态词库加载完成，共 {} 个词", words.size());
        } catch (Exception e) {
            log.error("加载 Redis 动态词库失败", e);
        }
    }

    private void loadDatabaseWords() {
        ValidationProperties.WordLibraryProperties wordLibrary = properties.getInputContent().getWordLibrary();
        if (!wordLibrary.isDatabaseEnabled()) {
            return;
        }
        if (sensitiveWordProviders.isEmpty()) {
            log.warn("已开启数据库词库加载，但未发现 SensitiveWordProvider 实现");
            return;
        }

        int totalLoaded = 0;
        for (SensitiveWordProvider provider : sensitiveWordProviders) {
            List<SensitiveWordDefinition> definitions = provider.loadWords();
            if (definitions == null || definitions.isEmpty()) {
                continue;
            }
            for (SensitiveWordDefinition definition : definitions) {
                if (definition == null) {
                    continue;
                }
                addWordInternal(definition.word(), definition.category(), normalizeLevel(definition.level()));
                totalLoaded++;
            }
        }
        log.info("数据库词库加载完成，共 {} 个词，provider 数量 {}", totalLoaded, sensitiveWordProviders.size());
    }

    private void addWordInternal(String word, String category, int level) {
        String normalizedWord = normalizeWord(word);
        String normalizedCategory = normalizeCategoryCode(category);
        if (normalizedWord == null || normalizedCategory == null) {
            return;
        }

        Map<Character, Node> current = wordTree;
        Node currentNode = null;
        for (char c : normalizedWord.toCharArray()) {
            currentNode = current.computeIfAbsent(c, key -> new Node());
            current = currentNode.next;
        }
        if (currentNode != null) {
            currentNode.isEnd = true;
            currentNode.category = normalizedCategory;
            currentNode.level = level;
        }
        maxWordLength = Math.max(maxWordLength, normalizedWord.length());
        categoryWords.computeIfAbsent(normalizedCategory, key -> new HashSet<>()).add(normalizedWord);
    }

    private int getTotalWordCount() {
        return categoryWords.values().stream().mapToInt(Set::size).sum();
    }

    private DynamicWordEntry parseDynamicWordEntry(String word, String meta) {
        String[] parts = meta.split("\\|");
        String category = normalizeCategoryCode(parts[0]);
        int level = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 3;
        return new DynamicWordEntry(word, category, normalizeLevel(level));
    }

    private void ensureDynamicWordManagementAvailable() {
        if (!properties.getInputContent().getWordLibrary().isDynamicEnabled()) {
            throw new IllegalStateException("当前未开启 Redis 动态词库");
        }
        if (redisTemplate == null) {
            throw new IllegalStateException("当前未配置 Redis，无法管理动态词库");
        }
    }

    private String normalizeWord(String word) {
        if (word == null) {
            return null;
        }
        String normalizedWord = word.trim();
        return normalizedWord.isEmpty() ? null : normalizedWord;
    }

    private String normalizeCategoryCode(String categoryCode) {
        if (categoryCode == null) {
            return null;
        }
        String normalized = categoryCode.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private int normalizeLevel(int level) {
        return Math.max(1, Math.min(level, 5));
    }

    private String resolveCategoryCodeFromFilename(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            return null;
        }
        String baseName = filename.substring(0, filename.lastIndexOf('.'));
        return normalizeCategoryCode(baseName);
    }
}
