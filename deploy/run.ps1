# One measured run of C0 or C3 (CS 8674, DECISIONS 65): start fresh, wait for every service, a
# 1-minute warm-up that is not kept, then the 10-minute Locust scenario of DECISIONS 54. The access
# logs and Locust's CSVs go to cs8674-project\data\dynamic\<date>-cargo-<system>-docker-run<n>.
#   .\deploy\run.ps1 -System c0 -N 1        (from the repository root; then c3 1, c0 2, c3 2, ...)
param([Parameter(Mandatory)][ValidateSet("c0", "c3")][string]$System,
      [Parameter(Mandatory)][int]$N)
$ErrorActionPreference = "Stop"
$root = (Get-Location).Path
$project = Join-Path (Split-Path (Split-Path $root -Parent) -Parent) "cs8674-project"
$python = Join-Path $project ".venv\Scripts\python.exe"
$name = "$(Get-Date -Format yyyy-MM-dd)-cargo-$System-docker-run$N"
$out = Join-Path $project "data\dynamic\$name"
if (Test-Path $out) { throw "$out exists already" }

Push-Location "deploy\$System"
docker compose down
$env:RUN = $name
docker compose up -d
& ..\wait-ready.ps1
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "services did not start" }

Write-Host "Warm-up (1 minute, not kept)..."
$warm = Join-Path $env:TEMP "cargo-warmup-$name"
& $python -m locust -f "$project\load\locustfile_cargo.py" --headless -u 10 -r 2 -t 1m --host http://localhost:8080 --csv $warm --only-summary

Write-Host "Measured run (10 minutes)..."
New-Item -ItemType Directory -Force $out | Out-Null
& $python -m locust -f "$project\load\locustfile_cargo.py" --headless -u 10 -r 2 -t 10m --host http://localhost:8080 --csv "$out\locust" --only-summary
Start-Sleep -Seconds 5                          # let the queued work finish and the logs flush
docker compose down
Pop-Location

Copy-Item "deploy\runs\$name\*.log" $out
Write-Host "Saved $out"
