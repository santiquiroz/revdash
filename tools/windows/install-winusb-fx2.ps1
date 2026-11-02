$ErrorActionPreference = 'Stop'
$root = Join-Path $env:TEMP 'revdash-winusb'
New-Item -ItemType Directory $root -Force | Out-Null
$pkg = Join-Path $root 'pkg'
Start-Transcript -Path (Join-Path $root 'install.log') -Force | Out-Null

try {
    Remove-Item $pkg -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory $pkg | Out-Null

    $inf = @'
[Version]
Signature   = "$Windows NT$"
Class       = USBDevice
ClassGUID   = {88BAE032-5A81-49f0-BC3D-A4FF138216D6}
Provider    = %Provider%
CatalogFile = fx2lafw_winusb.cat
DriverVer   = 11/01/2026,1.0.0.0
PnpLockdown = 1

[Manufacturer]
%Provider% = Devices,NTamd64

[Devices.NTamd64]
%DeviceName% = USB_Install, USB\VID_0925&PID_3881

[USB_Install]
Include = winusb.inf
Needs   = WINUSB.NT

[USB_Install.Services]
Include = winusb.inf
Needs   = WINUSB.NT.Services

[USB_Install.HW]
AddReg = Dev_AddReg

[Dev_AddReg]
HKR,,DeviceInterfaceGUIDs,0x10000,"{6B1B0E7C-3E2A-4C8B-9E0B-2F7D5A1C9D41}"

[Strings]
Provider   = "RevDash (local)"
DeviceName = "Analizador logico fx2lafw (Saleae Logic clon)"
'@
    Set-Content -Path (Join-Path $pkg 'fx2lafw_winusb.inf') -Value $inf -Encoding ascii

    $cat = Join-Path $pkg 'fx2lafw_winusb.cat'
    New-FileCatalog -Path (Join-Path $pkg 'fx2lafw_winusb.inf') -CatalogFilePath $cat -CatalogVersion 2 | Out-Null

    $cert = New-SelfSignedCertificate -Type CodeSigningCert -Subject 'CN=RevDash fx2lafw WinUSB (local)' `
        -CertStoreLocation Cert:\LocalMachine\My -KeyExportPolicy NonExportable -NotAfter (Get-Date).AddYears(10)
    $cer = Join-Path $root 'revdash-fx2lafw.cer'
    Export-Certificate -Cert $cert -FilePath $cer | Out-Null
    Import-Certificate -FilePath $cer -CertStoreLocation Cert:\LocalMachine\Root | Out-Null
    Import-Certificate -FilePath $cer -CertStoreLocation Cert:\LocalMachine\TrustedPublisher | Out-Null

    $sig = Set-AuthenticodeSignature -FilePath $cat -Certificate $cert -HashAlgorithm SHA256
    "firma: $($sig.Status) $($sig.StatusMessage)"

    # Como Zadig/libwdi: sin la clave privada nadie puede firmar otra cosa con este certificado de confianza

    Remove-Item -Path "Cert:\LocalMachine\My\$($cert.Thumbprint)" -DeleteKey

    pnputil /add-driver (Join-Path $pkg 'fx2lafw_winusb.inf') /install
    "pnputil exit: $LASTEXITCODE"
    Get-PnpDevice -InstanceId 'USB\VID_0925&PID_3881*' | Format-List Status, Class, FriendlyName, InstanceId
}
catch {
    "ERROR: $($_.Exception.Message)"
}
finally {
    Stop-Transcript | Out-Null
}
