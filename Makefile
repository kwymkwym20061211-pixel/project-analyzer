# コンパイラとフラグの設定
CC := gcc
CFLAGS := -std=c17 -Wall -Wextra -Wpedantic

# ディレクトリ定義
SRC_DIR := src
OBJ_DIR := compiled
TARGET := app # 実行ファイル名

# ソースとオブジェクトの定義
SRCS := $(shell find $(SRC_DIR) -name "*.c")
OBJS := $(SRCS:$(SRC_DIR)/%.c=$(OBJ_DIR)/%.o)
DIRS := $(sort $(dir $(OBJS)))

.PHONY: all clean

# 実行ファイルを生成するように変更
all: $(TARGET)

# リンクして実行ファイルを生成
$(TARGET): $(OBJS)
	$(CC) $(OBJS) -o $(TARGET)

# オブジェクトファイルの生成ルール
$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c | $(DIRS)
	$(CC) $(CFLAGS) -c $< -o $@

# ディレクトリ作成
$(DIRS):
	mkdir -p $@

# クリーンアップ
clean:
	rm -rf $(OBJ_DIR) $(TARGET)