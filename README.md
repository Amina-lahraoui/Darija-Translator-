# Darija Translator Project

This project is about Darija Translation using a backend API as our server and experience it using many client: Chrome extension, Python client, mobile application, PHP client.

## Project Tree

```text
miniproject2/
├─ backend/                 # In this backend I used Jakarta EE REST backend and WildFly WAR
├─ chrome-extension/        # Chrome side panel extension client
├─ mobile/                  # Expo React Native client application
├─ python-client/           # CLI Python client
├─ php-client/              # CLI PHP client
├─ Diagrams                   # UML 
├─ .gitignore
└─ README.md

```
## System Architecture:
-The user or client will send a source text to the backend endpoint POST/translator/translate.

-Then using Jakarta ee Basic authentication “ HTTP Basic auth the backend will check it.

-Then the Backend will call the LLM API which Groq returning the sourceText, translatedText, and model.

-After that we have API Backend.

-Then the base URL which is local “http://localhost:8080/translator-service/api.

-Then the health endpoint will be cheked via GET/about.

-Finally Translation of endpoint via POST /translator/translate where the auth is needed.

## Example body:
{
  "text": "Hello, Welcome to Morocco. ?"
}
## Run Instructions
**1) Backend**
```text
cd backend
mvn clean package
```
- Deploy backend/target/translator-service.war to WildFly, then test either using:
http://localhost:8080/translator-service/api/about
http://127.0.0.1:8080/translator-service/api/about
**2) Mobile App**
  **2) Mobile**
```text
cd  mobile
npm install
npx expo start --web -c
```

- Use in app:

-API base URL: http://127.0.0.1:8080/translator-service/api or http://localhost:8080/translator-service/api/about

-Username: DarijaTranslator

-Password: Morocco

**3) Chrome Extension**

- Open chrome://extensions
- Enable Developer mode
- Click Load unpacked
- Select the chrome-extension/ folder
- Configure API URL and credentials in the side panel
**4) Python Client**
```text 
cd python-client
python -m pip install -r requirements.txt
python translate_client.py "Hello, welcome to Morocco?"
```

**5) PHP Client**
```text
cd php-client
php translate.php "Hello, welcome to Morocco?"
```

## Security 
in config file, only the placeholders are commited because the API key can be only in environment variables without being commited. 

