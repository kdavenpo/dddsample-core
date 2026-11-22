# Waits until every application container of the Compose project in the current folder has logged
# "Started <name>Application" (CS 8674, DECISIONS 65). Run it after "docker compose up -d" and before a
# load test:   ..\wait-ready.ps1
param([int]$TimeoutSeconds = 300)
$skip = @("db", "activemq", "router", "seed")
$apps = docker compose config --services | Where-Object { $skip -notcontains $_ }
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ($true) {
    $waiting = @($apps | Where-Object { -not (docker compose logs $_ 2>$null | Select-String -Quiet "Started [A-Za-z]*Application") })
    if ($waiting.Count -eq 0) { Write-Host "Ready: $($apps -join ', ')"; break }
    if ((Get-Date) -gt $deadline) { Write-Host "Not ready after $TimeoutSeconds s: $($waiting -join ', ')"; exit 1 }
    Write-Host "Waiting for: $($waiting -join ', ')"
    Start-Sleep -Seconds 5
}
