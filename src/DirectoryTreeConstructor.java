package src;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ディレクトリ構造をツリー形式の文字列で構築するクラス。
 * 除外リストに基づき、不要なフォルダをフィルタリングします。
 */
public class DirectoryTreeConstructor {

    /**
     * ツリー構成の設定
     */
    public static class TreeRequest {
        public final String rootPath;
        public final List<String> excludeDirs;

        public TreeRequest(String rootPath, List<String> excludeDirs) {
            this.rootPath = rootPath;
            this.excludeDirs = excludeDirs;
        }
    }

    /**
     * 【純関数】指定されたパスからディレクトリツリーを生成します。
     * 
     * @param request ルートパスと除外リスト
     * @return ツリー形式の文字列
     */
    public static String construct(TreeRequest request) {
        File root = new File(request.rootPath);
        if (!root.exists() || !root.isDirectory()) {
            return "エラー: 無効なディレクトリパスです。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(root.getName()).append(" (").append(root.getAbsolutePath()).append(")\n");

        // 再帰的にツリーを構築
        buildTree(root, "", request.excludeDirs, sb);

        return sb.toString();
    }

    /**
     * 再帰的にディレクトリを走査し、StringBuilderにツリー図を蓄積します。
     * * @param dir 現在のディレクトリ
     * 
     * @param indent   現在の階層のインデント文字列
     * @param excludes 除外ディレクトリ名のリスト
     * @param sb       結果を格納するStringBuilder
     */
    private static void buildTree(File dir, String indent, List<String> excludes, StringBuilder sb) {
        File[] children = dir.listFiles();
        if (children == null)
            return;

        // 除外対象をフィルタリングし、名前順にソート
        List<File> filteredList = new ArrayList<>();
        for (File child : children) {
            if (!excludes.contains(child.getName())) {
                filteredList.add(child);
            }
        }
        Collections.sort(filteredList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        for (int i = 0; i < filteredList.size(); i++) {
            File file = filteredList.get(i);
            boolean isLast = (i == filteredList.size() - 1);

            final boolean isDirectory = file.isDirectory();

            // 枝の記号を決定
            String branch = isLast ? "└── " : "├── ";
            sb.append(indent).append(branch).append(file.getName());
            if (isDirectory) {
                sb.append("/");
            }
            sb.append("\n");

            // ディレクトリの場合はさらに深く潜る
            if (isDirectory) {
                // 次の階層のインデントを決定
                String nextIndent = indent + (isLast ? "    " : "│   ");
                buildTree(file, nextIndent, excludes, sb);
            }
        }
    }
}