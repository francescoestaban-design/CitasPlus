# CitasPlus (CitaPluus)

App Android en Java para gestionar citas médicas, encontrar centros de salud y farmacias cercanos y consultar información de medicamentos.

## Funcionalidades

- **Registro e inicio de sesión** de usuarios con Firebase Authentication.
- **Agendar citas** y consultar el **historial de citas**, guardadas en Cloud Firestore.
- **Recordatorios de cita** mediante notificaciones.
- **Mapa de centros de salud y farmacias** cercanos con Google Maps y Places, con orden por tipo o por distancia.
- **Favoritos**: guarda tus centros y farmacias habituales.
- **Medicamentos**: consulta de fichas e información usando la API CIMA de la AEMPS (con modo simulado para desarrollo).
- **Perfil** del usuario y **ajustes** (tema claro, oscuro o del sistema).

## Tecnologías

- Java 17, Android (minSdk 24, targetSdk 34), Gradle
- Firebase: Authentication, Firestore, Storage, Cloud Messaging
- Google Maps, Places y Location
- Retrofit, OkHttp y Gson para las llamadas a APIs
- ViewBinding y Material Components

## Estructura

```
app/src/main/java/com/francesco/citapluus/
├── ui/      Pantallas (login, perfil, mapa, historial, medicamentos…)
├── data/    Repositorios y acceso a Firebase
├── net/     Cliente de red, DTOs y repositorios (CIMA, Places, mock)
└── …        Favoritos, ajustes y gestión de citas
```

## Cómo ejecutarlo

1. Clona el repositorio y ábrelo con **Android Studio**.
2. Crea tu propio proyecto en [Firebase](https://console.firebase.google.com/) y descarga tu `google-services.json` en `app/`.
3. Crea tus claves de Google Maps y Places y añádelas en `app/src/main/res/values/google_maps_api.xml`.
4. Sincroniza Gradle y ejecuta la app en un emulador o dispositivo.

La compilación **debug** usa datos simulados (`USE_MOCK = true`); la **release** usa los servicios reales.

>