# Запуск scrcpy для всех устройств из devices.txt
# Требования: установленный scrcpy, adb в PATH, PowerShell 5+
$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$devicesFile = Join-Path $scriptDir "devices.txt"

if (!(Test-Path $devicesFile)) {
  Write-Error "Файл devices.txt не найден: $devicesFile"
  exit 1
}

$devices = Get-Content $devicesFile | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith("#") }

foreach ($d in $devices) {
  Write-Host "Подключаюсь к $d ..."
  try {
    & adb connect $d | Out-Null
  } catch {
    Write-Warning "Не удалось подключиться к $d: $_"
  }
}

Start-Sleep -Seconds 1

foreach ($d in $devices) {
  Write-Host "Открываю scrcpy для $d ..."
  Start-Process scrcpy `
    -ArgumentList @("--serial=$d", "--max-size=1280", "--bit-rate=4M", "--no-audio", "--stay-awake", "--window-title=$d") `
    -WindowStyle Normal
}