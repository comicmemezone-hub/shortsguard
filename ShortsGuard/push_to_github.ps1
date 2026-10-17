# ShortsGuard - Direct GitHub Push Script

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  🚀 ShortsGuard - Push to GitHub Script  " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location -Path $PSScriptRoot

# Check if git remote 'origin' is already configured
$existingRemote = git remote get-url origin 2>$null

if (-not $existingRemote) {
    Write-Host "No GitHub remote repository configured yet." -ForegroundColor Yellow
    Write-Host "Example URL: https://github.com/your-username/ShortsGuard.git" -ForegroundColor DarkGray
    Write-Host ""
    $repoUrl = Read-Host "👉 Paste your GitHub Repository URL"

    if ([string]::IsNullOrWhiteSpace($repoUrl)) {
        Write-Host "❌ Error: Repository URL cannot be empty." -ForegroundColor Red
        Pause
        exit 1
    }

    git remote add origin $repoUrl.Trim()
    Write-Host "✅ Remote added: $repoUrl" -ForegroundColor Green
} else {
    Write-Host "Current GitHub Remote: $existingRemote" -ForegroundColor Green
    $change = Read-Host "Do you want to change this URL? (y/N)"
    if ($change -eq 'y' -or $change -eq 'Y') {
        $repoUrl = Read-Host "👉 Paste new GitHub Repository URL"
        if (-not [string]::IsNullOrWhiteSpace($repoUrl)) {
            git remote set-url origin $repoUrl.Trim()
            Write-Host "✅ Remote updated: $repoUrl" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "📦 Ensuring all files are staged and committed..." -ForegroundColor Yellow
git add .
git commit -m "Update ShortsGuard app" 2>$null

Write-Host ""
Write-Host "🚀 Pushing branch 'main' to GitHub..." -ForegroundColor Cyan
git branch -M main
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "  🎉 SUCCESS! Pushed to GitHub successfully!             " -ForegroundColor Green
    Write-Host "  👉 Visit your repo on GitHub and open the 'Actions' tab" -ForegroundColor Green
    Write-Host "     to download your automatically compiled APK!         " -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "❌ Push encountered an issue. Check your GitHub permissions/sign-in." -ForegroundColor Red
}

Write-Host ""
Pause
