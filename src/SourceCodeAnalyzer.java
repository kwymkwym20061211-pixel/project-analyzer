package src;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ソースコード行数集計エンジン (Pure Logic Version)
 * * 特定のディレクトリを走査し、コメントや空行を除外した有効行数を集計します。
 * 標準出力に依存せず、結果を String として返す純粋な解析ロジックを提供します。
 */
public class SourceCodeAnalyzer {

    // --- 設定用データ構造 ---

    /**
     * 解析リクエスト設定
     */
    public static class AnalysisRequest {
        public final String rootPath;          // 走査対象のルートパス
        public final List<String> excludeDirs; // 除外するディレクトリ名（.git, build 等）
        public final List<SumGroupConfig> sumConfigs; // 合算集計の定義

        public AnalysisRequest(String rootPath, List<String> excludeDirs, List<SumGroupConfig> sumConfigs) {
            this.rootPath = rootPath;
            this.excludeDirs = excludeDirs;
            this.sumConfigs = sumConfigs;
        }
    }

    /**
     * 合算グループの設定
     */
    public static class SumGroupConfig {
        public final String label;
        public final Set<String> extensions;

        public SumGroupConfig(String label, String... exts) {
            this.label = label;
            this.extensions = new HashSet<>(Arrays.asList(exts));
        }
    }

    // --- メイン・エントリポイント (純関数的アプローチ) ---

    /**
     * 指定された設定に基づき、解析レポートを生成します。
     * * @param request 解析対象のパスと設定
     * @return 解析結果を整形した文字列
     */
    public static String generateReport(AnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        
        try {
            // 1. ファイルの収集（除外設定の適用）
            List<File> allFiles = collectFiles(request, sb);

            // 2. 拡張子別の有効行数集計
            Map<String, Integer> extensionCounts = countLinesByExtension(allFiles);

            // 3. 設定に基づいたグループ合算
            Map<String, Integer> groupTotals = calculateGroupTotals(extensionCounts, request.sumConfigs);

            // 4. ファイル単体の行数ランキング作成
            List<FileRank> ranking = calculateRanking(allFiles);

            // 5. 結果レポートの構築
            buildFinalReport(sb, extensionCounts, groupTotals, ranking);

        } catch (IOException e) {
            sb.append("\n[ERROR] ファイル走査中に致命的なエラーが発生しました:\n");
            sb.append(e.getMessage());
        }

        return sb.toString();
    }

    // --- 内部ロジック ---

    /**
     * ディレクトリを再帰的に走査し、ファイルリストを作成します。
     */
    private static List<File> collectFiles(AnalysisRequest request, StringBuilder log) throws IOException {
        Path root = Paths.get(request.rootPath);
        Set<String> skippedDirs = new TreeSet<>();

        List<File> files = Files.walk(root)
                .filter(path -> {
                    // 各階層のディレクトリ名が除外リストに含まれているかチェック
                    for (Path part : path) {
                        String name = part.getFileName() != null ? part.getFileName().toString() : "";
                        if (request.excludeDirs.contains(name)) {
                            skippedDirs.add(name);
                            return false;
                        }
                    }
                    return Files.isRegularFile(path);
                })
                .map(Path::toFile)
                .collect(Collectors.toList());

        if (!skippedDirs.isEmpty()) {
            log.append("[INFO] スキップされたディレクトリ:\n");
            skippedDirs.forEach(dir -> log.append(" - ").append(dir).append("\n"));
        }
        return files;
    }

    /**
     * 拡張子ごとに有効行数をカウントします。
     */
    private static Map<String, Integer> countLinesByExtension(List<File> files) {
        Map<String, Integer> summary = new TreeMap<>();
        Map<String, SrcLinesCounter> counters = getCounterMap();

        for (File file : files) {
            String ext = getExtension(file.getName());
            if (counters.containsKey(ext)) {
                int lines = counters.get(ext).countLines(file);
                summary.put(ext, summary.getOrDefault(ext, 0) + lines);
            }
        }
        return summary;
    }

    /**
     * 指定されたグループ定義に基づき、行数を合算します。
     */
    private static Map<String, Integer> calculateGroupTotals(Map<String, Integer> counts, List<SumGroupConfig> configs) {
        Map<String, Integer> results = new LinkedHashMap<>();
        for (SumGroupConfig group : configs) {
            int total = counts.entrySet().stream()
                    .filter(e -> group.extensions.contains(e.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
            results.put(group.label, total);
        }
        return results;
    }

    /**
     * 全ファイルを行数順に並び替えます。
     */
    private static List<FileRank> calculateRanking(List<File> files) {
        Map<String, SrcLinesCounter> counters = getCounterMap();
        List<FileRank> ranks = new ArrayList<>();

        for (File file : files) {
            String ext = getExtension(file.getName());
            if (counters.containsKey(ext)) {
                int lines = counters.get(ext).countLines(file);
                if (lines > 0) {
                    ranks.add(new FileRank(file.getAbsolutePath(), lines));
                }
            }
        }
        ranks.sort((a, b) -> Integer.compare(b.lines, a.lines));
        return ranks;
    }

    /**
     * 集計データを人間が読みやすい形式に整形します。
     */
    private static void buildFinalReport(StringBuilder sb, Map<String, Integer> counts, 
                                        Map<String, Integer> groupTotals, List<FileRank> ranking) {
        
        sb.append("\n===== 1. 拡張子別集計 (有効行数) =====\n");
        counts.forEach((ext, lines) -> 
            sb.append(String.format("%-10s: %6d lines%n", ext, lines)));

        sb.append("\n===== 2. グループ合算集計 =====\n");
        groupTotals.forEach((label, total) -> 
            sb.append(String.format("%-30s: %d lines%n", label, total)));

        sb.append("\n===== 3. 行数ランキング (Top 15) =====\n");
        int limit = 15;
        for (int i = 0; i < Math.min(ranking.size(), limit); i++) {
            FileRank rank = ranking.get(i);
            sb.append(String.format("%2d位: %6d lines - %s%n", (i + 1), rank.lines, rank.path));
        }
    }

    // --- 補助メソッド ---

    private static String getExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return (dotIdx == -1) ? "" : fileName.substring(dotIdx).toLowerCase();
    }

    private static Map<String, SrcLinesCounter> getCounterMap() {
        Map<String, SrcLinesCounter> map = new HashMap<>();
        SrcLinesCounter cStyle = new CStyleCounter();
        SrcLinesCounter xmlStyle = new XmlStyleCounter();
        SrcLinesCounter jsonStyle = new JsonStyleCounter();

        // 各言語のコメント形式に合わせてカウンターを割り当て
        String[] cExts = {".java", ".js", ".ts", ".cpp", ".c", ".h", ".hpp", ".kt",".gs"};
        for (String e : cExts) map.put(e, cStyle);
        map.put(".xml", xmlStyle);
        map.put(".json", jsonStyle);
        return map;
    }

    // --- 内部インターフェース・クラス ---

    /**
     * 行数カウントの戦略インターフェース
     */
    private interface SrcLinesCounter {
        int countLines(File targetFile);
    }

    private static class FileRank {
        final String path;
        final int lines;
        FileRank(String path, int lines) { this.path = path; this.lines = lines; }
    }

    /**
     * Cスタイル (Java, JS, C, C++, Kotlin etc.) 用カウンター
     * ブロックコメント、行コメント、文字列リテラルを考慮
     */
    private static class CStyleCounter implements SrcLinesCounter {
        @Override
        public int countLines(File targetFile) {
            int count = 0;
            boolean inBlockComment = false;

            try (BufferedReader br = new BufferedReader(new FileReader(targetFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    boolean hasCode = false;
                    boolean inString = false;

                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        char next = (i + 1 < line.length()) ? line.charAt(i + 1) : '\0';

                        if (inBlockComment) {
                            if (c == '*' && next == '/') {
                                inBlockComment = false;
                                i++;
                            }
                        } else if (inString) {
                            if (c == '"' && line.charAt(Math.max(0, i - 1)) != '\\') {
                                inString = false;
                                hasCode = true;
                            }
                        } else {
                            if (c == '/' && next == '/') break; // 行末までコメント
                            if (c == '/' && next == '*') {
                                inBlockComment = true;
                                i++;
                            } else if (c == '"') {
                                inString = true;
                                hasCode = true;
                            } else if (!Character.isWhitespace(c)) {
                                hasCode = true;
                            }
                        }
                    }
                    if (hasCode && !inBlockComment) count++;
                }
            } catch (IOException e) { return 0; }
            return count;
        }
    }

    /**
     * XML/HTMLスタイル用カウンター
     */
    private static class XmlStyleCounter implements SrcLinesCounter {
        @Override
        public int countLines(File targetFile) {
            int count = 0;
            boolean inComment = false;
            try (BufferedReader br = new BufferedReader(new FileReader(targetFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (inComment) {
                        if (line.contains("-->")) {
                            inComment = false;
                        }
                        continue; // コメント内なら次の行へ
                    }

                    // コメント開始判定
                    if (line.startsWith("<!--")) {
                            inComment = true;
                            continue; // コメント行自体はカウントしない
                        }
                    // 有効な行（XML宣言、タグ等）をカウント
                    count++;
                }
                }catch (IOException e) { return 0; }
            return count;
        }
    }

    /**
     * JSON/プレーンテキスト用カウンター (空行のみ除外)
     */
    private static class JsonStyleCounter implements SrcLinesCounter {
        @Override
        public int countLines(File targetFile) {
            int count = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(targetFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) count++;
                }
            } catch (IOException e) { return 0; }
            return count;
        }
    }
}