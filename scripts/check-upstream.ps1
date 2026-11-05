$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$release=Invoke-RestMethod 'https://api.github.com/repos/mesamirh/MovieBox-Tui/releases/latest'
Write-Output "Latest MovieBox-TUI release: $($release.tag_name)"
Write-Output $release.html_url
Write-Output 'Review the changes, update vendor/moviebox-tui from that tag, then rebuild and test both ABIs.'
Write-Output 'Update ENGINE_VERSION and ENGINE_COMMIT in app/build.gradle.kts and increment versionCode/versionName before releasing.'
Write-Output 'Never distribute a terminal executable as an Android engine update.'
