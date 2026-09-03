# JitPack CDN Monitor - Checks if the CloudStream gradle plugin is available
# When available, automatically re-runs the CI build

$REPO = "mahdiridoy/Saicord-Extensions"
$JITPACK_URL = "https://jitpack.io/com/github/recloudstream/gradle/gradle/-SNAPSHOT/maven-metadata.xml"
$JAR_URL = "https://jitpack.io/com/github/recloudstream/gradle/gradle/-32895aedb6-1/gradle--32895aedb6-1.jar"
$INTERVAL_SECONDS = 300  # Check every 5 minutes

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  JitPack CDN Monitor" -ForegroundColor Cyan
Write-Host "  Checking: CloudStream gradle plugin" -ForegroundColor Cyan
Write-Host "  Repo: $REPO" -ForegroundColor Cyan
Write-Host "  Interval: ${INTERVAL_SECONDS}s" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$checkCount = 0

while ($true) {
    $checkCount++
    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$timestamp] Check #$checkCount - Testing JitPack CDN..." -ForegroundColor Yellow

    try {
        # Check 1: metadata exists (baseline)
        $metaResponse = Invoke-WebRequest -Uri $JITPACK_URL -UseBasicParsing -TimeoutSec 15 -ErrorAction Stop
        Write-Host "  [OK] maven-metadata.xml accessible (HTTP $($metaResponse.StatusCode))" -ForegroundColor Green

        # Check 2: actual JAR file (the critical check)
        try {
            $jarResponse = Invoke-WebRequest -Uri $JAR_URL -UseBasicParsing -TimeoutSec 15 -ErrorAction Stop
            Write-Host "  [OK] JAR file accessible (HTTP $($jarResponse.StatusCode), size: $($jarResponse.Content.Length) bytes)" -ForegroundColor Green

            # JAR is available! Trigger CI rerun
            Write-Host ""
            Write-Host "============================================" -ForegroundColor Green
            Write-Host "  JITPACK CDN IS BACK!" -ForegroundColor Green
            Write-Host "  Triggering CI rebuild..." -ForegroundColor Green
            Write-Host "============================================" -ForegroundColor Green

            # Try to rerun via gh CLI if available
            $ghAvailable = Get-Command gh -ErrorAction SilentlyContinue
            if ($ghAvailable) {
                Write-Host "  Using gh CLI to rerun workflow..." -ForegroundColor Cyan
                $runs = gh run list --repo $REPO --limit 1 --json databaseId --output json 2>$null
                if ($runs) {
                    $runId = ($runs | ConvertFrom-Json)[0].databaseId
                    gh run rerun $runId --repo $REPO 2>$null
                    Write-Host "  Triggered rerun of workflow run #$runId" -ForegroundColor Green
                }
            } else {
                Write-Host "  gh CLI not available. Please manually re-run the workflow:" -ForegroundColor Yellow
                Write-Host "  https://github.com/$REPO/actions" -ForegroundColor Cyan
            }

            Write-Host ""
            Write-Host "Monitor complete. Exiting." -ForegroundColor Green
            exit 0
        }
        catch {
            $statusCode = $_.Exception.Response.StatusCode.value__
            if ($statusCode -eq 404) {
                Write-Host "  [WAIT] JAR file still 404 - CDN not recovered yet" -ForegroundColor Red
            } else {
                Write-Host "  [WARN] JAR check failed: HTTP $statusCode" -ForegroundColor Yellow
            }
        }
    }
    catch {
        Write-Host "  [ERROR] Failed to reach JitPack: $($_.Exception.Message)" -ForegroundColor Red
    }

    Write-Host "  Next check in ${INTERVAL_SECONDS}s... (Press Ctrl+C to stop)" -ForegroundColor DarkGray
    Write-Host ""
    Start-Sleep -Seconds $INTERVAL_SECONDS
}
