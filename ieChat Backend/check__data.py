from app import app, db, User
with app.app_context():
    users = User.query.all()

    if not users:
        print("No users found in the database.")
    else:
        for user in users:
            print(f"User ID: {user.id}, Username: {user.username}, Password: {user.password}")
            
            if user.public_key:
                
                print(f"   Public Key PEM:\n{user.public_key}")
            else:
                print("   No public key associated with this user.")
