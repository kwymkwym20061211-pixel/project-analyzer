@echo off
setlocal
chcp 65001 > nul

rem プロジェクトのルートディレクトリに移動
cd /d "C:\Users\admin\dev\projects\ProjectAnalyzer"

echo ========================================
echo [1/2] ビルド環境をクリーンアップ中...
rem 古いクラスファイルを一度削除して、ディレクトリを再作成します
if exist "compiled" rd /s /q "compiled"
mkdir "compiled"

echo [2/2] 全サブディレクトリを含めて一括コンパイル中...
rem src 以下のすべての .java ファイルを再帰的にリストアップ
dir /s /b src\*.java > sources.txt

rem リストを元に一括コンパイル（出力先は compiled フォルダ）
javac -d "compiled" -encoding UTF-8 @sources.txt

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] 全てのソースコードのコンパイルが成功しました。
) else (
    echo.
    echo [ERROR] コンパイル中にエラーが発生しました。
)

rem 一時ファイルを削除して終了
if exist "sources.txt" del sources.txt

echo ========================================
echo Enterキーを押すと終了します。
pause > nul
endlocal