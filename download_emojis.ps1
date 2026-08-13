$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path "src\markdownviewer\emojis"
Write-Host "Downloading Twemoji..."
Invoke-WebRequest -Uri "https://github.com/twitter/twemoji/archive/refs/tags/v14.0.2.zip" -OutFile "twemoji.zip"
Write-Host "Extracting..."
Expand-Archive -Path "twemoji.zip" -DestinationPath "temp_emojis" -Force
Write-Host "Moving SVG files..."
Move-Item -Path "temp_emojis\twemoji-14.0.2\assets\svg\*" -Destination "src\markdownviewer\emojis\" -Force
Write-Host "Cleaning up..."
Remove-Item -Path "twemoji.zip" -Force
Remove-Item -Path "temp_emojis" -Recurse -Force
Write-Host "Done!"
