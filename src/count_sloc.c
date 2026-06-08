// -------------------------------- C言語風 -------------------------------------------

/**
  * C言語風のファイルの行数をカウントする関数
 */
int count_c_like_file(const unsigned char *text, size_t text_len) {
    // コメント範囲を無視する+空でない行数を数えるの複合。
    // コメント範囲のテキストを何もないものとみなして、行数を数える。
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
        } else if (c != ' ' && !has_comment_begun) {
            is_empty_line = false;
        }
    }
    return line_count;
}

static sloc_counter_t c_like_sloc_counter = {
    .file_reader = count_c_like_file};

// ---------------------------------------------------------------------------------

/**
  * プロジェクトのソースコード行数をカウントする関数
 */
int count_sloc(const char *project_path) {

    return 0;// 仮の戻り値
}