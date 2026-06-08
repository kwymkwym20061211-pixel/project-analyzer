
typedef struct {
    /** ファイルを読んで行数をカウントする関数。エラーなら-1を返す */
    int (*file_reader)(const unsigned char *text);
} sloc_counter_t;

/**
  * プロジェクトのソースコード行数をカウントする関数
 */
int count_sloc(const char *project_path);