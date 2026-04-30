<img width="1920" height="1080" alt="APP DASHBOARD" src="https://github.com/user-attachments/assets/cd9ef76a-ed0f-4250-a500-d618fbb18f03" />SafeeTrip360: Smart Tourist Safety System 🛡️🌍
SafeeTrip360 is a proactive Android-based safety application designed for travelers. It leverages real-time location services and Geofencing to provide an automated safety net, ensuring users are alerted when they stray from safe areas or enter high-risk zones.

🚀 Key Features
Proactive Geofencing Logic: * Safe Zone (Alert on EXIT): Automatically triggers a notification if the tourist moves outside a pre-defined safe perimeter.

Danger Zone (Alert on ENTER): Sends an immediate high-priority warning if the user enters a restricted or high-risk location.

One-Tap SOS: A panic button that instantly captures live GPS coordinates.

Real-time Tracking: Continuous location monitoring using Google Play Services.

Firebase Integration: Secure user authentication and real-time data storage for emergency contacts.

🛠️ Tech Stack
Language: Kotlin

IDE: Android Studio

Database & Auth: Firebase (Firestore / Realtime Database)

APIs: Google Maps Platform & Google Geofencing API

Architecture: MVVM (Model-View-ViewModel)

📐 How the Geofencing Works
The app uses the Google Geofencing API to create virtual perimeters:

Dwell/Enter Transition: Monitors when a user enters a "Danger Zone."

Exit Transition: Monitors when a user leaves a "Safe Zone."

Broadcast Receiver: A background service that listens for these transitions even when the app is minimized, ensuring 24/7 protection.

⚙️ Setup & Installation
Clone this repository:

git clone https://github.com/priyadharshini783/smart-tourist-system.git

Open the project in Android Studio.

Note: You must add your own google-services.json file to the app/ folder to enable Firebase features.
## 📱 Application Screenshots

<p align="center">
  <img src=""C:\Users\adthi\Downloads\bdy\APP DASHBOARD.png"" width="250" />
  <img src=""C:\Users\adthi\Pictures\Screenshots\Screenshot 2026-02-17 112312.png"" width="250" /> 
  <img src=""C:\Users\adthi\Pictures\Screenshots\Screenshot 2026-02-16 203723.png"" width="250" />
</p>

Ensure Location Permissions (including Background Location) are granted on the device.







