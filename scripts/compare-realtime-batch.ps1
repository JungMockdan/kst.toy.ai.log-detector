param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Ip = "203.0.113.99",
    [int]$Count = 60,
    [int]$Status = 200,
    [int]$MaxWaitSeconds = 40,
    [int]$PollIntervalMs = 1000
)

$ErrorActionPreference = "Stop"

function Step([string]$m) { Write-Host "[STEP] $m" -ForegroundColor Cyan }
function Info([string]$m) { Write-Host "[INFO] $m" -ForegroundColor Gray }

function PostLogWithRetry([string]$uri, [string]$jsonBody, [int]$maxRetry = 3) {
    for ($attempt = 1; $attempt -le $maxRetry; $attempt++) {
        try {
            Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $jsonBody | Out-Null
            return
        } catch {
            if ($attempt -eq $maxRetry) { throw }
            Start-Sleep -Milliseconds 200
        }
    }
}

Step "Inject logs to /logs"
for ($i = 1; $i -le $Count; $i++) {
    $body = @{ ip = $Ip; url = "/api/probe"; status = $Status } | ConvertTo-Json -Compress
    PostLogWithRetry -uri "$BaseUrl/logs" -jsonBody $body
    Start-Sleep -Milliseconds 20
}
Info "Injected $Count logs for $Ip"

Step "Wait until /results has the target IP"
$deadline = (Get-Date).AddSeconds($MaxWaitSeconds)
$realtime = $null
while ((Get-Date) -lt $deadline) {
    $rows = Invoke-RestMethod -Method Get -Uri "$BaseUrl/results"
    $realtime = $rows | Where-Object { $_.ip -eq $Ip } | Select-Object -First 1
    if ($null -ne $realtime) { break }
    Start-Sleep -Milliseconds $PollIntervalMs
}

if ($null -eq $realtime) {
    Write-Host "[FAIL] realtime row not found in /results for $Ip" -ForegroundColor Red
    exit 1
}

Step "Wait until /analyze/features has the target IP"
$featureReady = $false
$deadline2 = (Get-Date).AddSeconds($MaxWaitSeconds)
while ((Get-Date) -lt $deadline2) {
    $features = Invoke-RestMethod -Method Get -Uri "$BaseUrl/analyze/features"
    $targetFeature = $features | Where-Object { $_.ip -eq $Ip } | Select-Object -First 1
    if ($null -ne $targetFeature) {
        $featureReady = $true
        break
    }
    Start-Sleep -Milliseconds $PollIntervalMs
}

if (-not $featureReady) {
    Write-Host "[FAIL] feature row not found in /analyze/features for $Ip" -ForegroundColor Red
    exit 1
}

Step "Run /analyze and find target IP"
$analysis = Invoke-RestMethod -Method Post -Uri "$BaseUrl/analyze"
$batch = $analysis | Where-Object { $_.ip -eq $Ip } | Select-Object -First 1
if ($null -eq $batch) {
    Write-Host "[FAIL] batch row not found in /analyze for $Ip" -ForegroundColor Red
    exit 1
}

Step "Compare risk/score/aiScore"
$riskMatch = $realtime.riskLevel -eq $batch.riskLevel
Write-Host ("realtime: risk={0}, score={1}, ai={2}, req={3}" -f $realtime.riskLevel, ([double]$realtime.score).ToString("F3"), ([double]$realtime.aiScore).ToString("F3"), $realtime.requestCount)
Write-Host ("batch   : risk={0}, score={1}, ai={2}, req={3}" -f $batch.riskLevel, ([double]$batch.score).ToString("F3"), ([double]$batch.aiScore).ToString("F3"), $batch.requestCount)

if ($riskMatch) {
    Write-Host "[PASS] riskLevel is consistent between realtime and batch" -ForegroundColor Green
    exit 0
}

Write-Host "[WARN] riskLevel mismatch detected" -ForegroundColor Yellow
exit 2
