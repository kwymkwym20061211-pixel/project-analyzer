#define _XOPEN_SOURCE 500
#include <ctype.h>
#include <fcntl.h>
#include <ftw.h>
#include <stdbool.h>
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
        if (c == '\n') {
            // 条件を満たした場合のみ行数カウント
            if (!is_empty_line) {
                line_count++;
            }
            // 無差別に状態更新はする
            has_comment_begun = false;
            is_empty_line = true;
            continue;
        }
        // コメント内にいない時で、空白系以外を発見したら空行でないとみなす。
        if (!isspace(c) && !is_in_block_comment && !has_comment_begun) {
            is_empty_line = false;
            continue;
        }
    }
    // ループ終了後に「まだ有効な行がある」場合は加算する
    if (!is_empty_line) {
        line_count++;
    }
    return line_count;
}

// -------------------------------- XML/HTML風 -----------------------------------------
/**
 * XML, HTMLファイルの行数をカウントする関数
 * ブロックコメント（）を考慮。行コメントは無し。
 */
int count_xml_like_file(const unsigned char *text, size_t text_len) {
    int line_count = 0;
    bool is_empty_line = true;
    bool is_in_block_comment = false;

    for (size_t i = 0; i < text_len; i++) {
        unsigned char c = text[i];

        // 1. ブロックコメントの終了判定
        if (is_in_block_comment) {
            if (i + 2 < text_len && c == '-' && text[i + 1] == '-' && text[i + 2] == '>') {
                is_in_block_comment = false;
                i += 2;// 残りの2文字をスキップ
                continue;
            }
        }
        // 2. ブロックコメントの開始判定
        else {
            if (i + 3 < text_len && c == '<' && text[i + 1] == '!' && text[i + 2] == '-' && text[i + 3] == '-') {
                is_in_block_comment = true;
                i += 3;// 残りの3文字をスキップ
                continue;
            }
        }

        // 3. 改行の処理
        if (c == '\n') {
            if (!is_empty_line) {
                line_count++;
            }
            is_empty_line = true;
            continue;
        }

        // 4. 有効な文字の判定
        if (!isspace(c) && !is_in_block_comment) {
            is_empty_line = false;
        }
    }

    if (!is_empty_line) {
        line_count++;
    }
    return line_count;
}

// -------------------------------- Python風 -------------------------------------------
/**
 * Pythonファイルの行数をカウントする関数
 * 行コメント（#）と、Docstring（""" または '''）をブロックコメントとして考慮。
 */
int count_python_like_file(const unsigned char *text, size_t text_len) {
    int line_count = 0;
    bool is_empty_line = true;
    bool has_comment_begun = false;
    bool is_in_double_docstring = false;
    bool is_in_single_docstring = false;

    for (size_t i = 0; i < text_len; i++) {
        unsigned char c = text[i];

        // 1. Docstringの終了判定
        if (is_in_double_docstring) {
            if (i + 2 < text_len && c == '"' && text[i + 1] == '"' && text[i + 2] == '"') {
                is_in_double_docstring = false;
                i += 2;
                continue;
            }
        } else if (is_in_single_docstring) {
            if (i + 2 < text_len && c == '\'' && text[i + 1] == '\'' && text[i + 2] == '\'') {
                is_in_single_docstring = false;
                i += 2;
                continue;
            }
        }
        // 2. コメント/Docstringの開始判定 (行コメントが始まっていない場合のみ)
        else if (!has_comment_begun) {
            if (c == '#') {
                has_comment_begun = true;
                continue;// 1文字なので i は増やさず continue
            } else if (i + 2 < text_len && c == '"' && text[i + 1] == '"' && text[i + 2] == '"') {
                is_in_double_docstring = true;
                i += 2;
                continue;
            } else if (i + 2 < text_len && c == '\'' && text[i + 1] == '\'' && text[i + 2] == '\'') {
                is_in_single_docstring = true;
                i += 2;
                continue;
            }
        }

        // 3. 改行の処理
        if (c == '\n') {
            if (!is_empty_line) {
                line_count++;
            }
            has_comment_begun = false;
            is_empty_line = true;
            continue;
        }

        // 4. 有効な文字の判定
        if (!isspace(c) && !has_comment_begun && !is_in_double_docstring && !is_in_single_docstring) {
            is_empty_line = false;
        }
    }

    if (!is_empty_line) {
        line_count++;
    }
    return line_count;
}

// -------------------------------- Script風 -------------------------------------------
/**
 * Shellスクリプト, YAML, Rubyなどの行数をカウントする関数
 * 行コメント（#）のみ考慮。
 */
int count_script_like_file(const unsigned char *text, size_t text_len) {
    int line_count = 0;
    bool is_empty_line = true;
    bool has_comment_begun = false;

    for (size_t i = 0; i < text_len; i++) {
        unsigned char c = text[i];

        // 1. コメント開始判定
        if (!has_comment_begun && c == '#') {
            has_comment_begun = true;
            continue;
        }

        // 2. 改行の処理
        if (c == '\n') {
            if (!is_empty_line) {
                line_count++;
            }
            has_comment_begun = false;
            is_empty_line = true;
            continue;
        }

        // 3. 有効な文字の判定
        if (!isspace(c) && !has_comment_begun) {
            is_empty_line = false;
        }
    }

    if (!is_empty_line) {
        line_count++;
    }
    return line_count;
}

// ----------------------------- ハンドラ -------------------------------------

static const sloc_handler_t handlers[] = {
    // C言語スタイル
    {".c", count_c_like_file},
    {".h", count_c_like_file},
    {".cpp", count_c_like_file},
    {".hpp", count_c_like_file},
    {".go", count_c_like_file},
    {".rs", count_c_like_file},
    {".java", count_c_like_file},
    {".js", count_c_like_file},
    {".ts", count_c_like_file},
    // XML/HTMLスタイル
    {".xml", count_xml_like_file},
    {".html", count_xml_like_file},
    {".htm", count_xml_like_file},
    // Pythonスタイル
    {".py", count_python_like_file},
    // Scriptスタイル
    {".sh", count_script_like_file},
    {".yaml", count_script_like_file},
    {".yml", count_script_like_file},
    {".rb", count_script_like_file},// RubyもScriptスタイルでカウントすることにする
};

// ---------------------------------------------------------------------------------

// nftwのコールバックで使うための内部コンテキスト
typedef struct {
    sloc_count_result_t *result;
    unsigned char *text_buf;
    size_t text_buf_size;
} walk_context_t;

// グローバル（または静的）にコンテキストを保持してコールバックに渡す
static walk_context_t g_ctx;
// 拡張子から直接ハンドラを検索する関数に変更
static int (*get_handler_by_ext(const char *ext))(const unsigned char *, size_t) {
    for (size_t i = 0; i < sizeof(handlers) / sizeof(handlers[0]); i++) {
        if (strcmp(ext, handlers[i].ext) == 0) return handlers[i].file_reader;
    }
    return NULL;
}

static int process_file(const char *fpath, const struct stat *sb, int typeflag, struct FTW *ftwbuf) {
    if (typeflag != FTW_F) return 0;

    // 1. 拡張子を一度だけ取得
    const char *ext = strrchr(fpath, '.');
    if (!ext) return 0;

    // 2. 取得した拡張子を使ってハンドラを特定
    int (*reader)(const unsigned char *, size_t) = get_handler_by_ext(ext);
    if (!reader) return 0;

    // ファイル読み込みと解析
    int fd = open(fpath, O_RDONLY);
    if (fd < 0) return 0;

    // テキスト走査用バッファの確保
    size_t len = sb->st_size;
    if (len > g_ctx.text_buf_size) {
        unsigned char *new_buf = realloc(g_ctx.text_buf, len);
        if (!new_buf) {
            close(fd);
            return 0;
        }
        g_ctx.text_buf = new_buf;
        g_ctx.text_buf_size = len;
    }
    if (!g_ctx.text_buf) {
        close(fd);
        return 0;
    }

    // バッファをゼロ埋め
    memset(g_ctx.text_buf, 0, g_ctx.text_buf_size);

    // ファイルを読み込んで行数をカウント
    if (read(fd, g_ctx.text_buf, len) == (ssize_t) len) {
        int lines = reader(g_ctx.text_buf, len);

        // 3. 同じ ext を使って加算先を特定
        for (size_t i = 0; i < g_ctx.result->sloc_count; i++) {
            if (strcmp(ext, g_ctx.result->slocs[i].extension) == 0) {
                g_ctx.result->slocs[i].count += lines;
                break;
            }
        }
    }

    close(fd);
    return 0;
}

int count_sloc(sloc_count_result_t *result_buf, const char *project_path) {
    if (!result_buf || !project_path) return -1;

    // 1. handlers から拡張子の数を取得
    size_t num_exts = sizeof(handlers) / sizeof(sloc_handler_t);

    // 2. 結果用のメモリを確保
    ext_sloc_count_t *slocs = calloc(num_exts, sizeof(ext_sloc_count_t));
    if (!slocs) return -1;

    // 3. handlers の定義を元に初期化
    for (size_t i = 0; i < num_exts; i++) {
        slocs[i].extension = handlers[i].ext;
        slocs[i].count = 0;
    }

    result_buf->slocs = slocs;
    result_buf->sloc_count = num_exts;

    g_ctx.result = result_buf;

    // nftw で走査
    if (nftw(project_path, process_file, 64, FTW_PHYS) == -1) {
        free(slocs);
        return -1;
    }

    // 終了処理
    free(g_ctx.text_buf);
    g_ctx.text_buf = NULL;
    g_ctx.text_buf_size = 0;

    return 0;
}