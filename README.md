prerequisites:
- Java 25
- GROQ apikey generated from https://console.groq.com/ and added to environment variable

Steps to run:
- Import the pom.xml
- mvn clean install
- start the GroqDemoApplication
- Hit any prompt in your browser 
    Example: http://localhost:8080/api/groq/ask?prompt=what%20is%20chatgpt


Groq Models:
- You can find the groq model accessible to you on your groq logged-in portal
<img width="295" height="500" alt="image" src="https://github.com/user-attachments/assets/8fb92839-b0a5-42d2-91a0-896df718fe9c" />


This is the way to call promptEngine end point and response content is OTHER when prompt is irrelevant


![img.png](img.png)

This is the way to call promptEngine end point and response content is Technical when prompt is relevant

![img_1.png](img_1.png)
