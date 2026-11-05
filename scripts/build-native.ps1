param([string]$Ndk = $env:ANDROID_NDK_HOME, [string[]]$Targets = @('aarch64-linux-android','x86_64-linux-android'))
$ErrorActionPreference='Stop'
if (-not $Ndk) { throw 'Set ANDROID_NDK_HOME or pass -Ndk pointing to Android NDK r28 or newer.' }
$projectRoot = Split-Path $PSScriptRoot -Parent
$bin = Join-Path $Ndk 'toolchains/llvm/prebuilt/windows-x86_64/bin'
foreach ($target in $Targets) {
    $abi = if ($target -eq 'aarch64-linux-android') {'arm64-v8a'} elseif ($target -eq 'x86_64-linux-android') {'x86_64'} else {throw "Unsupported target $target"}
    $linker = Join-Path $bin "$($target)26-clang.cmd"
    $targetKey=$target.Replace('-','_')
    [Environment]::SetEnvironmentVariable("CARGO_TARGET_$($targetKey.ToUpper())_LINKER",$linker,'Process')
    [Environment]::SetEnvironmentVariable("CC_$targetKey",$linker,'Process')
    [Environment]::SetEnvironmentVariable("AR_$targetKey",(Join-Path $bin 'llvm-ar.exe'),'Process')
    & rustup target add $target
    if ($LASTEXITCODE) {throw 'Rust target installation failed'}
    & cargo build --manifest-path "$projectRoot/native/Cargo.toml" --target $target --release --locked -j 4
    if ($LASTEXITCODE) {throw 'Native engine build failed'}
    $outputDir="$projectRoot/app/src/main/jniLibs/$abi"
    New-Item -ItemType Directory -Force $outputDir | Out-Null
    Copy-Item "$projectRoot/native/target/$target/release/libmoviebox_mobile.so" $outputDir
}
