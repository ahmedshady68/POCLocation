# Location POC – Android (Kotlin, Jetpack Compose, Hilt, Room, Retrofit)

This is a proof-of-concept Android app that demonstrates:

- Requesting runtime **location + notification** permissions.
- Starting a **foreground service** to continuously record GPS points during a trip.
- Opening **Google Maps navigation** externally while still recording route data in the background.
- Storing points in **Room** and uploading an **encoded polyline** + trip stats to a backend via **Retrofit**.
- Built entirely with **Kotlin + Jetpack Compose** and using **Hilt** for DI.

---

## Features

- ✅ **Foreground Service** – keeps recording even if Google Maps is open
- ✅ **Trip Recording** – logs latitude/longitude/accuracy/timestamp
- ✅ **Polyline Simplification** – reduces noise, encoded polyline for API
- ✅ **API Upload** – sends route, distance, duration, accuracy stats
- ✅ **Permissions Flow** – fine location → notifications → (optional) background location
- ✅ **Jetpack Compose UI** – simple start/end trip screen

---

## Tech Stack

- **Kotlin** + **Coroutines**
- **Jetpack Compose**
- **Hilt** (DI)
- **Room** (local storage)
- **Retrofit** + **Moshi** + **OkHttp logging**
- **Google Play Services Location**
- **Android Maps Utils** (polyline encode/simplify)
- **Foreground Service** with notification

---

## Project Structure

app/src/main/java/com/tahakom/locationpoc/
│
├── App.kt # Hilt Application
├── MainActivity.kt # Entry point (Compose)
│
├── permissions/PermissionFlow.kt # Runtime permission coordinator
├── data/ # Room DB + DAO
├── net/ # Retrofit API + DTOs
├── di/Modules.kt # Hilt modules (DB, Network, Location)
├── repo/TripRepository.kt # Core trip logic (save/upload)
└── trip/ # TripService + ExternalNav (Google Maps)
├── TripService.kt # Foreground service
└── ExternalNav.kt # Helper to open Google Maps
└── ui/TripScreen.kt # Compose UI (start/end trip)
