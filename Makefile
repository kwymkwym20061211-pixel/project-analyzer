# コンパイラとフラグの設定
CC := gcc
CFLAGS := -std=c17 -Wall -Wextra -Wpedantic

# ディレクトリ定義
SRC_DIR := src
BUILD_DIR := compiled
# 実行ファイルのパス
TARGET := $(BUILD_DIR)/main

# ソースとオブジェクトの定義
SRCS := $(shell find $(SRC_DIR) -name "*.c")
# オブジェクトファイルは compiled/obj/ に出すと整理しやすいです
OBJS := $(SRCS:$(SRC_DIR)/%.c=$(BUILD_DIR)/obj/%.o)
DIRS := $(sort $(dir $(OBJS)))

.PHONY: all clean

# 実行ファイルを生成するように変更
all: $(TARGET)

# リンクして実行ファイルを生成
$(TARGET): $(OBJS)
	$(CC) $(OBJS) -o $(TARGET)

# オブジェクトファイルの生成ルール
$(BUILD_DIR)/obj/%.o: $(SRC_DIR)/%.c | $(DIRS)
	$(CC) $(CFLAGS) -c $< -o $@

# ディレクトリ作成
$(DIRS):
	mkdir -p $@

# クリーンアップ
clean:
	rm -rf $(BUILD_DIR)