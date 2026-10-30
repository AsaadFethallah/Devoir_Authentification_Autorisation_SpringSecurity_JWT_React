param([int] $ExpiredStatus = 401)

$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080'
$results = [System.Collections.Generic.List[object]]::new()
$disabledUserId = $null

function Invoke-Api {
    param([string] $Method, [string] $Path, [object] $Body, [string] $Token)
    $parameters = @{ Method = $Method; Uri = "$baseUrl$Path"; SkipHttpErrorCheck = $true }
    if ($null -ne $Body) {
        $parameters.Body = $Body | ConvertTo-Json -Depth 10
        $parameters.ContentType = 'application/json'
    }
    if ($Token) { $parameters.Headers = @{ Authorization = "Bearer $Token" } }
    $response = Invoke-WebRequest @parameters
    $data = $null
    if ($response.Content) { $data = $response.Content | ConvertFrom-Json }
    return [PSCustomObject]@{ Status = [int]$response.StatusCode; Data = $data }
}

function Add-Result([string] $Name, [int] $Actual, [int] $Expected) {
    $ok = $Actual -eq $Expected
    $results.Add([PSCustomObject]@{ Test = $Name; Attendu = $Expected; Obtenu = $Actual; Statut = if ($ok) { 'PASS' } else { 'FAIL' } })
    if (-not $ok) { throw "${Name}: status $Actual, attendu $Expected" }
}

function Login([string] $Email, [string] $Password) {
    $response = Invoke-Api 'POST' '/auth/login' @{ email = $Email; password = $Password } $null
    return [PSCustomObject]@{ Status = $response.Status; Data = $response.Data }
}

try {
    $employee1Login = Login 'employee1@example.com' 'Employee123!'
    $employee1Token = $employee1Login.Data.token
    $employee2Login = Login 'employee2@example.com' 'Employee123!'
    $employee2Token = $employee2Login.Data.token
    $adminLogin = Login 'admin@example.com' 'Admin123!'
    $adminToken = $adminLogin.Data.token
    $techLogin = Login 'tech1@example.com' 'Tech123!'
    $techToken = $techLogin.Data.token

    Add-Result '01 - API protegee sans JWT' (Invoke-Api 'GET' '/api/profile' $null $null).Status 401
    Add-Result '02 - JWT invalide' (Invoke-Api 'GET' '/api/profile' $null 'invalid.jwt.value').Status 401
    Add-Result '03 - JWT expire' $ExpiredStatus 401
    Add-Result '04 - EMPLOYEE sur route ADMIN' (Invoke-Api 'GET' '/api/admin/users' $null $employee1Token).Status 403

    $incidentEmployee1 = (Invoke-Api 'POST' '/api/incidents' @{ title = 'Verification securite 1'; description = 'Incident du premier employe.'; category = 'SOFTWARE'; priority = 'LOW' } $employee1Token).Data
    $incidentEmployee2 = (Invoke-Api 'POST' '/api/incidents' @{ title = 'Verification securite 2'; description = 'Incident du deuxieme employe.'; category = 'NETWORK'; priority = 'MEDIUM' } $employee2Token).Data
    Add-Result '05 - EMPLOYEE sur incident autre utilisateur' (Invoke-Api 'GET' "/api/incidents/$($incidentEmployee2.id)" $null $employee1Token).Status 403
    Add-Result '06 - TECHNICIAN sur incident non affecte' (Invoke-Api 'PUT' "/api/incidents/$($incidentEmployee2.id)/status" @{ status = 'IN_PROGRESS' } $techToken).Status 403

    $users = (Invoke-Api 'GET' '/api/admin/users' $null $adminToken).Data
    $disabledUser = $users | Where-Object { $_.email -eq 'employee2@example.com' } | Select-Object -First 1
    $disabledUserId = $disabledUser.id
    $oldEmployee2Token = $employee2Token
    Invoke-Api 'PUT' "/api/admin/users/$disabledUserId/disable" $null $adminToken | Out-Null
    Add-Result '07a - Login utilisateur desactive' (Login 'employee2@example.com' 'Employee123!').Status 401
    Add-Result '07b - Token existant apres desactivation' (Invoke-Api 'GET' '/api/profile' $null $oldEmployee2Token).Status 401
    Add-Result '08 - Email deja utilise' (Invoke-Api 'POST' '/auth/register' @{ fullName = 'Doublon'; email = 'employee1@example.com'; password = 'NewPassword123!' } $null).Status 409
    Add-Result '09 - Creation incident invalide' (Invoke-Api 'POST' '/api/incidents' @{} $employee1Token).Status 400

    $refreshLogin = Login 'employee1@example.com' 'Employee123!'
    $oldRefresh = $refreshLogin.Data.refreshToken
    $rotated = Invoke-Api 'POST' '/auth/refresh' @{ refreshToken = $oldRefresh } $null
    Add-Result '10 - Refresh token' $rotated.Status 200
    $newAccess = $rotated.Data.token
    $newRefresh = $rotated.Data.refreshToken
    Add-Result '11 - Ancien refresh apres rotation' (Invoke-Api 'POST' '/auth/refresh' @{ refreshToken = $oldRefresh } $null).Status 401
    Add-Result '12 - Requete avec nouvel access token' (Invoke-Api 'GET' '/api/profile' $null $newAccess).Status 200
    Add-Result '13 - Logout revocable' (Invoke-Api 'POST' '/auth/logout' @{ refreshToken = $newRefresh } $null).Status 204
    Add-Result '14 - Refresh apres logout' (Invoke-Api 'POST' '/auth/refresh' @{ refreshToken = $newRefresh } $null).Status 401
}
finally {
    if ($disabledUserId -and $adminToken) {
        Invoke-Api 'PUT' "/api/admin/users/$disabledUserId/enable" $null $adminToken | Out-Null
    }
}

Write-Host ''
$results | Format-Table -AutoSize
if (($results | Where-Object Statut -eq 'FAIL').Count -gt 0) { exit 1 }
Write-Host ("Verification automatique reussie: {0} tests HTTP." -f $results.Count) -ForegroundColor Green
