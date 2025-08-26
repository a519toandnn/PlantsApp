# 🌱 Smart Potato Disease Detection and Soil Moisture Monitoring System

## 📖 Overview
This project aims to help farmers **detect potato late blight disease early** and **monitor soil moisture levels efficiently** in order to increase crop yield and optimize farming processes.  

The system uses a **Raspberry Pi 4 with a webcam** to capture leaf images, which are processed by an **AI server** for disease recognition.  
At the same time, a **soil moisture sensor** monitors field conditions and can automatically activate a **water pump** when needed.  

Farmers receive **real-time notifications via a mobile application** about soil status, disease alerts, and pump control options. This reduces crop losses and supports smarter agricultural practices.

---

## ⚙️ Technologies Used
- **Mobile Application**: Java (Android)  
- **Database**: Firebase Realtime Database
- **Backend Server**: Flask (acts as middleware between Firebase and Mobile App)  
- **Cloud Storage**: AWS S3 Bucket (for storing captured leaf images)  
- **Hardware**: Raspberry Pi 4, Webcam, Soil Moisture Sensor, Water Pump  
- **AI Model**: Image-based potato disease classification (Server-side)

---

## 📲 Main Features

### 1. Real-Time Soil Moisture Monitoring
- Tracks soil moisture continuously.  
- Sends **alerts when soil is too dry or too wet**.  
- Can automatically turn on/off the water pump.  

---

### 2. Potato Disease Detection (AI-powered)
- Raspberry Pi captures potato leaf images.  
- AI server processes and classifies diseases.  
- Mobile app displays **disease type and warnings**.  

---

### 3. Pump Control via Mobile App
- Users can **manually control the water pump** from the app.  
- Works in parallel with automatic irrigation.  

---

### 4. Real-Time Notifications
- Push notifications delivered instantly.  
- Works **even when the app is closed**.  
- Notifications include:  
  - Soil moisture warnings  
  - Disease detection alerts  
  - Pump activity updates  
