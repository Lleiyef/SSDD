# LlamaChat — Proyecto SSDD

**Universidad de Murcia · Grado en Ingeniería Informática · Curso 2025/2026**

Convocatoria Mayo 2026 -- SSDD

Sistema distribuido de chat con modelo de lenguaje (Llama-2), autenticación JWT, monitorización con Prometheus y registro de eventos con Kafka. Desplegado íntegramente mediante Docker Compose.

---

## Autores

- María Garcerán Madrid
- José Francisco González Mayol

## Índice

1. [Descripción del proyecto](#descripción-del-proyecto)
2. [Arquitectura](#arquitectura)
3. [Prerrequisitos](#prerrequisitos)
4. [Ejecución rápida](#ejecución-rápida)
5. [Tests](#tests)
6. [Monitorización](#monitorización)
7. [Usuarios de prueba](#usuarios-de-prueba)

---

## Descripción del proyecto

LlamaChat permite a los usuarios mantener conversaciones con el modelo de lenguaje Llama-2 a través de una interfaz web. El sistema implementa:

- Autenticación **stateless con JWT** (HMAC-SHA384) en todos los puntos de entrada REST y gRPC.
- Gestión del ciclo de vida de conversaciones con estados `READY`, `BUSY` y `FINISHED`.
- Comunicación asíncrona entre servicios mediante **gRPC** y polling.
- Registro de eventos de sesión mediante **Apache Kafka**.
- Monitorización con **Prometheus**, incluyendo una métrica de negocio personalizada.
- Validación automática con **TestClient Java** y tests **Selenium E2E**.

---

## Arquitectura

El sistema se compone de nueve servicios orquestados con Docker Compose:

| Servicio | Tecnología | Puerto |
|---|---|---|
| `ssdd-frontend` | Flask 3 + Flask-Login + prometheus-flask-exporter | 5010 |
| `backend-rest` | Java 17 + Jersey 3 (JAX-RS) + Tomcat 10.1 | 8080 |
| `backend-rest-externo` | Java 17 + Jersey 3 | 8180 |
| `backend-grpc` | Java 17 + gRPC 1.69 | 50051 / 9091 |
| `ssdd-llamachat` | Flask (modelo Llama-2) | 5020 |
| `db-mysql` | MySQL 8 | 3306 |
| `kafka` | Confluent Kafka 7.6 (KRaft) | 9092 |
| `log-service` | Python 3.11 + confluent-kafka | — |
| `prometheus` | prom/prometheus:latest | 9090 |

### Flujo de una conversación

```
Usuario → Frontend (Flask)
       → POST /u/{id}/dialogue/{name}/next/{token}  →  backend-rest
       → gRPC asíncrono                             →  backend-grpc
       → HTTP polling cada 500 ms                   →  ssdd-llamachat (Llama-2)
       ← respuesta                                  ←  backend-grpc
       ← actualiza BD + publica eventos Kafka       ←  backend-rest
       ← polling /api/poll_chat cada 2 s            ←  Frontend
```

---

## Prerrequisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución.
- Git Bash (Windows) o terminal Linux/macOS.
- Al menos **6 GB de RAM** disponibles (el modelo Llama-2 requiere ~4 GB).
- Para los tests Selenium: Python 3.9+, Google Chrome instalado.

> **Windows:** se requiere la variable `MSYS_NO_PATHCONV=1` al usar Git Bash. El Makefile la aplica automáticamente.

---

## Ejecución rápida

### 1. Compilar los módulos Java

```bash
make
```

Compila en orden: `dao`, proto de `GrpcService`, `GrpcServiceImpl` y `backend-rest`. No es necesario tener Maven instalado localmente; la compilación se realiza dentro de contenedores Maven.

### 2. Levantar el sistema

```bash
make run-devel
```

Construye las imágenes y lanza todos los servicios. La primera vez puede tardar varios minutos.

> **Nota:** LlamaChat tarda entre 30 y 60 segundos en cargar el modelo en memoria. Durante ese tiempo el endpoint `/healthcheck` devuelve `204`; cuando está listo devuelve `200`.

### 3. Acceder a la aplicación

| Servicio | URL |
|---|---|
| Frontend (chat) | http://localhost:5010 |
| Backend REST | http://localhost:8081/Service/jaxrs/... |
| Backend Externo | http://localhost:8180/ServiceExterno/jaxrs/... |
| Prometheus | http://localhost:9090 |
| MySQL | localhost:3306 (usuario: `root`, contraseña: `root`) |

### 4. Detener el sistema

```bash
docker compose -f docker-compose-devel.yml down
```

---

## Tests

### TestClient (Java)

Con el sistema arrancado, ejecuta el cliente de prueba desde el contenedor `backend-rest`:

```bash
docker compose -f docker-compose-devel.yml exec backend-rest \
  java -cp /usr/local/tomcat/webapps/Service/WEB-INF/lib/*:/usr/local/tomcat/webapps/Service/WEB-INF/classes \
  es.um.sisdist.backend.Service.TestClient http://backend-rest:8080
```

El cliente realiza el flujo completo:
1. Registro del usuario `e2euser@um.es`
2. Login y obtención de JWT
3. Creación de conversación y envío de prompt
4. Polling hasta recibir respuesta de LlamaChat
5. Cierre del diálogo (`POST /end` → estado `FINISHED`)
6. Borrado final

### Tests Selenium (E2E)

```bash
cd e2e
pip install -r requirements.txt
python selenium_test.py
```

Los tests se ejecutan en modo **headless** (sin ventana visible) y cubren:
1. Login con credenciales válidas
2. Navegación al chat y envío de prompt
3. Espera explícita (`WebDriverWait`, hasta 300 s) de la respuesta de la IA
4. Validación de que el texto de respuesta no está vacío

Para ajustar el timeout:

```bash
WAIT_TIMEOUT=300 FRONTEND_URL=http://localhost:5010 python selenium_test.py
```

---

## Monitorización

Prometheus está disponible en **http://localhost:9090** y recoge métricas cada 15 segundos de tres fuentes:

- `ssdd-frontend:5010/metrics`
- `backend-rest:8080/Service/metrics`
- `backend-grpc:9091/metrics`

### Métrica de negocio personalizada

Se expone el contador `conversations_started_total`, que se incrementa cada vez que un usuario inicia una nueva conversación. Puedes consultarla en Prometheus con la expresión:

```
conversations_started_total
```

---

## Usuarios de prueba

> ⚠️ No eliminar el usuario de test predefinido.

| Email | Contraseña |
|---|---|
| dsevilla@um.es | admin |

---
