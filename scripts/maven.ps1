$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$mavenVersion = '3.9.11'
$mavenDirectory = Join-Path $projectRoot ".tools\apache-maven-$mavenVersion"
$mavenCommand = Join-Path $mavenDirectory 'bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $mavenCommand)) {
    New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot '.tools') | Out-Null
    $archive = Join-Path $projectRoot '.tools\maven.zip'
    $distribution = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    Invoke-WebRequest -Uri $distribution -OutFile $archive -UseBasicParsing
    $expected = (Invoke-WebRequest -Uri "$distribution.sha512" -UseBasicParsing).Content.Trim().Split(' ')[0]
    $sha = [System.Security.Cryptography.SHA512]::Create()
    try { $actual = [BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($archive))).Replace('-', '') }
    finally { $sha.Dispose() }
    if ($actual -ne $expected) { throw 'Maven checksum mismatch' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($archive, (Join-Path $projectRoot '.tools'))
}
Push-Location $projectRoot
try { & $mavenCommand "-Dmaven.repo.local=$projectRoot\.m2\repository" @args; exit $LASTEXITCODE }
finally { Pop-Location }
