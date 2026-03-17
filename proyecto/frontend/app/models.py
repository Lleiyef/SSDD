from flask_login import UserMixin
import hashlib

users = []

class User(UserMixin):

    def __init__(self, id, name, email, password, is_admin=False):
        self.id = id
        self.name = name
        self.email = email
        self.password = hashlib.sha256(password).hexdigest()
        self.is_admin = is_admin

    def set_password(self, password):
        self.password = hashlib.sha256(password).hexdigest()

    def check_password(self, password):
        return self.password == hashlib.sha256(password).hexdigest()

    def get_user(email):
        for user in users:
            if user.email == email:
                return user
        return None

    def __repr__(self):
        return '<User {}>'.format(self.email)

# Usuario admin por defecto para usarlo en pruebas
admin_user = User(
    id=1,
    name="Admin",
    email="admin@admin.com",
    password="admin".encode('utf-8'),
    is_admin=True
)
users.append(admin_user)
