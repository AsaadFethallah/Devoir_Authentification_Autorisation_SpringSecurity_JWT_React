$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080'
$runId = Get-Date -Format 'yyyyMMddHHmmss'
$employee1Email = "scenario.employee1.$runId@example.com"
$employee2Email = "scenario.employee2.$runId@example.com"
$employeePassword = 'Scenario123!'

function Invoke-Api {
    param(
        [Parameter(Mandatory)] [string] $Method,
        [Parameter(Mandatory)] [string] $Path,
        [object] $Body,
        [string] $Token,
        [int] $ExpectedStatus = 200
    )

    $parameters = @{
        Method = $Method
        Uri = "$baseUrl$Path"
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $Body) {
        $parameters.Body = $Body | ConvertTo-Json -Depth 10
        $parameters.ContentType = 'application/json'
    }
    if ($Token) {
        $parameters.Headers = @{ Authorization = "Bearer $Token" }
    }

    $response = Invoke-WebRequest @parameters
    $data = $null
    if ($response.Content) {
        $data = $response.Content | ConvertFrom-Json
    }
    if ($response.StatusCode -ne $ExpectedStatus) {
        throw "Étape échouée: $Method $Path retourne $($response.StatusCode), attendu $ExpectedStatus"
    }
    return [PSCustomObject]@{ Status = $response.StatusCode; Data = $data }
}

function Register-Employee([string] $fullName, [string] $email) {
    return (Invoke-Api 'POST' '/auth/register' @{ fullName = $fullName; email = $email; password = $employeePassword } $null 201).Data
}

function Login([string] $email, [string] $password) {
    return (Invoke-Api 'POST' '/auth/login' @{ email = $email; password = $password }).Data.token
}

function Step([int] $number, [string] $message) {
    Write-Host ("[{0:00}/17] {1}" -f $number, $message) -ForegroundColor Cyan
}

Step 1 'Création du premier compte EMPLOYEE'
$employee1 = Register-Employee 'Employé Scénario 1' $employee1Email

Step 2 'Création du deuxième compte EMPLOYEE'
$employee2 = Register-Employee 'Employé Scénario 2' $employee2Email

Step 3 'Vérification des comptes TECHNICIAN et ADMIN de développement'
$users = (Invoke-Api 'GET' '/api/admin/users' ( $null ) (Login 'admin@example.com' 'Admin123!')).Data
$technician = $users | Where-Object { $_.role -eq 'TECHNICIAN' -and $_.enabled } | Select-Object -First 1
if (-not $technician) { throw 'Aucun technicien actif trouvé' }

Step 4 'Connexion du premier employé'
$employee1Token = Login $employee1Email $employeePassword

Step 5 'Déclaration de deux incidents par le premier employé'
$incident1 = (Invoke-Api 'POST' '/api/incidents' @{ title = 'Scénario - poste suspect'; description = 'Le poste présente un comportement inhabituel.'; category = 'MALWARE'; priority = 'HIGH' } $employee1Token).Data
$incident2 = (Invoke-Api 'POST' '/api/incidents' @{ title = 'Scénario - accès impossible'; description = "L’accès au compte métier est impossible."; category = 'ACCESS'; priority = 'MEDIUM' } $employee1Token).Data

Step 6 'Connexion du deuxième employé'
$employee2Token = Login $employee2Email $employeePassword

Step 7 "Déclaration d’un incident par le deuxième employé"
$otherIncident = (Invoke-Api 'POST' '/api/incidents' @{ title = 'Scénario - réseau indisponible'; description = 'Le réseau du bureau est indisponible.'; category = 'NETWORK'; priority = 'LOW' } $employee2Token).Data

Step 8 "Vérification de l’isolation entre employés"
Invoke-Api 'GET' "/api/incidents/$($otherIncident.id)" $null $employee1Token 403 | Out-Null

Step 9 "Connexion de l’administrateur"
$adminToken = Login 'admin@example.com' 'Admin123!'

Step 10 'Consultation de tous les incidents par ADMIN'
$allIncidents = (Invoke-Api 'GET' '/api/incidents' $null $adminToken).Data
if (-not ($allIncidents | Where-Object { $_.id -eq $incident1.id })) { throw "L’incident du scénario n’apparaît pas dans la liste admin" }

Step 11 'Affectation du premier incident au technicien'
$assigned = (Invoke-Api 'PUT' "/api/incidents/$($incident1.id)/assign/$($technician.id)" $null $adminToken).Data
if ($assigned.status -ne 'ASSIGNED') { throw "Statut après affectation inattendu: $($assigned.status)" }

Step 12 'Connexion du technicien'
$technicianPassword = if ($technician.email -eq 'tech1@example.com' -or $technician.email -eq 'tech2@example.com') { 'Tech123!' } else { throw "Mot de passe inconnu pour le compte de test $($technician.email)" }
$technicianToken = Login $technician.email $technicianPassword

Step 13 'Consultation des incidents affectés par TECHNICIAN'
$assignedList = (Invoke-Api 'GET' '/api/incidents/assigned' $null $technicianToken).Data
if (-not ($assignedList | Where-Object { $_.id -eq $incident1.id })) { throw "L’incident affecté n’apparaît pas dans la file du technicien" }

Step 14 'Transitions ASSIGNED -> IN_PROGRESS -> RESOLVED'
$inProgress = (Invoke-Api 'PUT' "/api/incidents/$($incident1.id)/status" @{ status = 'IN_PROGRESS' } $technicianToken).Data
if ($inProgress.status -ne 'IN_PROGRESS') { throw 'La transition vers IN_PROGRESS a échoué' }
$resolved = (Invoke-Api 'PUT' "/api/incidents/$($incident1.id)/status" @{ status = 'RESOLVED' } $technicianToken).Data
if ($resolved.status -ne 'RESOLVED') { throw 'La transition vers RESOLVED a échoué' }

Step 15 "Ajout d’une note de traitement"
Invoke-Api 'POST' "/api/incidents/$($incident1.id)/comments" @{ content = 'Analyse antivirus effectuée dans le cadre du scénario.' } $technicianToken | Out-Null

Step 16 'Reconnexion du premier employé'
$employee1Token = Login $employee1Email $employeePassword

Step 17 "Vérification de l’état final, de l’historique et de la note"
$finalIncident = (Invoke-Api 'GET' "/api/incidents/$($incident1.id)" $null $employee1Token).Data
$finalHistory = (Invoke-Api 'GET' "/api/incidents/$($incident1.id)/history" $null $employee1Token).Data
$finalComments = (Invoke-Api 'GET' "/api/incidents/$($incident1.id)/comments" $null $employee1Token).Data

if ($finalIncident.status -ne 'RESOLVED') { throw "État final inattendu: $($finalIncident.status)" }
if (-not ($finalHistory | Where-Object { $_.action -like '*Affecté à*' })) { throw "Historique d’affectation absent" }
if (-not ($finalHistory | Where-Object { $_.action -eq 'ASSIGNED -> IN_PROGRESS' })) { throw 'Historique de passage en cours absent' }
if (-not ($finalHistory | Where-Object { $_.action -eq 'IN_PROGRESS -> RESOLVED' })) { throw 'Historique de résolution absent' }
if (-not ($finalComments | Where-Object { $_.content -like '*Analyse antivirus*' })) { throw 'Note de traitement absente' }

Write-Host ''
Write-Host 'SCÉNARIO PHASE 4 RÉUSSI' -ForegroundColor Green
Write-Host ("Incident vérifié: #{0} | état final: {1} | événements: {2} | notes: {3}" -f $finalIncident.id, $finalIncident.status, $finalHistory.Count, $finalComments.Count) -ForegroundColor Green
Write-Host ("Comptes temporaires créés: {0}, {1}" -f $employee1Email, $employee2Email) -ForegroundColor DarkGray
