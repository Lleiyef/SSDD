from flask_login import UserMixin

class User(UserMixin):
    def __init__(self, user_id, email, name, jwt_token):
        self.id = user_id
        self.email = email
        self.name = name
        self.jwt_token = jwt_token
