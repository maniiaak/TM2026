### About this project
This project uses Kotlin and Gradle to build a simple application. The application aims to enhance the 
experience of music enjoyers by allowing them to log and keep track of the music they've listened to.
Python is used serverside to handle data storage and retrieval, while the frontend is built using Kotlin to provide a seamless user interface.

### Deployement
Download the project from the repository to build it. Ensure that you have Kotlin, Gradle and Android Studio installed on your system.
The serverside runs in a containerized environment using Docker. To deploy the application, follow these steps:
1. Clone the repository to your local machine.
2. Get your client secret and client ID from the Spotify Developer Dashboard and add them into a .env file in the Serverside directory.
3. Start the server by running the Docker container.
4. In the `iluvmusic-app/composeApp/src/commonMain/kotlin/com/jetbrains/kmpapp/config/ApiConfig.kt` file, update the `BASE_URL` variable to point to your server's URL.
5. Navigate to the project directory and build the application using Gradle.
6. Access the application on your android mobile device.

### Future Enhancements
- Port the app to support iOS devices and web browsers.
- Let users import their Musicboard and RateYourMusic data to the app.
- Cleaner UI and better user experience.