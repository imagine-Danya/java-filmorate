CREATE TABLE IF NOT EXISTS users (
    user_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email     VARCHAR(255) NOT NULL UNIQUE,
    login     VARCHAR(100) NOT NULL UNIQUE,
    name      VARCHAR(100),
    birthday  DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS mpa_ratings (
    mpa_rating_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(10) NOT NULL UNIQUE,
    description   VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS genres (
    genre_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name     VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS films (
    film_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(500),
    release_date  DATE NOT NULL,
    duration      INT NOT NULL CHECK (duration > 0),
    mpa_rating_id BIGINT NOT NULL REFERENCES mpa_ratings(mpa_rating_id)
);

CREATE TABLE IF NOT EXISTS film_genres (
    film_id  BIGINT NOT NULL REFERENCES films(film_id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres(genre_id),
    PRIMARY KEY (film_id, genre_id)
);

CREATE TABLE IF NOT EXISTS friendships (
    user_id   BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, friend_id),
    CHECK (user_id <> friend_id)
);

CREATE TABLE IF NOT EXISTS likes (
    film_id BIGINT NOT NULL REFERENCES films(film_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    PRIMARY KEY (film_id, user_id)
);

ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1;
ALTER TABLE mpa_ratings ALTER COLUMN mpa_rating_id RESTART WITH 1;
ALTER TABLE genres ALTER COLUMN genre_id RESTART WITH 1;
ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1;