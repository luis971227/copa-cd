# generar-icono.ps1
# Genera el set de iconos del launcher para "Copa CD" (MiTienda POS).
# Concepto: escudo oficial COPA CD (extraido de las bases en PDF, fondo negro)
#   - legacy: fondo negro #000000 + escudo centrado al 64% de alto
#   - adaptativo: fondo negro plano + escudo (~56% del centro, sin tocar bordes)
#   - monocromo: silueta blanca del escudo
# Requiere: res\icon\cresta-oficial.png (recortar-cresta.ps1)
# Uso: powershell -NoProfile -ExecutionPolicy Bypass -File "res\icon\generar-icono.ps1"

Add-Type -AssemblyName System.Drawing

$Salida = Split-Path -Parent $MyInvocation.MyCommand.Path
$W = 512; $H = 512
$NEGRO = [System.Drawing.Color]::FromArgb(255, 0, 0, 0)
$CRESTA = Join-Path $Salida 'cresta-oficial.png'
if (-not (Test-Path $CRESTA)) { throw "falta cresta-oficial.png (ejecuta recortar-cresta.ps1)" }

function Nuevo-Canvas {
    param([int]$w = $W, [int]$h = $H)
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    return ,@($bmp, $g)
}

$crest = [System.Drawing.Bitmap]::FromFile($CRESTA)
$cw = $crest.Width; $ch = $crest.Height

# ---- Fondo adaptativo: negro plano ----
$bg = Nuevo-Canvas
$gB = $bg[1]
$gB.FillRectangle((New-Object System.Drawing.SolidBrush($NEGRO)), 0, 0, $W, $H)
$gB.Dispose()

# ---- Primer plano: escudo centrado, alto = 56% del canvas ----
$fg = Nuevo-Canvas
$gF = $fg[1]
$hF = [int]($H * 0.56)
$wF = [int]($cw * $hF / $ch)
$xF = [int](($W - $wF) / 2); $yF = [int](($H - $hF) / 2)
$gF.DrawImage($crest, $xF, $yF, $wF, $hF)
$gF.Dispose()

# ---- Composicion legacy: fondo negro + escudo al 64% de alto ----
$legacy = Nuevo-Canvas
$gL = $legacy[1]
$gL.FillRectangle((New-Object System.Drawing.SolidBrush($NEGRO)), 0, 0, $W, $H)
$hL = [int]($H * 0.64)
$wL = [int]($cw * $hL / $ch)
$gL.DrawImage($crest, [int](($W - $wL) / 2), [int](($H - $hL) / 2), $wL, $hL)
$gL.Dispose()

# ---- Monocromo: silueta blanca (usa alfa del escudo) ----
$sil = New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$silG = [System.Drawing.Graphics]::FromImage($sil)
$silG.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$silG.DrawImage($crest, $xF, $yF, $wF, $hF)
$silG.Dispose()
for ($y = 0; $y -lt $H; $y++) {
    for ($x = 0; $x -lt $W; $x++) {
        $px = $sil.GetPixel($x, $y)
        if ($px.A -gt 0) { $sil.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($px.A, 255, 255, 255)) }
    }
}

function Guarda { param($bmp, $ruta); $bmp.Save($ruta, [System.Drawing.Imaging.ImageFormat]::Png) }

function Redimensiona {
    param($bmp, [int]$tam)
    $dst = New-Object System.Drawing.Bitmap($tam, $tam, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $gs = [System.Drawing.Graphics]::FromImage($dst)
    $gs.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $gs.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gs.DrawImage($bmp, 0, 0, $tam, $tam)
    $gs.Dispose()
    return $dst
}

$densidades = @('ldpi', 'mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')
$tamanos = @{ ldpi = 36; mdpi = 48; hdpi = 72; xhdpi = 96; xxhdpi = 144; xxxhdpi = 192 }

foreach ($d in $densidades) {
    $t = $tamanos[$d]
    Guarda (Redimensiona $bg[0] $t) (Join-Path $Salida "ic_launcher_background_${d}.png")
    Guarda (Redimensiona $fg[0] $t) (Join-Path $Salida "ic_launcher_foreground_${d}.png")
    Guarda (Redimensiona $sil $t) (Join-Path $Salida "ic_launcher_monochrome_${d}.png")
    Guarda (Redimensiona $legacy[0] $t) (Join-Path $Salida "ic_launcher_${d}.png")
}

$sil.Dispose(); $fg[0].Dispose(); $legacy[0].Dispose(); $bg[0].Dispose(); $crest.Dispose()
Write-Host "OK: iconos del logo oficial regenerados en $Salida"