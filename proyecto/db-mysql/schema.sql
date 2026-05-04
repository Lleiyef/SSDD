CREATE SCHEMA IF NOT EXISTS ssdd;
USE ssdd;

CREATE TABLE IF NOT EXISTS users(
	id varchar(50),
       	email varchar(50),
	password_hash text,
       	name text,
	token text,
	visits int,
	PRIMARY KEY(id)
);

-- Para búsquedas con email
CREATE INDEX user_email_idx ON users (email);

-- CUIDADO!! AÑADO UN USUARIO PARA PROBAR, PASSWORD: "admin"
INSERT INTO users VALUES ("dsevilla", "dsevilla@um.es", "21232f297a57a5a743894a0e4a801fc3", "diego", "TOKEN", 0);

CREATE TABLE IF NOT EXISTS dialogues(
    id varchar(36) NOT NULL,
    user_id varchar(50) NOT NULL,
    name varchar(255) NOT NULL,
    status ENUM('READY','BUSY','FINISHED') NOT NULL DEFAULT 'READY',
    next_token varchar(36),
    created_at BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX dialogue_user_idx ON dialogues (user_id);
CREATE UNIQUE INDEX dialogue_user_name_idx ON dialogues (user_id, name);

CREATE TABLE IF NOT EXISTS messages(
    id varchar(36) NOT NULL,
    dialogue_id varchar(36) NOT NULL,
    prompt TEXT NOT NULL,
    answer TEXT,
    timestamp BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(dialogue_id) REFERENCES dialogues(id) ON DELETE CASCADE
);

CREATE INDEX message_dialogue_idx ON messages (dialogue_id);

