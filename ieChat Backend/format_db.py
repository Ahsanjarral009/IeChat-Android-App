from app import app, db, User

with app.app_context():
    # Clear only public keys from all users
    users = User.query.all()
    for user in users:
        user.public_key = None

    # Commit changes
    db.session.commit()

print("✅ All public keys cleared from users (users remain intact).")
