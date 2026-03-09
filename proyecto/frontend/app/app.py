from flask import Flask, render_template, send_from_directory, url_for, request, redirect
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
from flask import render_template, request, jsonify
import requests
import os

# Usuarios
from models import users, User

# Login
from forms import LoginForm, SignupForm

app = Flask(__name__, static_url_path='')
login_manager = LoginManager()
login_manager.init_app(app) # Para mantener la sesión

# Configurar el secret_key. OJO, no debe ir en un servidor git público.
# Python ofrece varias formas de almacenar esto de forma segura, que
# no cubriremos aquí.
app.config['SECRET_KEY'] = 'qH1vprMjavek52cv7Lmfe1FoCexrrV8egFnB21jHhkuOHm8hJUe1hwn7pKEZQ1fioUzDb3sWcNK1pJVVIhyrgvFiIrceXpKJBFIn_i9-LTLBCc4cqaI3gjJJHU6kxuT8bnC7Ng'

@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/login', methods=['GET', 'POST'])
def login():
    # Si ya está dentro, al index
    if current_user.is_authenticated:
        return redirect(url_for('index'))
    
    error = None
    form = LoginForm(None if request.method != 'POST' else request.form)
    
    if request.method == "POST" and form.validate():
        # 1. Buscamos el usuario en nuestra "base de datos" (la lista users)
        # Usamos el método estático que arreglamos en models.py
        user = User.get_user(form.email.data)
        
        # 2. Si el usuario existe, comprobamos la contraseña
        # Nota: encode('utf-8') es necesario porque tu modelo usa hash sobre bytes
        if user is not None and user.check_password(form.password.data.encode('utf-8')):
            login_user(user, remember=form.remember_me.data)
            return redirect(url_for('index'))
        else:
            error = 'Credenciales inválidas. Inténtalo de nuevo.'

    return render_template('login.html', form=form,  error=error)

@app.route('/profile')
@login_required
def profile():
    return render_template('profile.html')

@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('index'))

@login_manager.user_loader
def load_user(user_id):
    for user in users:
        if user.id == int(user_id):
            return user
    return None


@app.route('/signup', methods=['GET', 'POST'])
def signup():
    # Si ya está logueado, lo mandamos al inicio
    if current_user.is_authenticated:
        return redirect(url_for('index'))

    form = SignupForm()
    error = None
    
    if request.method == "POST" and form.validate():
        # 1. Comprobar si el email ya existe en nuestra lista "falsa" de usuarios
        # (Esto se sustituirá luego por una llamada a la base de datos)
        existing_user = next((u for u in users if u.email == form.email.data), None)
        
        if existing_user:
            error = 'El email ya está registrado.'
        else:
            # 2. Crear el nuevo usuario
            # Generamos un ID simple basado en la longitud de la lista + 1
            new_id = len(users) + 1
            
            # Creamos el objeto usuario. 
            # IMPORTANTE: Encodeamos a utf-8 porque tu clase User espera bytes para el hash
            new_user = User(
                id=new_id,
                name=form.name.data,
                email=form.email.data,
                password=form.password.data.encode('utf-8')
            )
            
            # 3. Guardar en la "base de datos" (lista en memoria)
            users.append(new_user)
            
            # 4. Loguear al usuario directamente y redirigir
            login_user(new_user)
            return redirect(url_for('index'))

    return render_template('signup.html', form=form, error=error)

# 1. Ruta para mostrar la página web del chat
@app.route('/chat')
def chat_view():
    return render_template('chat.html')

# 2. Ruta que recibe el mensaje de JS y llama a tu Backend REST
@app.route('/api/send_chat', methods=['POST'])
def api_send_chat():
    data = request.get_json()
    prompt = data.get('prompt', '')

    try:
        # Flask llama a tu servidor Tomcat a través de la red interna de Docker
        # Fíjate que usamos el nombre del contenedor "backend-rest"
        url_rest = "http://backend-rest:8080/Service/jaxrs/chat"
        payload = {"prompt": prompt}
        
        # Hacemos la petición y esperamos (los 5 segundos del dummy)
        rest_response = requests.post(url_rest, json=payload)
        
        if rest_response.status_code == 200:
            # Si va bien, devolvemos el JSON al navegador
            return jsonify(rest_response.json())
        else:
            return jsonify({"response": "Error en el servidor REST"}), 500
            
    except Exception as e:
        return jsonify({"response": f"Error de conexión: {str(e)}"}), 500



if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))
