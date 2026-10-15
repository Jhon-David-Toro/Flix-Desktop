# Downloads the libmpv build Flix plays audio with, checks its SHA-256 and leaves
# libmpv-2.dll in app/resources/windows (not committed: it weighs 121 MB).
# Needs 7-Zip. Run from the project root: powershell -File scripts/fetch-libmpv.ps1
$ErrorActionPreference = "Stop"
$url = "https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/20261004/mpv-dev-x86_64-20261004-git-413ff0b1cd.7z"
$sha256 = "445fc72a8aa10980504355a5d99c9a0a128c2fa177e029996fb312d9d3193d45"
$archive = Join-Path $env:TEMP "flix-mpv-dev.7z"
$target = Join-Path $PSScriptRoot "..\app\resources\windows"

Invoke-WebRequest -Uri $url -OutFile $archive
if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLower() -ne $sha256) { throw "SHA-256 mismatch: the download is not the expected libmpv build." }
New-Item -ItemType Directory -Force $target | Out-Null
& "C:\Program Files\7-Zip\7z.exe" e -y "-o$target" $archive libmpv-2.dll | Out-Null
Remove-Item $archive
Write-Host "libmpv-2.dll ready in $target"
