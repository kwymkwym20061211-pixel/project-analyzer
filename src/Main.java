package src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ソースコード解析・ディレクトリ構造可視化ツールのメインエントリポイント。
 * 設定ファイルからパス情報を読み込み、ユーザー対話形式で解析を実行します。
 */
public class Main {

    // プロジェクトの設定フォルダのパス。
    private static final String CONFIG_DIR = "C:/Users/admin/dev/projects/ProjectAnalyzer/configs";

    // 設定ファイルの配置パスこちらはプロジェクトルートのconfigフォルダ内にtargets.txtとexclusions.txtを配置する想定。
    private static final String TARGETS_FILE = CONFIG_DIR+"/targets.txt";
    private static final String EXCLUSIONS_FILE = CONFIG_DIR+"/exclusions.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // 1. 設定の読み込み
            List<String> targetPaths = loadFileLines(TARGETS_FILE);
            List<String> exclusions = loadFileLines(EXCLUSIONS_FILE);

            if (targetPaths.isEmpty()) {
                System.err.println("エラー: " + TARGETS_FILE + " に解析対象のパスが記述されていません。");
                return;
            }

            // 2. 除外リストの明示（自白）
            System.out.println("========================================");
            System.out.println("[INFO] 以下の要素を全操作で強制除外します:");
            if (exclusions.isEmpty()) {
                System.out.println(" - (なし)");
            } else {
                exclusions.forEach(e -> System.out.println(" - " + e));
            }
            System.out.println("========================================\n");

            // 3. 対象パスの選択ループ
            String selectedPath = null;
            while (selectedPath == null) {
                System.out.println("解析対象のディレクトリを選択してください:");
                for (int i = 0; i < targetPaths.size(); i++) {
                    System.out.printf("[%d] %s%n", i, targetPaths.get(i));
                }
                System.out.print("\n選択 (IDを入力) > ");

                String input = scanner.nextLine();
                try {
                    int index = Integer.parseInt(input);
                    if (index >= 0 && index < targetPaths.size()) {
                        selectedPath = targetPaths.get(index);
                    } else {
                        System.out.println("\n[!] エラー: 範囲外のIDです。もう一度入力してください。\n");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("\n[!] エラー: 数値を入力してください。\n");
                }
            }

            // 4. 解析の実行
            executeAnalysis(selectedPath, exclusions);

        } catch (IOException e) {
            System.err.println("致命的なI/Oエラーが発生しました: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    /**
     * 指定されたパスに対して、ツリー構造の出力と行数集計を実行します。
     */
    private static void executeAnalysis(String rootPath, List<String> exclusions) {
        System.out.println("\n--- 解析を開始します: " + rootPath + " ---\n");

        // A. ディレクトリツリーの構築
        DirectoryTreeConstructor.TreeRequest treeReq = 
            new DirectoryTreeConstructor.TreeRequest(rootPath, exclusions);
        String treeReport = DirectoryTreeConstructor.construct(treeReq);
        
        System.out.println("===== ディレクトリ構造 =====");
        System.out.println(treeReport);

        // B. ソースコード行数集計
        // 集計グループの定義（bœmさんの開発スタイルに合わせたプリセット）
        List<SourceCodeAnalyzer.SumGroupConfig> sumConfigs = Arrays.asList(
            new SourceCodeAnalyzer.SumGroupConfig("All Src Codes", ".java", ".kt", ".h", ".hpp", ".c", ".cpp", ".ts", ".xml"),
            new SourceCodeAnalyzer.SumGroupConfig("Logic Only (No XML)", ".java", ".kt", ".h", ".hpp", ".c", ".cpp", ".ts"),
            new SourceCodeAnalyzer.SumGroupConfig("C-Family Native", ".c", ".h", ".cpp", ".hpp")
        );

        SourceCodeAnalyzer.AnalysisRequest analysisReq = 
            new SourceCodeAnalyzer.AnalysisRequest(rootPath, exclusions, sumConfigs);
        String analysisReport = SourceCodeAnalyzer.generateReport(analysisReq);

        System.out.println(analysisReport);
        System.out.println("========================================");
        System.out.println("解析が完了しました。");
    }

    /**
     * ファイルから全行を読み込み、空行を除去してリストで返します。
     */
    private static List<String> loadFileLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            // ファイルが存在しない場合は空リストを返し、警告を表示
            System.out.println("[WARN] 設定ファイルが見つかりません: " + filePath);
            return Collections.emptyList();
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }
}