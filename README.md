# 📱 Smart Attendance Tracker App

A modern, native Android app built with **Jetpack Compose** and **Material 3** to help students track and manage their university attendance in a smart, flexible, and interactive way. Built with a focus on real-time use, schedule-based automation, and detailed analytics.

---

## ✨ **Key Features**

### 🎯 **Core Functionality**
- **📅 Daily Attendance Logging**: Mark subjects as Present/Absent/Late with intuitive swipe gestures
- **📚 Smart Subject Management**: Add, edit, delete subjects with validation and type categorization
- **📊 Real-time Analytics**: Live attendance percentage calculations with color-coded indicators
- **🎯 Goal Tracking**: Safe skip calculator and recovery class suggestions
- **📈 Attendance Forecasting**: Predictive analytics for attendance trends

### 🏗️ **Technical Excellence**
- **🚀 Modern Architecture**: MVVM + Clean Architecture with Hilt DI
- **🎨 Material 3 Design**: Beautiful, accessible UI with dynamic theming
- **⚡ Performance Optimized**: Efficient Room database with proper indexing
- **🔄 Real-time Updates**: StateFlow-based reactive programming
- **📱 Edge-to-Edge**: Modern Android UI with proper insets handling

---

## 🛠️ **Modern Tech Stack**

### **Core Technologies**
- **Language**: Kotlin with Coroutines
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Database**: Room with KSP
- **Navigation**: Navigation Compose
- **State Management**: StateFlow + Compose State

### **Development Tools**
- **Build System**: Gradle with Kotlin DSL
- **Code Processing**: KSP (Kotlin Symbol Processing)
- **Testing**: JUnit, Mockk, Compose Testing

---

## 🏗️ **Architecture Overview**

### **Clean Architecture Layers**
```
┌─────────────────────────────────────────┐
│                 UI Layer                │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │  Composables│  │   ViewModels    │   │
│  │   (Screens) │  │  (State Mgmt)   │   │
│  └─────────────┘  └─────────────────┘   │
└─────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────┐
│              Domain Layer               │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │  Use Cases  │  │   Domain Models │   │
│  │ (Bus Logic) │  │  (Entities)     │   │
│  └─────────────┘  └─────────────────┘   │
└─────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────┐
│               Data Layer                │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │ Repository  │  │   Data Sources  │   │
│  │(Abstraction)│  │  (Room, API)    │   │
│  └─────────────┘  └─────────────────┘   │
└─────────────────────────────────────────┘
```

### **Project Structure**
```
com.example.lol/
├── data/                    # Data models and enums
├── db/                      # Room database entities and DAOs
├── di/                      # Dependency injection modules
├── navigation/              # Navigation setup
├── repository/              # Repository implementations
├── ui/
│   ├── components/          # Reusable UI components
│   ├── screens/             # Screen composables
│   └── theme/               # Material 3 theming
├── utils/                   # Utility classes and helpers
└── viewmodel/               # ViewModels with state management
```

---

## 🎨 **UI/UX Design Principles**

### **Material 3 Design System**
- **Dynamic Color**: Adaptive theming based on user preferences
- **Typography Scale**: Consistent text styling throughout the app
- **Motion Design**: Smooth animations and transitions
- **Accessibility**: Full screen reader and keyboard navigation support

### **User Experience Focus**
- **Intuitive Interactions**: Swipe gestures for quick attendance marking
- **Visual Feedback**: Haptic feedback and color-coded status indicators
- **Responsive Design**: Works seamlessly on different screen sizes
- **Error Handling**: Clear error messages with recovery suggestions

---

## 🚀 **Getting Started**

### **Prerequisites**
- **Java**: JDK 17 (LTS)
- **Android Studio**: Latest version
- **Kotlin**: Latest stable version
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Gradle**: Latest version

### **Setup Instructions**

1. **Ensure Java 17 is Installed**
```bash
java -version
```

2. **Clone the Repository**
```bash
git clone https://github.com/yourusername/attendance-tracker.git
cd attendance-tracker
```

3. **Open in Android Studio**
- Open Android Studio
- Select "Open an existing project"
- Navigate to the cloned directory

4. **Build and Run**
```bash
./gradlew clean assembleDebug
```

---

## 🧪 **Testing Strategy**

### **Unit Tests**
- **ViewModels**: State management and business logic
- **Repositories**: Data operations and caching
- **Utilities**: Calculation accuracy and edge cases

### **Integration Tests**
- **Database**: Room operations and migrations
- **Navigation**: Screen transitions and state preservation

### **UI Tests**
- **User Flows**: Complete attendance marking workflow
- **Accessibility**: Screen reader and keyboard navigation

---

## 📱 **App Workflow**

### **User Journey**
1. **Setup**: Add subjects with weekly schedules
2. **Daily Use**: Open app to see today's classes
3. **Mark Attendance**: Swipe or tap to mark Present/Absent
4. **Monitor**: Check real-time attendance percentages
5. **Plan**: Use safe skip calculator for planning
6. **Analyze**: Review trends and patterns

---

## 📄 **License**

This project is licensed under the MIT License.


