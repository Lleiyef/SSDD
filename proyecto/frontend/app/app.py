import os
import time
import base64
import json

from flask import Flask, render_template, send_from_directory, url_for, request, redirect, session, jsonify
from flask_login import LoginManager, current_user, login_user, login_required, logout_user
import requests

from prometheus_flask_exporter import PrometheusMetrics
from prometheus_client import Counter

from models import User
from forms import LoginForm, SignupForm

app = Flask(__name__, static_url_path='')
app.config['SECRET_KEY'] = 'qH1vprMjavek52cv7Lmfe1FoCexrrV8egFnB21jHhkuOHm8hJUe1hwn7pKEZQ1fioUzDb3sWcNK1pJVVIhyrgvFiIrceXpKJBFIn_i9-LTLBCc4cqaI3gjJJHU6kxuT8bnC7Ng'

PrometheusMetrics(app)
conversations_started = Counter(
    'conversations_started_total',
    'Total de conversaciones iniciadas'
)

login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'login'

REST_SERVER = os.environ.get('REST_SERVER', 'backend-rest')

def backend_url(path):
    return f"http://{REST_SERVER}:8080/Service/jaxrs{path}"

def auth_headers():
    return {"Authorization": f"Bearer {session.get('jwt', '')}"}

def decode_jwt_payload(token):
    try:
        payload = token.split('.')[1]
        payload += '=' * (4 - len(payload) % 4)
        return json.loads(base64.urlsafe_b64decode(payload))
    except Exception:
        return {}


@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

@app.route('/')
def index():
    return render_template('index.html')

@login_manager.user_loader
def load_user(user_id):
    jwt = session.get('jwt')
    if not jwt:
        return None
    return User(
        user_id=session.get('user_id', user_id),
        email=session.get('user_email', ''),
        name=session.get('user_name', ''),
        jwt_token=jwt
    )

@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('index'))

    error = None
    form = LoginForm(None if request.method != 'POST' else request.form)

    if request.method == 'POST' and form.validate():
        try:
            r = requests.post(
                backend_url('/login'),
                json={'email': form.email.data, 'password': form.password.data},
                timeout=5
            )
            if r.status_code == 200:
                token = r.json().get('token', '')
                claims = decode_jwt_payload(token)
                user_id = claims.get('sub', '')
                email = claims.get('email', form.email.data)

                ur = requests.get(
                    backend_url(f'/u/{user_id}'),
                    headers={'Authorization': f'Bearer {token}'},
                    timeout=5
                )
                name = ur.json().get('name', email) if ur.status_code == 200 else email

                session['jwt'] = token
                session['user_id'] = user_id
                session['user_email'] = email
                session['user_name'] = name

                user = User(user_id=user_id, email=email, name=name, jwt_token=token)
                login_user(user, remember=form.remember_me.data)
                return redirect(url_for('index'))
            else:
                error = 'Credenciales inválidas. Inténtalo de nuevo.'
        except requests.exceptions.ConnectionError:
            error = 'No se puede conectar con el servidor.'

    return render_template('login.html', form=form, error=error)

@app.route('/signup', methods=['GET', 'POST'])
def signup():
    if current_user.is_authenticated:
        return redirect(url_for('index'))

    form = SignupForm()
    error = None

    if request.method == 'POST' and form.validate():
        try:
            r = requests.post(
                backend_url('/signup'),
                json={
                    'email': form.email.data,
                    'name': form.name.data,
                    'password': form.password.data
                },
                timeout=5
            )
            if r.status_code == 201:
                lr = requests.post(
                    backend_url('/login'),
                    json={'email': form.email.data, 'password': form.password.data},
                    timeout=5
                )
                if lr.status_code == 200:
                    token = lr.json().get('token', '')
                    claims = decode_jwt_payload(token)
                    user_id = claims.get('sub', '')
                    email = claims.get('email', form.email.data)
                    name = form.name.data

                    session['jwt'] = token
                    session['user_id'] = user_id
                    session['user_email'] = email
                    session['user_name'] = name

                    user = User(user_id=user_id, email=email, name=name, jwt_token=token)
                    login_user(user)
                return redirect(url_for('index'))
            elif r.status_code == 409:
                error = 'El email ya está registrado.'
            else:
                error = f'Error al registrar ({r.status_code}).'
        except requests.exceptions.ConnectionError:
            error = 'No se puede conectar con el servidor.'

    return render_template('signup.html', form=form, error=error)

@app.route('/logout')
@login_required
def logout():
    session.pop('jwt', None)
    session.pop('user_id', None)
    session.pop('user_email', None)
    session.pop('user_name', None)
    session.pop('dialogue_name', None)
    logout_user()
    return redirect(url_for('index'))

@app.route('/profile')
@login_required
def profile():
    return render_template('profile.html')

@app.route('/chat')
@login_required
def chat_view():
    user_id = session.get('user_id')
    if 'dialogue_name' not in session:
        dname = f"chat-{int(time.time())}"
        try:
            requests.post(
                backend_url(f'/u/{user_id}/dialogue'),
                json={'name': dname},
                headers=auth_headers(),
                timeout=5
            )
        except Exception:
            pass
        session['dialogue_name'] = dname
        conversations_started.inc()
    return render_template('chat.html')

@app.route('/api/send_chat', methods=['POST'])
@login_required
def api_send_chat():
    data = request.get_json()
    prompt = (data.get('prompt') or '').strip()
    if not prompt:
        return jsonify({'error': 'Mensaje vacío'}), 400

    user_id = session.get('user_id')
    dname = session.get('dialogue_name')

    try:
        dr = requests.get(
            backend_url(f'/u/{user_id}/dialogue/{dname}'),
            headers=auth_headers(), timeout=5
        )
        if dr.status_code != 200:
            return jsonify({'error': 'No se pudo acceder al diálogo'}), 500

        dialogue = dr.json()
        next_token = dialogue.get('nextToken')
        if dialogue.get('status') != 'READY':
            return jsonify({'error': 'El diálogo está ocupado'}), 409

        pr = requests.post(
            backend_url(f'/u/{user_id}/dialogue/{dname}/next/{next_token}'),
            json={'prompt': prompt, 'timestamp': int(time.time() * 1000)},
            headers=auth_headers(), timeout=5
        )

        if pr.status_code == 201:
            return jsonify({'ok': True})
        elif pr.status_code == 204:
            return jsonify({'error': 'Diálogo ocupado'}), 409
        else:
            return jsonify({'error': f'Error {pr.status_code}'}), 500

    except requests.exceptions.Timeout:
        return jsonify({'error': 'Timeout conectando con el backend'}), 504
    except requests.exceptions.ConnectionError:
        return jsonify({'error': 'No se puede conectar con el backend'}), 503

@app.route('/api/poll_chat')
@login_required
def api_poll_chat():
    user_id = session.get('user_id')
    dname = session.get('dialogue_name')

    try:
        r = requests.get(
            backend_url(f'/u/{user_id}/dialogue/{dname}'),
            headers=auth_headers(), timeout=5
        )
        if r.status_code != 200:
            return jsonify({'status': 'error'}), 500

        dialogue = r.json()
        status = dialogue.get('status')
        messages = dialogue.get('messages') or []

        if status == 'READY' and messages:
            last = messages[-1]
            answer = last.get('answer') or ''
            if answer:
                return jsonify({'status': 'READY', 'answer': answer})

        return jsonify({'status': status or 'BUSY'})

    except Exception:
        return jsonify({'status': 'error'}), 503


@app.route('/logs')
@login_required
def logs_view():
    user_id = session.get('user_id')
    try:
        r = requests.get(
            backend_url(f'/u/{user_id}/dialogue'),
            headers=auth_headers(), timeout=5
        )
        dialogues = r.json() if r.status_code == 200 else []
    except Exception:
        dialogues = []
    return render_template('logs.html', dialogues=dialogues)

@app.route('/logs/<dname>')
@login_required
def logs_detail(dname):
    user_id = session.get('user_id')
    try:
        r = requests.get(
            backend_url(f'/u/{user_id}/dialogue/{dname}'),
            headers=auth_headers(), timeout=5
        )
        dialogue = r.json() if r.status_code == 200 else None
    except Exception:
        dialogue = None
    if dialogue is None:
        return redirect(url_for('logs_view'))
    return render_template('logs_detail.html', dialogue=dialogue)

@app.route('/logs/<dname>/delete', methods=['POST'])
@login_required
def logs_delete(dname):
    user_id = session.get('user_id')
    try:
        requests.delete(
            backend_url(f'/u/{user_id}/dialogue/{dname}'),
            headers=auth_headers(), timeout=5
        )
    except Exception:
        pass
    if session.get('dialogue_name') == dname:
        session.pop('dialogue_name', None)
    return redirect(url_for('logs_view'))

@app.route('/stats')
@login_required
def stats_view():
    prometheus = os.environ.get('PROMETHEUS_URL', 'http://prometheus:9090')

    def prom_query(q):
        try:
            r = requests.get(f'{prometheus}/api/v1/query', params={'query': q}, timeout=3)
            if r.status_code == 200:
                result = r.json().get('data', {}).get('result', [])
                if result:
                    return result[0].get('value', [None, 'N/D'])[1]
        except Exception:
            pass
        return 'N/D'

    metrics = {
        'conversations_total': prom_query('conversations_started_total'),
        'avg_latency':         prom_query(
            'rate(http_server_requests_seconds_sum{uri=~"/jaxrs/.*"}[5m])'
            ' / rate(http_server_requests_seconds_count{uri=~"/jaxrs/.*"}[5m])'
        ),
        'active_dialogues':    prom_query('sum(ssdd_active_dialogues)'),
    }
    return render_template('stats.html', metrics=metrics)


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))
