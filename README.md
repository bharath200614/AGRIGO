# AgriGo - Android Application

A complete Android application for farming logistics, connecting farmers with drivers for crop transportation.

## Project Structure

```
AgriGo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agrigo/
│   │   │   │   ├── activities/       # Activity classes
│   │   │   │   ├── adapters/          # RecyclerView adapters
│   │   │   │   ├── models/            # Data models
│   │   │   │   ├── firebase/          # Firebase integration
│   │   │   │   ├── network/           # Retrofit API client
│   │   │   │   ├── animations/        # Custom animations
│   │   │   │   └── utils/             # Utility classes
│   │   │   ├── res/
│   │   │   │   ├── layout/            # XML layouts
│   │   │   │   ├── drawable/          # Drawables & shapes
│   │   │   │   ├── values/            # Colors, styles, strings, dimens
│   │   │   │   ├── anim/              # Animation definitions
│   │   │   │   ├── font/              # Font families
│   │   │   │   └── xml/               # Backup & extraction rules
│   │   │   └── AndroidManifest.xml    # App manifest
│   │   ├── build.gradle               # App dependencies & build config
│   │   └── proguard-rules.pro          # ProGuard rules
│   ├── build.gradle                   # Project build config
│   └── gradle.properties              # Gradle properties
```

## WELCOME SCREEN - COMPLETED ✅

### What's Been Created:

#### 1. **Layout Files**
- `activity_welcome.xml` - Responsive welcome screen with:
  - App logo and name
  - Subtitle: "Smart Transport for Smart Farmers"
  - Farmer & truck illustration
  - Feature list with checkmarks
  - "Get Started" call-to-action button

#### 2. **Activity**
- `WelcomeActivity.java` - Main welcome screen activity with:
  - Sequential fade-in animations for logo, text, and illustrations
  - Slide-up animations for illustration and button
  - Button click handler (placeholder for login navigation)
  - Staggered animation timing (600ms - 1000ms range)

#### 3. **Animations**
- `fade_in.xml` - 1000ms fade in animation
- `slide_up_fade.xml` - Combines slide-up and fade-in (800ms)
- `slide_in_right.xml` - Slide from right with fade (600ms)
- `scale_enter.xml` - Scale animation for button press effect

#### 4. **Drawable Resources**
- `ic_agrigo_logo.xml` - Green circular logo with leaf and truck
- `ic_farmer_truck_welcome.xml` - Main illustration (farmer with truck)
- `ic_check_green.xml` - Feature checkmark icon
- `shape_circle_green_light.xml` - Decorative circle shape
- `farm_background_welcome.xml` - Green gradient background
- Button backgrounds:
  - `bg_button_green.xml` - Primary green button
  - `bg_button_yellow.xml` - Secondary yellow button
  - `bg_button_white_stroke.xml` - White outline button
  - `bg_button_green_gradient.xml` - Gradient button
  - `bg_button_green_ripple.xml` - Ripple effect button
- `bg_input_field.xml` - Input field styling
- `bg_card.xml` - Card styling

#### 5. **Resources**
- `colors.xml` - Complete color palette:
  - Primary: #2E7D32 (Farm Green) & variants
  - Secondary: #FFC107 (Yellow) & variants
  - Status colors (blue, green, orange, red)
  - Text & neutral colors
  
- `styles.xml` - Material Design theme with:
  - AppTheme (main theme)
  - WelcomeScreenTheme
  - Text styles (Heading 1-3, Body 1-2, Caption)
  - Button styles (Primary, Secondary)
  - Card styles
  
- `dimens.xml` - Dimension constants:
  - Card radius: 16dp
  - Button height: 48dp
  - Text sizes from 12sp to 32sp
  - Icon and illustration sizes
  
- `strings.xml` - All string resources for multiple screens

#### 6. **Fonts**
- Poppins font family support:
  - `poppins_regular.xml`
  - `poppins_semibold.xml`
  - `poppins_bold.xml`

#### 7. **Configuration Files**
- `AndroidManifest.xml` - App manifest with:
  - All required permissions (internet, location, camera, etc.)
  - Activity declarations (placeholder for future screens)
  - WelcomeActivity as launcher
  - Firebase & Google Maps prepared
  
- `build.gradle` - Dependencies:
  - AndroidX Core libraries
  - Material Design 3
  - Firebase (Auth, Firestore, Messaging, Analytics)
  - Google Maps & Location Services
  - Retrofit & OkHttp for API calls
  - Lottie for animations
  - Glide for image loading
  - Testing libraries

- `proguard-rules.pro` - Obfuscation rules

#### 8. **Utility Classes**
- `PreferenceManager.java` - SharedPreferences management
- `NetworkUtils.java` - Network connectivity checks
- `ToastUtils.java` - Toast message helpers

#### 9. **Model Classes**
- `User.java` - User data model (farmers & drivers)
- `Booking.java` - Booking/trip data model
- `DriverLocation.java` - GPS location tracking model

## Design Theme

### Colors
- **Primary Green**: #2E7D32 (AgriGo brand color)
- **Light Green**: #4CAF50
- **Dark Green**: #1B5E20
- **Secondary Yellow**: #FFC107
- **Status Blue**: #2196F3
- **Status Green**: #4CAF50
- **Status Orange**: #FF9800

### Typography
- **Font**: Poppins (modern, friendly)
- **Sizes**: 12sp-32sp range
- **Weights**: Regular, Semibold, Bold

### Spacing & Styling
- **Card Radius**: 16dp
- **Button Radius**: 12dp
- **Shadow Elevation**: 4-8dp
- **Padding**: 16-24dp
- **Animations**: 300-1000ms durations

## Next Steps - Ready for Implementation

### 2️⃣ Login Screen
- Phone & password input fields
- Login & Register buttons
- Role selection (Farmer/Driver)
- Use Login Activity placeholder in manifest

### 3️⃣ Registration Screen
- Full name input
- Phone number input
- Role selection
- Password & confirmation

### 4️⃣ Farmer Dashboard
- Greeting banner
- Crop selection (Paddy, Tomato, Banana, Sugarcane)
- Weight input
- Vehicle suggestion button
- Navigation drawer setup

### 5️⃣ My Bookings Screen
- RecyclerView with booking cards
- Status badges
- Booking details display

### 6️⃣ Tracking Screen
- Google Maps integration
- Driver location updates
- Driver info card
- Call & cancel buttons

### 7️⃣ Driver Dashboard
- Online/Offline toggle buttons
- Request notifications
- Status indicator

### 8️⃣ Driver Request Screen
- Request card display
- Accept/Reject buttons
- Crop info display

## How to Run

1. **Prerequisites**
   - Android Studio 2022.1 or later
   - JDK 11 or later
   - Min SDK: 24 (Android 7.0)
   - Target SDK: 34 (Android 14)

2. **Setup**
   ```bash
   git clone <repo-url>
   cd AGRIGO
   ```

3. **Firebase Setup** (Required for completion)
   - Create Firebase project
   - Add google-services.json to app/ directory
   - Enable Authentication, Firestore, Cloud Messaging

4. **Google Maps Setup** (Required for tracking)
   - Get API key from Google Cloud Console
   - Add to AndroidManifest.xml

5. **Build & Run**
   - Sync Gradle
   - Build APK
   - Run on emulator or device

## Animation Details

### Welcome Screen Animations
- **Logo**: Fade in (0ms-600ms)
- **App Name**: Fade in (200ms-800ms)
- **Subtitle**: Fade in (400ms-1000ms)
- **Illustration**: Slide up + Fade (600ms-1400ms)
- **Button**: Slide up + Fade (1000ms-1800ms)

### Button Interactions
- Press: Scale animation (0.95x → 1.0x)
- Ripple effect on click
- Smooth state transitions

## Dependencies

### Core
- androidx.appcompat:1.6.1
- androidx.constraintlayout:2.1.4
- com.google.android.material:1.11.0

### Backend
- Firebase (Auth, Firestore, Messaging)
- Retrofit 2.10.0
- OkHttp 4.11.0

### UI/UX
- Lottie 6.4.0
- Glide 4.16.0
- Google Material Design

### Location & Maps
- play-services-maps:18.2.0
- play-services-location:21.1.0

## File Statistics

- **Total Classes**: 10+ (activities, models, utils)
- **Layout Files**: 1 (ready for more)
- **Drawable Resources**: 13
- **Animation Files**: 4
- **Resource Directories**: 7
- **Configuration Files**: 5

## Notes

- All colors follow Material Design 3 guidelines
- Animations are hardware-accelerated where possible
- Code is fully documented with Javadoc comments
- Ready for Firebase integration
- Permissions organized and properly declared
- ProGuard rules configured for production builds

## Font Files

⚠️ **Note**: To complete font setup, add Poppins font files to `app/src/main/res/font/`:
- `poppins_regular_file.ttf`
- `poppins_semibold_file.ttf`
- `poppins_bold_file.ttf`

(Or system fonts will be used as fallback)

---

**Status**: Welcome Screen ✅ Complete
**Next**: Ready for Login Screen implementation
**Architecture**: Multi-activity, modular, production-ready
