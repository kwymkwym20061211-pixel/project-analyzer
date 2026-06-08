package src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    // プロジェクトのルートディレクトリ
    private static final String PROJECT_ROOT;

    // ルートからの相対パスで設定ファイルを指定
    private static final String CONFIG_FILE;

    static {
        String root;
        try {
            // 1. Main.class が置かれている場所（URL）を取得
            // 2. それを URI に変換し、Path オブジェクトにする
            // 3. .class は generated/src/Main.class にある想定なので、3つ上がルート
            Path classPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            // generated/ フォルダにいるなら、その親がプロジェクトルート
            // (バッチの javac -d "generated" の設定に合わせる)
            Path projectRootPath = classPath.getParent();

            // もし classPath が "generated/" 自体を指しているなら、その親がルート
            root = projectRootPath.toAbsolutePath().toString().replace("\\", "/");

        } catch (Exception e) {
            // 万が一失敗した時の保険
            root = System.getProperty("user.dir").replace("\\", "/");
        }
        PROJECT_ROOT = root;
        CONFIG_FILE = PROJECT_ROOT + "/user/configs.json";
    }

    // プロジェクト情報を保持するインナークラス
    static class ProjectInfo {
        String name;
        String root;
        List<String> exclusions;

        ProjectInfo(String name, String root, List<String> exclusions) {
            this.name = name;
            this.root = root;
            this.exclusions = exclusions;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // 1. JSON設定の読み込み
            List<ProjectInfo> projects = loadProjectsFromJson(CONFIG_FILE);

            if (projects.isEmpty()) {
                System.err.println("Error: No project is defined.");
                return;
            }

            // 2. 対象プロジェクトの選択
            ProjectInfo selectedProject = null;
            while (selectedProject == null) {
                System.out.println("========================================");
                System.out.println("Choose Project to analyze:");
                for (int i = 0; i < projects.size(); i++) {
                    System.out.printf("[%d] %s (%s)%n", i, projects.get(i).name, projects.get(i).root);
                }
                System.out.print("\nChoose (Enter ID) > ");

                String input = scanner.nextLine();
                try {
                    int index = Integer.parseInt(input);
                    if (index >= 0 && index < projects.size()) {
                        selectedProject = projects.get(index);
                    } else {
                        System.out.println("\n[!] Error: Out of range.\n");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("\n[!] Error: Enter number.\n");
                }
            }

            // 3. 解析の実行（そのプロジェクト専用の除外リストを渡す）
            executeAnalysis(selectedProject);

        } catch (Exception e) {
            System.err.println("Fatal Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    /**
     * 言語の拡張子設定を管理する
     */
    public static enum LanguageGroup {
        JAVA("Java/Kotlin", ".java", ".kt"),
        NATIVE("C-Family Native", ".c", ".h", ".cpp", ".hpp"),
        WEB("Web Technologies", ".ts", ".js"),
        CONFIG("Config/Layouts", ".xml", ".json", ".yaml");

        public final String label;
        public final String[] extensions;

        LanguageGroup(String label, String... extensions) {
            this.label = label;
            this.extensions = extensions;
        }
    }

    private static String[] getAllExtensions(boolean excludeConfig) {
        return Arrays.stream(LanguageGroup.values())
                .filter(g -> !excludeConfig || g != LanguageGroup.CONFIG)
                .flatMap(g -> Arrays.stream(g.extensions))
                .toArray(String[]::new);
    }

    /**
     * プロジェクト個別の設定に基づいて解析を実行します。
     */
    private static void executeAnalysis(ProjectInfo project) {
        System.out.println("\n--- Analytics of : " + project.name + " ---");
        System.out.println("Root: " + project.root);
        System.out.println("Exclusions: " + String.join(", ", project.exclusions) + "\n");

        // A. ディレクトリツリーの構築
        DirectoryTreeConstructor.TreeRequest treeReq = new DirectoryTreeConstructor.TreeRequest(project.root,
                project.exclusions);
        String treeReport = DirectoryTreeConstructor.construct(treeReq);

        System.out.println("===== Directory Tree =====");
        System.out.println(treeReport);

        // B. ソースコード行数集計
        List<SourceCodeAnalyzer.SumGroupConfig> sumConfigs = new ArrayList<>();

        // 1. 全体統計 (Total)
        sumConfigs.add(new SourceCodeAnalyzer.SumGroupConfig("TOTAL (All Src)",
                getAllExtensions(false)));

        // 2. ロジック統計 (Logic Only - Config以外)
        sumConfigs.add(new SourceCodeAnalyzer.SumGroupConfig("LOGIC ONLY (No Config)",
                getAllExtensions(true)));

        // 3. 各グループごとの統計 (自動生成)
        for (LanguageGroup group : LanguageGroup.values()) {
            sumConfigs.add(new SourceCodeAnalyzer.SumGroupConfig("-> " + group.label,
                    group.extensions));
        }

        SourceCodeAnalyzer.AnalysisRequest analysisReq = new SourceCodeAnalyzer.AnalysisRequest(project.root,
                project.exclusions, sumConfigs);
        String analysisReport = SourceCodeAnalyzer.generateReport(analysisReq);

        System.out.println(analysisReport);
        System.out.println("========================================");
        System.out.println("End of analytics");
    }

    /**
     * JSONファイルを簡易解析してProjectInfoのリストを生成します。
     * ライブラリを使わないため、正規表現で「projects」オブジェクトの中身を抽出します。
     */
    private static List<ProjectInfo> loadProjectsFromJson(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        List<ProjectInfo> projectList = new ArrayList<>();

        // "projects": { ... } の中身を抽出
        Pattern projectsPattern = Pattern.compile("\"projects\"\\s*:\\s*\\{(.+)\\}", Pattern.DOTALL);
        Matcher matcher = projectsPattern.matcher(content);

        if (matcher.find()) {
            String projectsBody = matcher.group(1);

            // 各プロジェクトのブロックを分割（簡易的に "name": { ... } を探す）
            Pattern projectBlockPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]+)\\}", Pattern.DOTALL);
            Matcher blockMatcher = projectBlockPattern.matcher(projectsBody);

            while (blockMatcher.find()) {
                String projectName = blockMatcher.group(1);
                String details = blockMatcher.group(2);

                // root 抽出
                String root = "";
                Matcher rootMatcher = Pattern.compile("\"root\"\\s*:\\s*\"([^\"]+)\"").matcher(details);
                if (rootMatcher.find())
                    root = rootMatcher.group(1).replace("\\\\", "/");

                // exclusions 抽出
                List<String> exclusions = new ArrayList<>();
                Matcher exclMatcher = Pattern.compile("\"exclusions\"\\s*:\\s*\\[([^\\]]+)\\]").matcher(details);
                if (exclMatcher.find()) {
                    String[] items = exclMatcher.group(1).split(",");
                    for (String item : items) {
                        exclusions.add(item.replace("\"", "").trim());
                    }
                }
                projectList.add(new ProjectInfo(projectName, root, exclusions));
            }
        }
        return projectList;
    }
}