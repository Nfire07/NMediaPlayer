@echo off
echo ============================
echo 1. Building Vue project...
echo ============================
call npm run build || goto error

echo ============================
echo 2. Copying to Android...
echo ============================
call npx cap copy android || goto error

echo ============================
echo 3. Opening Android Studio...
echo ============================
call npx cap sync android || goto error
call npx cap open android || goto error

echo ============================
echo  Build Completed Succesfully
echo ============================
exit /b

:error
echo ============================
echo  Error during build
echo ============================
pause
exit /b 1
