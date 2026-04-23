param(
    [switch]$Run,
    [switch]$EnableNativeAccess
)

# JavaFX 编译脚本：将所有 .class 统一输出到 out\classes
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcDir = Join-Path $projectRoot "src"
$resourceDir = Join-Path $projectRoot "resources"
$outDir = Join-Path $projectRoot "out\classes"

$javaFxHome = $env:JAVAFX_HOME
if ([string]::IsNullOrWhiteSpace($javaFxHome)) {
    throw "请先设置环境变量 JAVAFX_HOME，例如：C:\\javafx-sdk-21.0.2"
}
$javaFxLib = Join-Path $javaFxHome "lib"
if (-not (Test-Path $javaFxLib)) {
    throw "未找到 JavaFX lib 目录：$javaFxLib"
}

# 清理 src 下历史 .class，避免源码目录混入编译产物
Get-ChildItem -Path $srcDir -Filter *.class -File -ErrorAction SilentlyContinue | Remove-Item -Force

# 准备输出目录
if (Test-Path $outDir) {
    Remove-Item -Path $outDir -Recurse -Force
}
New-Item -ItemType Directory -Path $outDir | Out-Null

$javaSources = Get-ChildItem -Path $srcDir -Filter *.java -Recurse -File | Select-Object -ExpandProperty FullName
if (-not $javaSources -or $javaSources.Count -eq 0) {
    throw "未找到 Java 源文件：$srcDir"
}

# 编译到统一目录（命名模块）
javac --module-path $javaFxLib -d $outDir $javaSources
if ($LASTEXITCODE -ne 0) {
    throw "javac compile failed"
}

# 复制静态资源到输出目录，保持目录结构（优先使用 resources/）
function Copy-ResourceTree {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourceRoot
    )

    Get-ChildItem -Path $SourceRoot -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($SourceRoot.Length).TrimStart('\', '/')
        $destination = Join-Path $outDir $relative
        $destinationDir = Split-Path -Parent $destination
        if (-not (Test-Path $destinationDir)) {
            New-Item -ItemType Directory -Path $destinationDir -Force | Out-Null
        }
        Copy-Item $_.FullName -Destination $destination -Force
    }
}

if (Test-Path $resourceDir) {
    Copy-ResourceTree -SourceRoot $resourceDir
} else {
    # 兼容旧布局：如果仍然把资源放在 src/ 下，则照旧复制
    Get-ChildItem -Path $srcDir -Filter *.css -File -ErrorAction SilentlyContinue | ForEach-Object {
        Copy-Item $_.FullName -Destination $outDir -Force
    }

    $legacyImageDir = Join-Path $srcDir "Image"
    if (Test-Path $legacyImageDir) {
        $targetImageDir = Join-Path $outDir "Image"
        if (Test-Path $targetImageDir) {
            Remove-Item -Path $targetImageDir -Recurse -Force
        }
        Copy-Item -Path $legacyImageDir -Destination $outDir -Recurse -Force
    }
}

Write-Host "Compile success. Classes are in: $outDir"

if ($Run) {
    Write-Host "Running JavaFX app..."
    $javaArgs = @(
        "--module-path", "$javaFxLib;$outDir",
        "--add-modules", "javafx.controls"
    )
    if ($EnableNativeAccess) {
        $javaArgs += "--enable-native-access=javafx.graphics"
    } else {
        $javaArgs += "--enable-native-access=javafx.graphics"
    }
    $javaArgs += @("-m", "student.manage/studentmanage.Main")
    & java @javaArgs
}
