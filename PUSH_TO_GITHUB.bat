@echo off
setlocal EnableDelayedExpansion
title ShortsGuard - Push to GitHub
cd /d "%~dp0"

echo ==========================================================
echo          ShortsGuard - Push to GitHub Tool
echo ==========================================================
echo.

:: Check if git is installed
where git >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Git is not installed or not in PATH!
    echo Please install Git from https://git-scm.com/
    pause
    exit /b 1
)

:: Check current remote
set "CURRENT_REMOTE="
for /f "tokens=*" %%i in ('git remote get-url origin 2^>nul') do set "CURRENT_REMOTE=%%i"

if "%CURRENT_REMOTE%"=="" (
    echo [INFO] No GitHub repository linked yet.
    echo.
    echo 1. Go to https://github.com/new and create an empty repository
    echo 2. Copy the URL (example: https://github.com/your-username/ShortsGuard.git)
    echo.
    set "REPO_URL="
    set /p "REPO_URL=Paste your GitHub Repository URL here: "
    if not defined REPO_URL (
        echo.
        echo [ERROR] Repository URL was empty. Exiting.
        echo.
        pause
        exit /b 1
    )
    if "!REPO_URL!"=="" (
        echo.
        echo [ERROR] Repository URL was empty. Exiting.
        echo.
        pause
        exit /b 1
    )
    git remote add origin !REPO_URL!
    echo.
    echo [OK] Added remote: !REPO_URL!
) else (
    echo [INFO] Currently linked to: %CURRENT_REMOTE%
    echo.
    set "CHANGE=N"
    set /p "CHANGE=Do you want to change this repository URL? (y/N): "
    if /i "!CHANGE!"=="y" (
        set "REPO_URL="
        set /p "REPO_URL=Paste new GitHub Repository URL: "
        if defined REPO_URL (
            if not "!REPO_URL!"=="" (
                git remote set-url origin !REPO_URL!
                echo.
                echo [OK] Updated remote: !REPO_URL!
            )
        )
    )
)

echo.
echo [1/3] Adding all files...
git add .

echo.
echo [2/3] Committing changes...
git commit -m "Update ShortsGuard app" >nul 2>&1

echo.
echo [3/3] Pushing to GitHub (branch: main)...
git branch -M main
git push -u origin main --force

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================================
    echo   SUCCESS! Your code has been pushed to GitHub!
    echo   Go to your GitHub repo and open the Actions tab
    echo   to download your APK file!
    echo ==========================================================
) else (
    echo.
    echo ==========================================================
    echo   [NOTICE] If the push failed:
    echo   1. Make sure you created the repo on https://github.com/new
    echo   2. Sign in when Windows/Git prompts you for GitHub login
    echo ==========================================================
)

echo.
pause
