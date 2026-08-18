[CmdletBinding()]
param([string[]]$Hosts = @("verse.local", "auth.verse.local", "mail.verse.local"))

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

foreach ($hostName in $Hosts) {
    $addresses = [System.Net.Dns]::GetHostAddresses($hostName)
    if (-not ($addresses.IPAddressToString -contains "127.0.0.1")) {
        throw "$hostName must resolve to 127.0.0.1. Review the documented hosts entries."
    }
    Write-Host "$hostName resolves to 127.0.0.1."
}
