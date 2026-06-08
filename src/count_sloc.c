#define _XOPEN_SOURCE 500
#include <ctype.h>
#include <fcntl.h>
#include <ftw.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

#include "./include/count_sloc.h"

// 拡張子と解析関数のペア
typedef struct {
    const char *ext;
    int (*file_reader)(const unsigned char *, size_t);
} sloc_handler_t;

// -------------------------------- C言語風 -------------------------------------------

/**
  * C言語風のファイルの行数をカウントする関数
 */
int count_c_like_file(const unsigned char *text, size_t text_len) {
    /*
    * 行コメント（//）とブロックコメント（/* ... * /）を考慮して、空行やコメント行を除外して行数をカウントする。
    * 行コメントは、そこより前に
    */
    int line_count = 0;
    bool is_empty_line = true;
    bool has_comment_begun = false;
    bool is_in_block_comment = false;
    for (size_t i = 0; i < text_len; i++) {
        unsigned char c = text[i];
        // まだ最後の文字でない場合は次の文字も見て、コメント開始/終了を判定する。
        if (i + 1 < text_len) {
            unsigned char next_c = text[i + 1];
            if (!is_in_block_comment && c == '/' && next_c == '/') {
                // 行コメント開始
                has_comment_begun = true;
                i++;// 次の文字もスキップ
                continue;
            } else if (!is_in_block_comment && c == '/' && next_c == '*') {
                // ブロックコメント開始
                is_in_block_comment = true;
                i++;// 次の文字もスキップ
                continue;
            } else if (is_in_block_comment && c == '*' && next_c == '/') {
                // ブロックコメント終了
                is_in_block_comment = false;
                i++;// 次の文字もスキップ
                continue;
            }
        }
        // コメント行やブロックコメント内の行は空行とみなす。
        if (c == '\n' && !is_empty_line && (!has_comment_begun || !is_in_block_comment)) {
            has_comment_begun = false;
            is_empty_line = true;
            line_count++;
            continue;
        }
        // コメント内にいない時で、空白系以外を発見したら空行でないとみなす。
        if (!isspace(c) && !is_in_block_comment && !has_comment_begun) {
            is_empty_line = false;
            continue;
        }
    }
    return line_count;
}

static const sloc_handler_t handlers[] = {
    {".c", count_c_like_file},
    {".h", count_c_like_file},
    {".cpp", count_c_like_file},
    {".hpp", count_c_like_file},
    {".java", count_c_like_file},
    {".js", count_c_like_file},
    {".ts", count_c_like_file},
};

// ---------------------------------------------------------------------------------

// 拡張子から適切なハンドラを探す
static int (*get_handler(const char *path))(const unsigned char *, size_t) {
    const char *ext = strrchr(path, '.');
    if (!ext) return NULL;
    for (size_t i = 0; i < sizeof(handlers) / sizeof(handlers[0]); i++) {
        if (strcmp(ext, handlers[i].ext) == 0) return handlers[i].file_reader;
    }
    return NULL;
}

// nftwのコールバックで使うための内部コンテキスト
typedef struct {
    sloc_count_result_t *result;
} walk_context_t;

// グローバル（または静的）にコンテキストを保持してコールバックに渡す
static walk_context_t g_ctx;

static int process_file(const char *fpath, const struct stat *sb, int typeflag, struct FTW *ftwbuf) {
    if (typeflag != FTW_F) return 0;

    int (*reader)(const unsigned char *, size_t) = get_handler(fpath);
    if (!reader) {
        return 0;
    }

    // ファイル読み込みと解析
    int fd = open(fpath, O_RDONLY);
    if (fd < 0) return 0;

    size_t len = sb->st_size;
    unsigned char *buf = malloc(len);
    if (buf) {
        if (read(fd, buf, len) == (ssize_t) len) {
            int lines = reader(buf, len);

            // 結果を result_buf に加算
            const char *ext = strrchr(fpath, '.');
            for (size_t i = 0; i < g_ctx.result->sloc_count; i++) {
                if (strcmp(ext, g_ctx.result->slocs[i].extension) == 0) {
                    g_ctx.result->slocs[i].count += lines;
                    break;
                }
            }
        }
        free(buf);
    }
    close(fd);
    return 0;
}

int count_sloc(sloc_count_result_t *result_buf, const char *project_path) {
    if (!result_buf || !project_path) return -1;

    // コンテキストの初期化
    g_ctx.result = result_buf;

    // nftw で走査開始
    if (nftw(project_path, process_file, 64, FTW_PHYS) == -1) {
        perror("nftw");
        return -1;
    }
    return 0;
}