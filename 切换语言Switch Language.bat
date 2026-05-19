@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   Switch Language / 切换语言
echo   Cosmicon Dice
echo ============================================
echo/

cd /d "%~dp0"
if errorlevel 1 (echo [ERROR] Cannot access mod directory. & pause & exit /b 1)

set "DATA_DIR=data\config"

if not exist "%DATA_DIR%\strings.json" (
    echo [ERROR] %DATA_DIR%\strings.json not found.
    pause
    exit /b 1
)

cd "%DATA_DIR%"
if errorlevel 1 (echo [ERROR] Cannot access %DATA_DIR%. & pause & exit /b 1)

REM --- Clean up temp file from interrupted previous run ---
if exist "strings.tmp" (
    echo [WARN] Found leftover strings.tmp from a previous interrupted switch.
    echo        Restoring it as strings.json...
    move /y "strings.tmp" "strings.json" >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Could not restore strings.tmp. Delete it manually and retry.
        pause
        exit /b 1
    )
    echo   [OK] Recovered.
    echo/
)

REM --- Detect current language from backup file state ---
set "CURRENT="
set "TARGET_FILE="
set "BACKUP_FILE="

if exist "strings-EN.json" (
    if not exist "strings-CN.json" (
        set "CURRENT=CN"
        set "TARGET_FILE=strings-EN.json"
        set "BACKUP_FILE=strings-CN.json"
    )
)
if not defined CURRENT (
    if exist "strings-CN.json" (
        set "CURRENT=EN"
        set "TARGET_FILE=strings-CN.json"
        set "BACKUP_FILE=strings-EN.json"
    )
)

if not defined CURRENT (
    if exist "strings-EN.json" if exist "strings-CN.json" (
        echo [ERROR] Both strings-EN.json and strings-CN.json found.
        echo        Cannot determine current language.
        echo        Delete one backup file and retry.
        pause
        exit /b 1
    )
    echo [ERROR] No backup language file found ^(strings-EN.json or strings-CN.json^).
    echo        Place the alternate language file in %DATA_DIR% and retry.
    pause
    exit /b 1
)

if "%CURRENT%"=="EN" (
    set "CURRENT_NAME=English"
    set "TARGET_NAME=Chinese / 中文"
) else (
    set "CURRENT_NAME=Chinese / 中文"
    set "TARGET_NAME=English"
)

echo Current language / 当前语言: %CURRENT_NAME%
echo/
echo Switch to / 切换为: %TARGET_NAME%?
echo/
set /p CONFIRM= [Y/N]:
if /i "!CONFIRM!" neq "Y" (
    echo Cancelled / 已取消。
    pause
    exit /b
)

echo/
echo Switching... / 切换中...
echo/

REM --- Three-step atomic swap ---
REM Step 1: Move current strings.json to temp
echo [1/3] strings.json --^> strings.tmp ...
move /y "strings.json" "strings.tmp" >nul 2>&1
if errorlevel 1 (echo   [FAILED] & pause & exit /b 1)
echo   [OK]

REM Step 2: Move target language file to strings.json
echo [2/3] %TARGET_FILE% --^> strings.json ...
move /y "%TARGET_FILE%" "strings.json" >nul 2>&1
if errorlevel 1 (
    echo   [FAILED] Rolling back...
    move /y "strings.tmp" "strings.json" >nul 2>&1
    pause
    exit /b 1
)
echo   [OK]

REM Step 3: Move temp to backup
echo [3/3] strings.tmp --^> %BACKUP_FILE% ...
move /y "strings.tmp" "%BACKUP_FILE%" >nul 2>&1
if errorlevel 1 (
    echo   [FAILED] Rolling back...
    move /y "strings.json" "%TARGET_FILE%" >nul 2>&1
    move /y "strings.tmp" "strings.json" >nul 2>&1
    pause
    exit /b 1
)
echo   [OK]

echo/
echo ============================================
echo  Done! / 完成！
echo  Language switched from %CURRENT_NAME% to %TARGET_NAME%
echo  已从 %CURRENT_NAME% 切换为 %TARGET_NAME%
echo ============================================
echo/
pause
