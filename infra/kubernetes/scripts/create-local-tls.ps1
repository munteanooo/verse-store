[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "verse-dev",
    [string]$SecretName = "verse-local-tls"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$mkcertCommand = Get-Command mkcert -CommandType Application -ErrorAction SilentlyContinue
if ($null -eq $mkcertCommand) { throw "mkcert is required." }
$kubectlCommand = Get-Command kubectl -CommandType Application -ErrorAction SilentlyContinue
if ($null -eq $kubectlCommand) { throw "kubectl is required." }
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to modify a context other than kind-$ClusterName." }

$temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) "verse-tls-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $temporaryDirectory *> $null
try {
    $namespaceOutputPath = Join-Path $temporaryDirectory "namespace.stdout"
    $namespaceErrorPath = Join-Path $temporaryDirectory "namespace.stderr"
    $namespaceProcess = Start-Process -FilePath $kubectlCommand.Source `
        -ArgumentList @("get", "namespace", $Namespace) -Wait -PassThru -NoNewWindow `
        -RedirectStandardOutput $namespaceOutputPath -RedirectStandardError $namespaceErrorPath
    if ($namespaceProcess.ExitCode -ne 0) {
        throw "Namespace '$Namespace' does not exist or cannot be accessed. Create it before generating the TLS Secret."
    }

    $certificatePath = Join-Path $temporaryDirectory "tls.crt"
    $keyPath = Join-Path $temporaryDirectory "tls.key"
    $mkcertArguments = @(
        "-cert-file", $certificatePath,
        "-key-file", $keyPath,
        "verse.local", "auth.verse.local", "mail.verse.local"
    )
    $mkcertProcess = Start-Process -FilePath $mkcertCommand.Source -ArgumentList $mkcertArguments `
        -Wait -PassThru -NoNewWindow
    if ($mkcertProcess.ExitCode -ne 0) {
        throw "mkcert failed with exit code $($mkcertProcess.ExitCode); the local TLS Secret was not changed."
    }
    foreach ($generatedFile in @(
        @{Name = "certificate"; Path = $certificatePath},
        @{Name = "private key"; Path = $keyPath}
    )) {
        if (-not (Test-Path -LiteralPath $generatedFile.Path -PathType Leaf)) {
            throw "mkcert reported success but the generated $($generatedFile.Name) file is missing."
        }
        if ((Get-Item -LiteralPath $generatedFile.Path).Length -le 0) {
            throw "mkcert reported success but the generated $($generatedFile.Name) file is empty."
        }
    }
    kubectl -n $Namespace create secret tls $SecretName --cert=$certificatePath --key=$keyPath --dry-run=client -o yaml |
        kubectl apply -f - *> $null
    if ($LASTEXITCODE -ne 0) { throw "Failed to create TLS Secret '$SecretName'." }
} finally {
    Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
Write-Host "TLS Secret '$SecretName' was created/updated without persisting its private key in the repository."
