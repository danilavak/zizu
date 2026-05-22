param(
    [string]$OutputDir = "secrets",
    [string]$KeystoreName = "https-keystore.p12",
    [string]$Alias = "zizu-https",
    [string]$StorePassword = "changeit",
    [string]$KeyPassword = "changeit",
    [string]$DistinguishedName = "CN=localhost, OU=RBPO, O=danilavak, L=Moscow, ST=Moscow, C=RU"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw "keytool is not available in PATH. Install a JDK and retry."
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$keystorePath = Join-Path $OutputDir $KeystoreName

keytool -genkeypair `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 3650 `
    -storetype PKCS12 `
    -keystore $keystorePath `
    -storepass $StorePassword `
    -keypass $KeyPassword `
    -dname $DistinguishedName `
    -ext SAN=dns:localhost,ip:127.0.0.1

Write-Host "Created HTTPS keystore: $keystorePath"
