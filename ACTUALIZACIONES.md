# Actualizaciones de la APK y sincronización entre móviles

La Copa CD se reparte como APK sideloaded (sin Play Store).

- **Actualizaciones de la APK** → **GitHub Releases** (público, sin token ni cuenta Firebase).
- **Datos de la copa** → **Firebase** (Authentication + Realtime Database), con Auto-sync.

## 1. GitHub Releases (actualizaciones de la APK)

### Crear el repo (una vez)

1. Entra a **https://github.com** con tu cuenta y crea un repositorio **público**, por ejemplo `copa-cd`.
2. No hace falta subir el código: solo se usa como sitio donde publicar los `.apk`. Puedes dejarlo vacío (o con un README).

### Configurar la app (una vez por móvil)

**Sincronizar → ⬆️ Actualizaciones (GitHub Releases)**:

1. **Repositorio (usuario/repo)** → escribe `tunombre/copa-cd`.
2. **Guardar repo** → **Comprobar versión**.

El repo se guarda en `localStorage` (`mtp_copa_cd_github_repo`) y **viaja en el JSON manual** (`paqueteSync().datos.githubRepo`), así que los capitanes que reciban la copa por primera vez ya lo tienen. Para fijarlo en la APK, rellena `GITHUB_REPO='tunombre/copa-cd'` al inicio del `<script>` de `www/index.html` y recompila.

### Publicar una versión nueva

1. Cambia `APP_VERSION` (ej. `1.0.2`) y sube `APP_BUILD` en `www/index.html`.
2. Sube la versión en `config.xml` (`widget version="…"`) y, como no hay Cordova CLI en la máquina de build, incrementa a mano `android:versionCode` y `android:versionName` en `platforms/android/app/src/main/AndroidManifest.xml` (p. ej. `10000`/`1.0.0` → `10001`/`1.0.1`). Cordova calcula el `versionCode` a partir del manifest en `build.gradle`.
3. Ejecuta los smokes, borra `www/smoke*.html`, copia `www/index.html` a `platforms/android/app/src/main/assets/www/index.html`, compila con `cmd /c build-reglamento.bat` y verifica el SHA con `verificar-apk.ps1`.
4. En GitHub: **Releases → Draft a new release**:
   - **Tag**: `v1.0.2` (debe empezar por `v`; la app compara `X.Y.Z` con `APP_VERSION`).
   - **Title**: `Copa CD 1.0.2`.
   - **Description**: las notas que verá el usuario en el aviso.
   - **Attach binaries**: arrastra la APK de `platforms\android\app\build\outputs\apk\debug\app-debug.apk`. (Mejor renómbrala antes a `CopaCD-1.0.2.apk`.)
   - **Publish release**.

Al abrir la app, cada móvil consulta:

```
https://api.github.com/repos/tunombre/copa-cd/releases/latest
```

Es **público, sin API key ni token** (límite: 60 peticiones/hora por IP, más que suficiente para unos pocos móviles). Si el tag es mayor que `APP_VERSION`, aparece el modal **⬆️ Hay una actualización** con versión y notas. **⬇️ Descargar e instalar** abre el `browser_download_url` del asset `.apk` con `window.open(..., '_system')`: Android abre el navegador/Descargas y el usuario acepta la instalación (no se pide `REQUEST_INSTALL_PACKAGES`). **Ahora no** pospone el aviso 24 h (`mtp_copa_cd_act_snooze`).

> Para el primer despliegue de esta APK (`1.0.1`) publica el Release `v1.0.1`: nadie verá el aviso, y cuando subas `v1.0.2` saltará.

## 2. Firebase (datos de la copa)

### Crear el proyecto (una vez)

1. **https://console.firebase.google.com** → iniciar sesión con Google → **Agregar proyecto** (ej. `copa-cd-2026`).
2. **Authentication → Sign-in method → Email/Password → Habilitar**. Crear un usuario por dispositivo (o uno compartido) en **Usuarios → Agregar usuario**. Son cuentas solo para autorizar la sync; no son los usuarios del torneo.
3. **Realtime Database → Crear base de datos** (modo bloqueado). Copiar la URL, tipo `https://copa-cd-2026-default-rtdb.europe-west1.firebasedatabase.app`.
4. Pestaña **Reglas** → pegar y **Publicar**:

```json
{
  "rules": {
    "copas": {
      "$idCopa": {
        "paquetes": {
          ".read": "auth != null",
          "$uid": {
            "dispositivos": {
              "$dispositivo": {
                ".write": "auth != null && auth.uid === $uid && newData.exists()",
                ".validate": "newData.hasChildren(['createdAt', 'uid', 'paquete']) && newData.child('uid').val() === $uid && newData.child('paquete').child('app').val() === 'copacd' && newData.child('paquete').child('idCopa').val() === $idCopa && !newData.child('paquete').child('datos').child('usuarios').exists()",
                "createdAt": { ".validate": "newData.isNumber()" },
                "uid": { ".validate": "newData.isString()" },
                "paquete": { ".validate": "newData.hasChildren(['app', 'idCopa', 'datos'])" }
              }
            }
          }
        }
      }
    }
  }
}
```

Cualquier cuenta Firebase que conozca el ID de la copa puede leer y publicar su propia copia; es un exchange compartido, no almacenamiento privado. **Ya no hace falta configurar Firebase Storage ni el nodo `copacd/app/version`**: las APK y el aviso de versión están en GitHub.

### Configurar la app

**Sincronizar → ☁️ Nube Firebase**:

1. **Firebase Web API key** — *Project settings → General → Your apps → Web app → apiKey* (`AIza…`).
2. **URL de Realtime Database** — la del paso 3.
3. **Guardar configuración** → **Iniciar sesión** con la cuenta Firebase → activar **🔁 Auto**.

La configuración se guarda en `localStorage` (`mtp_copa_cd_nube`) **y viaja en el JSON manual** (`paqueteSync().datos.nube`). Para fijarla en la APK, rellena `NUBE_DEFAULT` al inicio del `<script>`:

```js
var NUBE_DEFAULT={apiKey:'AIza…',databaseURL:'https://tu-proyecto-default-rtdb.europe-west1.firebasedatabase.app'};
```

La Web API key es pública (no da acceso sin iniciar sesión), pero conviene no pegarla en el código si la copa se reparte fuera del equipo. Los tokens de Firebase solo viven en memoria: se inicia sesión en cada arranque de la app.

## 3. Cómo anotan los capitanes desde su móvil

1. El superadmin crea la copa y reparte el JSON manual una vez (**Compartir copa** → WhatsApp/Drive/archivo). El JSON incluye la configuración Firebase, el repo de GitHub y los usuarios/capitanes.
2. El capitán abre la app, **Sincronizar → 📁 Buscar archivo recibido**, acepta el `confirm()` y entra con su usuario de la copa; después inicia sesión en Firebase.
3. Activa **🔁 Auto**. A partir de ahí:
   - Cada cambio (`guardar()`) publica su copia en `copas/<idCopa>/paquetes/<uid>/dispositivos/<deviceId>` con 900 ms de debounce.
   - Cada 45 s baja los paquetes de los demás dispositivos y los mezcla. Si alguien aportó datos, sale el aviso «Otro dispositivo actualizó la copa».
   - El capitán solo puede editar los partidos que el superadmin le asignó como árbitro (`puedeArbitrar`). Los datos se suben igual para todos.
   - Sin internet sigue disponible el intercambio manual de JSON/código COPACD1 por WhatsApp, Bluetooth o Nearby Share.

### Mezcla de anotaciones

Antes la sincronización solo rellenaba huecos: si el partido local ya estaba `iniciado` o `jugado`, las anotaciones del otro móvil se ignoraban. Ahora:

- Cada gol, asistencia, tarjeta y parada lleva un `id` único (`sUid()`). Los datos antiguos reciben un id determinista al migrar (`asegurarIdsStats()`).
- Las paradas se normalizan a un registro por unidad (`n:1`) para poder sumar/restar sin ambigüedad.
- Al mezclar (`mezclarAnotaciones()`), se unen por `id`: lo que tiene un móvil y no el otro se añade. Las anotaciones borradas viajan como «tumbas» en `p.borrados` para que no reaparezcan desde una copia atrasada.
- `p.resetTs` representa un borrado total (limpiar, FF, nulo, reiniciar partido/jornada). Gana el más reciente, evitando que una copia vieja resucite un partido reiniciado.
- El resumen de sync informa de `+N anotaciones` además del resto de cambios.

El dato local sigue ganando en los campos que no son anotaciones (nombre, fotos, carta, árbitro, sanciones…), y las copas con ID distinto nunca se mezclan.

## 4. Gol doble de la carta 7 (corrección incluida en esta versión)

El cronómetro cuenta atrás desde 10 minutos (o 15 con la carta 14). `ajustarPor` sella cada gol con `min = floor((cron.mins*60 - cron.seg)/60)`: 0:00–0:59 → `min=0`, …, 4:00–4:59 → `min=4`, 5:00–5:59 → `min=5`.

**Solo cuentan doble los goles con `min < 5`** (primeros cinco minutos). Un gol a partir de 5:00 cuenta 1. El jugador siempre suma 1 gol por anotación en `calcEstadisticas()`.

Caso verificado: 2 goles antes de 5 min + 4 después → marcador **8** y estadística del jugador **6** (`smoke-nube.html`, checks `gol-doble-8` y `jugador-seis-goles`). Antes era `min <= 5` y daba 12.
