#include <stdio.h>
#include <unistd.h>
#include <limits.h>

typedef struct{
    unsigned char project_path[4096];
    bool build_tree;
}main_args_t;

/**
 * 引数を解析する。
 */
static inline void reg_arg(main_args_t *args, const char *arg) {
    if(strcmp(arg, "--tree") == 0) {
        args->build_tree = true;
    }else {
        // それ以外はプロジェクトパスとみなす。
        strncpy((char*)args->project_path, arg, sizeof(args->project_path) - 1);
        args->project_path[sizeof(args->project_path) - 1] = '\0';
    }
}


/**
 * main関数。
 */
int main(int argc, char *argv[]) {
    // 0. 引数が足りないならエラー。
    if(argc < 2) {
        printf("Arguments are missing.\n");
        return -1;
    }
    // 1. 引数解析
    main_args_t args = {0};
    for(int i = 1; i < argc; i++) {
        reg_arg(&args, argv[i]);
    }

    return 0;
}