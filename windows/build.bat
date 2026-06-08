@echo off
setlocal
chcp 65001 > nul

set "PROJECT_ROOT=%~dp0"
cd /d "%PROJECT_ROOT%"

echo ========================================
echo Project Root: %CD%
echo ========================================

echo [1/2] ビルド環境をクリーンアップ中...
if exist "generated" rd /s /q "generated"
mkdir "generated"

echo [2/2] 全サブディレクトリを含めて一括コンパイル中...
dir /s /b src\*.java > sources.txt
javac -d "generated" -encoding UTF-8 @sources.txt

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] コンパイル成功。
    
    if not exist "generated\configs" mkdir "generated\configs"
    
    rem 改行や余計な空白を入れずに、現在のディレクトリ(%CD%)を直接書き込む
    <nul set /p="%CD%" > "generated\configs\root_path.txt"
    
    echo [INFO] Path saved: %CD%
)

if exist "sources.txt" del sources.txt
echo ========================================
echo Enterキーを押すと終了します。
pause > nul
endlocal