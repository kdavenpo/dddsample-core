# Builds the three jars and the three images the deployment uses (CS 8674, DECISIONS 65).
# Run from the repository root, on branch c3-deployment:   .\deploy\build.ps1
# The starting system's jars are built from branch subject-system, in a worktree next to this one,
# so the baseline runs exactly the starting system's code.
$ErrorActionPreference = "Stop"
$root = (Get-Location).Path
$c0 = Join-Path (Split-Path $root -Parent) "dddsample-c0"

if (-not (Test-Path $c0)) {
    git worktree add $c0 subject-system
}
New-Item -ItemType Directory -Force deploy\jars | Out-Null

Push-Location $c0
.\mvnw.cmd -q -DskipTests package
.\mvnw.cmd -q -DskipTests -f pathfinder\pom.xml package
Pop-Location
Copy-Item (Get-ChildItem "$c0\target\dddsample-*.jar" | Where-Object Name -notlike "*plain*")[0].FullName deploy\jars\c0-cargo.jar
Copy-Item (Get-ChildItem "$c0\pathfinder\target\pathfinder-*.jar" | Where-Object Name -notlike "*plain*")[0].FullName deploy\jars\c0-pathfinder.jar

.\mvnw.cmd -q -DskipTests package
Copy-Item (Get-ChildItem "target\dddsample-*.jar" | Where-Object Name -notlike "*plain*")[0].FullName deploy\jars\c3.jar

docker build -t dddsample-c3:local --build-arg JAR=c3.jar deploy
docker build -t dddsample-c0-cargo:local --build-arg JAR=c0-cargo.jar deploy
docker build -t dddsample-c0-pathfinder:local --build-arg JAR=c0-pathfinder.jar deploy
Write-Host "Built deploy\jars\ and the images dddsample-c3, dddsample-c0-cargo, dddsample-c0-pathfinder."
