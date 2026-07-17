$ErrorActionPreference = 'Stop'

$iosRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $iosRoot 'GhostCart'
$projectFile = Join-Path $iosRoot 'GhostCart.xcodeproj\project.pbxproj'
$errors = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path -LiteralPath $projectFile)) {
    $errors.Add('Missing GhostCart.xcodeproj/project.pbxproj')
}

$swiftFiles = Get-ChildItem -LiteralPath $sourceRoot -Filter '*.swift' -File
if ($swiftFiles.Count -eq 0) {
    $errors.Add('No Swift source files found')
}

$projectText = if (Test-Path -LiteralPath $projectFile) { Get-Content -Raw -LiteralPath $projectFile } else { '' }
if ($projectText -notmatch 'isa = PBXProject;') {
    $errors.Add('project.pbxproj does not contain a PBXProject object')
}

foreach ($file in $swiftFiles) {
    if ($projectText -notmatch [regex]::Escape($file.Name)) {
        $errors.Add("Project does not reference $($file.Name)")
    }
}

$sourceText = ($swiftFiles | ForEach-Object { [System.IO.File]::ReadAllText($_.FullName, [System.Text.Encoding]::UTF8) }) -join "`n"

$requiredStates = @('captured', 'cooling', 'snoozed', 'resolved_skipped', 'resolved_bought', 'expired')
foreach ($state in $requiredStates) {
    if ($sourceText -notmatch [regex]::Escape($state)) {
        $errors.Add("Missing canonical state: $state")
    }
}

$requiredTabs = @('Home', 'Cooldowns', 'Ghost +', 'Progress', 'Profile')
foreach ($tab in $requiredTabs) {
    if ($sourceText -notmatch [regex]::Escape('"' + $tab + '"')) {
        $errors.Add("Missing primary tab label: $tab")
    }
}

if ($sourceText -notmatch 'moneyKept:\s*items\.filter\s*\{\s*\$0\.state\s*==\s*\.resolvedSkipped') {
    $errors.Add('Money Kept is not visibly derived from resolvedSkipped items only')
}

$prohibited = @(
    'loremflickr',
    'AsyncImage\s*\(\s*url:',
    '\bCVV\b',
    'expiry date',
    'payment successful',
    'card was charged',
    'tax invoice',
    'bank balance',
    'wallet balance',
    '\bGhost Pay\b',
    '\bVisa\b',
    '\bMastercard\b',
    '\bApple Pay\b',
    '\bGoogle Pay\b',
    '\bAED\b',
    '"\$[0-9]'
)

foreach ($pattern in $prohibited) {
    if ($sourceText -match $pattern) {
        $errors.Add("Prohibited source pattern found: $pattern")
    }
}

if ($sourceText -match '[\u00C2\u00C3\u00E2]') {
    $errors.Add('Possible mojibake found in Swift source')
}

if ($errors.Count -gt 0) {
    Write-Host 'Ghost Cart iOS static checks failed:' -ForegroundColor Red
    $errors | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "Ghost Cart iOS static checks passed for $($swiftFiles.Count) Swift files." -ForegroundColor Green
Write-Host 'A macOS/Xcode build is still required; this Windows check does not compile SwiftUI.' -ForegroundColor Yellow
