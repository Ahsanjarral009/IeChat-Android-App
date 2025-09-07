from app import app, db, User

with app.app_context():
    db.create_all()

    # Add sample users
    sample_users = [
        ("Alice", "1234"),
        ("Bob", "1234"),
        ("Charlie", "1234"),
        ("David", "1234")
    ]

    for username, password in sample_users:
        user = User.query.filter_by(username=username).first()
        if not user:
            user = User(username=username, password=password)
            db.session.add(user)

    db.session.commit()

    # Add sample public keys directly into User table
    for user in User.query.all():
        if not user.public_key:  # only if no key already
            user.public_key = f"---PUBLIC KEY FOR {user.username}---"

    db.session.commit()

    # Read all users and public keys
    for user in User.query.all():
        print(f"User: {user.username}, Password: {user.password}, Public Key: {user.public_key}")
