from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
import json

app = Flask(__name__)

# Configure SQLite database for users only
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///chat.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(50), unique=True, nullable=False)
    password = db.Column(db.String(100), nullable=False)
    public_key = db.Column(db.Text, nullable=True)

# In-memory message storage (as in your original code)
messages = []

# --------------------
# DB INITIALIZATION (for users only)
# --------------------
with app.app_context():
    db.create_all()

@app.route('/')
def home():
    return "Chat Server with User Accounts ✅"

# Register new user
@app.route('/register', methods=['POST'])
def register():
    data = request.json
    username = data.get("username")
    password = data.get("password")
    public_key = data.get("public_key")

    if User.query.filter_by(username=username).first():
        return jsonify({"status": "error", "message": "Username already exists"}), 400

    new_user = User(username=username, password=password, public_key=public_key)
    db.session.add(new_user)
    db.session.commit()

    return jsonify({"status": "success", "message": "User registered successfully"})

# Login user
@app.route('/login', methods=['POST'])
def login():
    data = request.json
    username = data.get("username")
    password = data.get("password")
    public_key = data.get("public_key")

    user = User.query.filter_by(username=username, password=password).first()
    if user:
        # update latest public key on every login
        if public_key:
            user.public_key = public_key
            db.session.commit()
        return jsonify({"status": "success", "message": "Login successful"})
    else:
        return jsonify({"status": "error", "message": "Invalid credentials"}), 401

# Send message (using in-memory storage)
@app.route('/send', methods=['POST'])
def send_message():
    data = request.json
    sender = data.get("sender")
    receiver = data.get("receiver")
    ciphertext = data.get("ciphertext")
    nonce = data.get("nonce")
    ek = data.get("ek")
    timestamp = datetime.now().strftime("%I:%M %p")

    # Only require encrypted fields, not content
    if not all([sender, receiver, ciphertext, nonce, ek]):
        return jsonify({"status": "error", "message": "Missing required fields"}), 400

    message = {
        "sender": sender,
        "receiver": receiver,
        "ciphertext": ciphertext,
        "nonce": nonce,
        "ek": ek,
        "timestamp": timestamp
    }
    
    print("Received message:", message)
    messages.append(message)
    print("ek length:", len(ek))
    
    return jsonify({"status": "success", "message": message})
# Get users
@app.route('/users/<username>', methods=['GET'])
def get_users(username):
    all_users = User.query.filter(User.username != username).all()
    users_list = [u.username for u in all_users]
    return jsonify(users_list)

# Get messages between two users (from in-memory storage)
@app.route("/messages/<user>/<chatwith>", methods=["GET"])
def get_messages(user, chatwith):
    # Filter messages between user and chatwith from in-memory storage
    chat_history = []
    for m in messages:
        if (m["sender"] == user and m["receiver"] == chatwith) or \
           (m["sender"] == chatwith and m["receiver"] == user):
            chat_history.append({
                "sender": m["sender"],
                "receiver": m["receiver"],
                "content": m.get("content", ""),  # For backward compatibility
                "ciphertext": m.get("ciphertext"),
                "nonce": m.get("nonce"),
                "ek": m.get("ek"),
                "timestamp": m.get("timestamp")
            })
    return jsonify(chat_history)

# Upload public key
@app.route('/keys/upload', methods=['POST'])
def upload_key():
    data = request.json
    username = data.get("username")
    pem = data.get("pem")

    if not username or not pem:
        return jsonify({"status":"error","message":"missing parameters"}), 400

    user = User.query.filter_by(username=username).first()
    if not user:
        return jsonify({"status":"error","message":"user not found"}), 404

    # update or insert key
    user.public_key = pem
    db.session.commit()

    return jsonify({"status":"success"})

# Fetch public key
@app.route("/keys/<username>", methods=["GET"])
def get_key(username):
    user = User.query.filter_by(username=username).first()

    if user and user.public_key:
        return jsonify({"status": "success", "pem": user.public_key})
    
    return jsonify({"status": "error", "message": "Key not found"}), 404

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)