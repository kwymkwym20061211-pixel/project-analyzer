
typedef struct {
    /** ファイルを読んで行数をカウントする関数。エラーなら-1を返す */
    int (*file_reader)(const unsigned char *text, size_t text_len);
} sloc_counter_t;

/**
 * 拡張子と行数の対応。
 */
typedef struct {
    const char *extension;
    int64_t count;
} ext_sloc_count_t;

/**
* count_slocの結果バッファ
 */
typedef struct {
    ext_sloc_count_t *slocs;
    size_t sloc_len;
} sloc_count_result_t;

/**
  * プロジェクトのソースコード行数をカウントする関数
 */
int count_sloc(sloc_count_result_t *result_buf, const char *project_path);