param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Ip = "203.0.113.77",
    [int]$Count = 55,
    [int]$Status = 200,
    [int]$MaxWaitSeconds = 30,
    [int]$PollIntervalMs = 1000
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host "[STEP] $Message" -ForegroundColor Cyan
}

function Write-Info([string]$Message) {
    Write-Host "[INFO] $Message" -ForegroundColor Gray
}

Write-Step "Inject logs"
for ($i = 1; $i -le $Count; $i++) {
    $body = @{ ip = $Ip; url = "/api/probe"; status = $Status } | ConvertTo-Json -Compress
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/logs" -ContentType "application/json" -Body $body | Out-Null
}
Write-Info "Injected $Count logs for $Ip"

Write-Step "Poll feature count until Kafka consumer catches up"
$deadline = (Get-Date).AddSeconds($MaxWaitSeconds)
$observedCount = -1

while ((Get-Date) -lt $deadline) {
    $features = Invoke-RestMethod -Method Get -Uri "$BaseUrl/analyze/features"
    $targetFeature = $features | Where-Object { $_.ip -eq $Ip } | Select-Object -First 1

    if ($null -ne $targetFeature) {
        $observedCount = [int]$targetFeature.requestCount
        Write-Info "Observed requestCount=$observedCount for $Ip"
        if ($observedCount -ge $Count) {
            break
        }
    } else {
        Write-Info "Feature row not ready yet for $Ip"
    }

    Start-Sleep -Milliseconds $PollIntervalMs
}

if ($observedCount -lt $Count) {
    Write-Warning "Kafka consumer lag remains. Expected >= $Count, observed=$observedCount"
}

Write-Step "Run analyze and print target row"
$analysis = Invoke-RestMethod -Method Post -Uri "$BaseUrl/analyze"
$target = $analysis | Where-Object { $_.ip -eq $Ip } | Select-Object -First 1

if ($null -eq $target) {
    Write-Host "[FAIL] Target IP not found in /analyze result: $Ip" -ForegroundColor Red
    exit 1
}

$target | ConvertTo-Json -Depth 5

Write-Step "Summary"
Write-Host ("ip={0}, requestCount={1}, z={2}, ai={3}, risk={4}" -f $target.ip, $target.requestCount, ([double]$target.score).ToString("F3"), ([double]$target.aiScore).ToString("F3"), $target.riskLevel)
