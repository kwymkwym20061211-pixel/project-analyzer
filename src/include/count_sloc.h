#ifndef COUNT_SLOC_H
#define COUNT_SLOC_H
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
    size_t sloc_count;
} sloc_count_result_t;

/**
  * プロジェクトのソースコード行数をカウントする関数
 */
int count_sloc(sloc_count_result_t *result_buf, const char *project_path);

#endif// COUNT_SLOC_H