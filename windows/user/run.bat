@echo off
setlocal
chcp 65001 > nul

rem --- 自分の場所(user/)を基準に、一つ上のプロジェクトルートを特定 ---
set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

rem クラスパスはルート直下の compiled フォルダを指定
set "BIN_DIR=generated"

echo [INFO] Booting the application...
echo Project Root: %CD%
echo ----------------------------------------
echo.

rem 実行（generated フォルダをクラスパスに含めて src.Main を起動）
java -cp "%BIN_DIR%" src.Main

echo.
echo ----------------------------------------
echo Finished. Enter to quit.
pause > nul

endlocal