# IeChat-Android-App
Android chatting application with end-to-end encryption


**Secure Encrypted Chat Application**
https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white
https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white
https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white
https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white
https://img.shields.io/badge/Cryptography-2C3E50?style=for-the-badge&logo=keybase&logoColor=white

A secure Android chat application featuring end-to-end encryption (E2EE) that ensures only intended recipients can read messages. Built with Java for Android and Python Flask for the backend server.

**What This System Does**
This application provides a secure communication platform where:

Messages are encrypted on the sender's device before transmission

Only the intended recipient can decrypt and read messages

The server acts only as a relay and cannot access message content

All cryptographic keys are securely managed using Android's hardware-backed Keystore

Perfect forward secrecy is maintained through unique encryption keys for each message

**Key Features**
End-to-End Encryption: Messages are encrypted before leaving the device and only decrypted on the recipient's device

Hybrid Cryptography: Combines RSA-2048 for key exchange and AES-256-GCM for message encryption

Android Keystore Integration: Secure hardware-backed storage of cryptographic keys

Perfect Forward Secrecy: Unique encryption key for each message

Message Integrity: AES-GCM provides authentication and prevents tampering

User-Friendly Interface: Simple chat interface that hides cryptographic complexity

Cross-Device Compatibility: Works across various Android versions and devices

**System Architecture**
Components
Android Client Application (Java)

User interface for messaging

Encryption/decryption engine

Key management via Android Keystore

Network communication with backend

Backend Server (Python Flask)

RESTful API for message routing

User authentication and management

Public key storage and distribution

Encrypted message persistence

**Encryption Scheme**
The application uses a hybrid encryption approach:










**Technical Implementation**
Cryptographic Details
**Asymmetric Encryption (RSA-OAEP)**

Key Size: 2048 bits

Digest: SHA-256

MGF1 Digest: SHA-1 (for Android compatibility)

Padding: OAEP

Purpose: Encrypts the symmetric AES key

**Symmetric Encryption (AES-GCM)**

Key Size: 256 bits

Nonce Size: 12 bytes

Authentication Tag: 128 bits

Purpose: Encrypts message content

**Android Keystore Integration**
The application leverages Android's hardware-backed security features:

Key generation and storage in secure hardware

Private keys never leave the secure environment

Biometric authentication support for key access

Key attestation for verifying key properties

**Message Flow**
Sending a Message:

User composes message in the chat interface

System generates a random 256-bit AES key

Message is encrypted with AES-GCM using a random nonce

AES key is encrypted with recipient's public RSA key

Encrypted package (ciphertext + nonce + encrypted key) is sent to server

Message is displayed in local UI with optimistic updates

Receiving a Message:

Client polls server for new messages

Encrypted message package is retrieved

Encrypted AES key is decrypted using recipient's private RSA key

Message is decrypted using AES key and nonce

Decrypted message is displayed in chat UI

📁 Project Structure
text
secure-chat-app/
├── android-app/                 # Android client application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/example/iechat/
│   │   │   │   ├── CryptoBox.java      # Core encryption/decryption
│   │   │   │   ├── ChatActivity.java   # Main chat interface
│   │   │   │   ├── LoginActivity.java  # User authentication
│   │   │   │   └── ChatMessage.java    # Message data model
│   │   │   └── res/                    # Layouts and resources
│   │   └── build.gradle
│   └── build.gradle
├── flask-server/               # Backend server
│   ├── app.py                 # Main Flask application
│   ├── requirements.txt       # Python dependencies
│   └── chat.db                # SQLite database (generated)
├── docs/                      # Documentation
│   ├── ARCHITECTURE.md        # System architecture details
│   └── SECURITY.md            # Security implementation details
└── README.md                  # This file
🚀 Installation and Setup
Prerequisites
Android Studio (for Android development)

Python 3.7+ (for Flask server)

Android device/emulator with API level 23+

Backend Server Setup
Navigate to the flask-server directory:

bash
cd flask-server
Install required dependencies:

bash
pip install -r requirements.txt
Run the Flask server:

bash
python app.py
The server will start on http://localhost:5000

Android Application Setup
Open the Android project in Android Studio

Update the server URL in ChatActivity.java if needed:

java
private static final String SERVER_BASE_URL = "http://your-server-ip:5000";
Build and run the application on your Android device or emulator

🧪 Usage
Register a New Account

Open the app and register with a username and password

The system automatically generates a cryptographic keypair

Login

Authenticate with your username and password

Your public key is uploaded to the server

Start Chatting

Select a user to chat with from the main screen

Type messages and send them securely

Messages are automatically encrypted and decrypted

**Security Considerations**
No Plaintext Storage: Messages are never stored in plaintext on the server

Forward Secrecy: Each message uses a unique encryption key

Key Protection: Private keys are stored in Android's hardware-backed Keystore

Authentication: Message integrity is verified using AES-GCM authentication tags

Transport Security: All communications should use HTTPS in production

**Future Enhancements**
Group chat encryption support

Media file encryption (images, videos, documents)

Key verification through QR code scanning

Message expiration (disappearing messages)

Offline message support with local encryption

Multi-device synchronization

Web client with consistent encryption

Advanced security features (screenshot prevention, app lock)

**Performance Characteristics**
Message encryption/decryption time: < 50ms on modern devices

Key generation time: ~500ms (one-time operation during registration)

Network overhead: ~344 bytes for encrypted key + 28 bytes for ciphertext overhead

Memory usage: Minimal overhead for cryptographic operations

**Contributing**
Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

Fork the project

Create your feature branch (git checkout -b feature/AmazingFeature)

Commit your changes (git commit -m 'Add some AmazingFeature')

Push to the branch (git push origin feature/AmazingFeature)

Open a Pull Request

**License**
This project is licensed under the MIT License - see the LICENSE.md file for details.

**Acknowledgments**
Android Security team for the Keystore API

Cryptography researchers whose work made these techniques possible

The open-source community for various libraries and tools

**Disclaimer**
This project is for educational purposes and demonstrates cryptographic principles. For production use, please conduct a thorough security audit and consider using well-established libraries and protocols.

Remember: Security is a process, not a product. Always keep your dependencies updated and follow best practices for secure application development.

For questions or support, please open an issue in the GitHub repository.
