@echo off
REM Fix all drawable XML files with SVG attribute errors
REM Replace with simple but valid Android vector drawables

cd /d "%~dp0app\src\main\res\drawable"

REM Simple placeholder drawables
for %%F in (ic_auto_vehicle.xml ic_crop_banana.xml ic_crop_paddy.xml ic_crop_sugarcane.xml ic_crop_tomato.xml ic_driver_role.xml ic_farmer_role.xml ic_mini_truck.xml ic_lorry.xml ic_farmer_truck_welcome.xml ic_back_arrow.xml ic_bookings.xml ic_check_green.xml ic_eye_hidden.xml ic_eye_visible.xml ic_help.xml ic_home.xml ic_logout.xml ic_menu.xml ic_settings.xml ic_track.xml) do (
  if exist %%F (
    (
      echo ^<?xml version="1.0" encoding="utf-8"^?^>
      echo ^<vector xmlns:android="http://schemas.android.com/apk/res/android"
      echo     android:width="48dp"
      echo     android:height="48dp"
      echo     android:viewportWidth="48"
      echo     android:viewportHeight="48"^>
      echo     ^<path
      echo         android:fillColor="#4CAF50"
      echo         android:pathData="M4,4 L44,4 L44,44 L4,44 Z" /^>
      echo ^</vector^>
    ) > %%F
    echo Fixed %%F
  )
)

echo Done fixing drawable files
