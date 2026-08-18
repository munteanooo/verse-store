[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "verse-dev",
    [string]$AppBaseUrl = "https://verse.local"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$kubectlCommand = Get-Command kubectl -CommandType Application -ErrorAction SilentlyContinue
if ($null -eq $kubectlCommand) { throw "kubectl is required." }
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to modify a context other than kind-$ClusterName." }
kubectl -n $Namespace rollout status deployment/verse-keycloak --timeout=5m
$pod = kubectl -n $Namespace get pod -l app.kubernetes.io/component=keycloak -o jsonpath='{.items[0].metadata.name}'
if ([string]::IsNullOrWhiteSpace($pod)) { throw "Keycloak pod was not found." }

$script = @"
set -eu
KCADM=/opt/keycloak/bin/kcadm.sh
CONFIG=/tmp/verse-kcadm.config
CLIENT_FILE=/tmp/verse-client.json
trap 'rm -f `$CONFIG `$CLIENT_FILE' EXIT
KC_CLI_PASSWORD="`$KC_BOOTSTRAP_ADMIN_PASSWORD" `$KCADM config credentials --config `$CONFIG --server http://localhost:8080 --realm master --user "`$KC_BOOTSTRAP_ADMIN_USERNAME" >/dev/null

`$KCADM update realms/verse --config `$CONFIG \
  -s registrationAllowed=true \
  -s verifyEmail=false \
  -s resetPasswordAllowed=true \
  -s loginWithEmailAllowed=true \
  -s duplicateEmailsAllowed=false \
  -s 'passwordPolicy="length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and notUsername(undefined)"'

for ROLE in CUSTOMER ADMIN; do
  if ! `$KCADM get "roles/`$ROLE" --config `$CONFIG -r verse >/dev/null 2>&1; then
    `$KCADM create roles --config `$CONFIG -r verse -s "name=`$ROLE" >/dev/null
  fi
done
`$KCADM update roles/CUSTOMER --config `$CONFIG -r verse -s 'description="Verse Store customer"'
`$KCADM update roles/ADMIN --config `$CONFIG -r verse -s 'description="Verse Store control-plane administrator"'

GROUP_ID=`$(`$KCADM get groups --config `$CONFIG -r verse -q search=customers -q exact=true --fields id --format csv --noquotes | head -n 1)
if [ -z "`$GROUP_ID" ]; then
  `$KCADM create groups --config `$CONFIG -r verse -s name=customers >/dev/null
  GROUP_ID=`$(`$KCADM get groups --config `$CONFIG -r verse -q search=customers -q exact=true --fields id --format csv --noquotes | head -n 1)
fi
test -n "`$GROUP_ID"
if ! `$KCADM get "groups/`$GROUP_ID/role-mappings/realm/composite" --config `$CONFIG -r verse --fields name --format csv --noquotes | grep -Fx CUSTOMER >/dev/null; then
  `$KCADM add-roles --config `$CONFIG -r verse --gid "`$GROUP_ID" --rolename CUSTOMER
fi
if ! `$KCADM get realms/verse/default-groups --config `$CONFIG --fields id --format csv --noquotes | grep -Fx "`$GROUP_ID" >/dev/null; then
  `$KCADM update "realms/verse/default-groups/`$GROUP_ID" --config `$CONFIG -n
fi

`$KCADM update authentication/required-actions/VERIFY_EMAIL --config `$CONFIG -r verse -s defaultAction=false

CLIENT_UUID=`$(`$KCADM get clients --config `$CONFIG -r verse -q clientId=verse-store --fields id --format csv --noquotes)
if [ -z "`$CLIENT_UUID" ]; then
  umask 077
  cat > "`$CLIENT_FILE" <<EOF
{"clientId":"verse-store","name":"Verse Store","enabled":true,"clientAuthenticatorType":"client-secret","secret":"`$KEYCLOAK_CLIENT_SECRET","publicClient":false,"protocol":"openid-connect","standardFlowEnabled":true,"implicitFlowEnabled":false,"directAccessGrantsEnabled":false,"serviceAccountsEnabled":false,"frontchannelLogout":true}
EOF
  `$KCADM create clients --config `$CONFIG -r verse -f "`$CLIENT_FILE" >/dev/null
  CLIENT_UUID=`$(`$KCADM get clients --config `$CONFIG -r verse -q clientId=verse-store --fields id --format csv --noquotes)
fi
test -n "`$CLIENT_UUID"
`$KCADM update "clients/`$CLIENT_UUID" --config `$CONFIG -r verse \
  -s 'redirectUris=["$AppBaseUrl/login/oauth2/code/verse-store"]' \
  -s 'webOrigins=["$AppBaseUrl"]' \
  -s 'attributes."post.logout.redirect.uris"="$AppBaseUrl/"'
"@
$script = $script.Replace("`r`n", "`n")
$processStartInfo = New-Object System.Diagnostics.ProcessStartInfo
$processStartInfo.FileName = $kubectlCommand.Source
$processStartInfo.Arguments = "-n `"$Namespace`" exec -i `"$pod`" -- sh"
$processStartInfo.UseShellExecute = $false
$processStartInfo.RedirectStandardInput = $true
$processStartInfo.RedirectStandardOutput = $true
$processStartInfo.RedirectStandardError = $true
$processStartInfo.CreateNoWindow = $true

$process = New-Object System.Diagnostics.Process
$process.StartInfo = $processStartInfo
$processStarted = $false
try {
    if (-not $process.Start()) { throw "Failed to start kubectl for Keycloak client reconciliation." }
    $processStarted = $true
    $standardOutputTask = $process.StandardOutput.ReadToEndAsync()
    $standardErrorTask = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($script)
    $process.StandardInput.Close()
    $process.WaitForExit()
    $null = $standardOutputTask.Result
    $null = $standardErrorTask.Result
    if ($process.ExitCode -ne 0) {
        throw "Keycloak client reconciliation failed with kubectl exit code $($process.ExitCode)."
    }
} finally {
    if ($processStarted -and -not $process.HasExited) { $process.Kill() }
    $process.Dispose()
}
Write-Host "Persistent Keycloak realm, roles, customer group, required actions, and client settings are reconciled."
