#include <limits.h>
#include <stdio.h>
#include <unistd.h>

typedef struct {
    unsigned char project_path[4096];
    bool build_tree;
    bool count_lines;
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
        printf("  --help       Show this help message and exit\n");
        printf("  --tree       Build a tree structure of the project\n");
        printf("  --count      Count the number of lines in the project\n");
        exit(0);
    } else if (strcmp(arg, "--all") == 0 || strcmp(arg, "-a") == 0) {
        args->build_tree = true;
        args->count_lines = true;
    } else if (strcmp(arg, "--tree") == 0) {
        args->build_tree = true;
    } else if (strcmp(arg, "--count") == 0) {
        args->count_lines = true;
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

    return 0;
}