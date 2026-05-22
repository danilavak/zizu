param(
    [string]$OutputDir = "secrets",
    [string]$KeystoreName = "signature-keystore.p12",
    [string]$Alias = "signature-key",
    [string]$StorePassword = "changeit",
    [string]$KeyPassword = "changeit",
    [string]$DistinguishedName = "CN=Zizu Signature, OU=RBPO, O=danilavak, L=Moscow, ST=Moscow, C=RU"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw "keytool is not available in PATH. Install a JDK and retry."
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$keystorePath = Join-Path $OutputDir $KeystoreName
$certificatePath = Join-Path $OutputDir "signature-public.cer"

keytool -genkeypair `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 3650 `
    -storetype PKCS12 `
    -keystore $keystorePath `
    -storepass $StorePassword `
    -keypass $KeyPassword `
    -dname $DistinguishedName

keytool -exportcert `
    -alias $Alias `
    -keystore $keystorePath `
    -storepass $StorePassword `
    -rfc `
    -file $certificatePath

Write-Host "Created keystore: $keystorePath"
Write-Host "Created certificate: $certificatePath"
