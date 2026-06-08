#include <limits.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "./include/count_sloc.h"

typedef struct {
    unsigned char project_path[4096];
    bool build_tree;
    bool count_sloc;
} main_args_t;

/**
 * 引数を解析する。
 */
static inline void reg_arg(main_args_t *args, const char *arg) {
    if (arg == NULL) {
        return;
    } else if (strcmp(arg, "--help") == 0) {
        printf("Usage: program [OPTIONS] [PROJECT_PATH]\n");
        printf("Options:\n");
        printf("  --all, -a    Build a tree structure and count lines in the project\n");
        printf("  --help       Show this help message and exit\n");
        printf("  --tree       Build a tree structure of the project\n");
        printf("  --sloc      Count the number of lines in the project\n");
        exit(0);
    } else if (strcmp(arg, "--all") == 0 || strcmp(arg, "-a") == 0) {
        args->build_tree = true;
        args->count_sloc = true;
    } else if (strcmp(arg, "--tree") == 0) {
        args->build_tree = true;
    } else if (strcmp(arg, "--sloc") == 0) {
        args->count_sloc = true;
    } else {
        // それ以外はプロジェクトパスとみなす。
        strncpy((char *) args->project_path, arg, sizeof(args->project_path) - 1);
        args->project_path[sizeof(args->project_path) - 1] = '\0';
    }
}

/**
 * main関数。
 */
int main(int argc, char *argv[]) {
    // 0. 引数が足りないならエラー。
    if (argc < 2) {
        printf("Arguments are missing.\n");
        return -1;
    }
    // 1. 引数解析
    main_args_t args = {0};
    for (int i = 1; i < argc; i++) {
        reg_arg(&args, argv[i]);
    }
    // 2. プロジェクトパスがない場合はカレントディレクトリを参照
    if (args.project_path[0] == '\0') {
        if (getcwd((char *) args.project_path, sizeof(args.project_path)) == NULL) {
            printf("Failed to get current working directory.\n");
            return -1;
        }
    }
    // 3. プロジェクトパスを表示
    printf("Project Path: %s\n", args.project_path);
    // 4. 設定に従って処理を実行
    if (args.count_sloc) {
        printf("Counting SLOC...\n");
        sloc_count_result_t result = {0};
        ext_sloc_count_t counts[] = {
            {".c", 0},
            {".h", 0},
            {".cpp", 0},
            {".hpp", 0},
            {".java", 0},
            {".js", 0},
            {".ts", 0}};
        result.slocs = counts;
        int sloc_rc = count_sloc(&result, (const char *) args.project_path);
        if (sloc_rc < 0) {
            printf("Failed to count SLOC.\n");
            return -1;
        }
        printf("SLOC Count:\n");
        int64_t total_sloc = 0;
        for (size_t i = 0; i < result.sloc_count; i++) {
            printf("  %s: %lld\n", result.slocs[i].extension, result.slocs[i].count);
            total_sloc += result.slocs[i].count;
        }
        printf("Total SLOC: %lld\n", total_sloc);
    }

    return 0;
}