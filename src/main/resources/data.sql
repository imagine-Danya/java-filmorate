MERGE INTO mpa_ratings (mpa_rating_id, name, description) KEY (mpa_rating_id) VALUES
(1, 'G', 'У фильма нет возрастных ограничений'),
(2, 'PG', 'Детям рекомендуется смотреть фильм с родителями'),
(3, 'PG-13', 'Детям до 13 лет просмотр не желателен'),
(4, 'R', 'Лицам до 17 лет просматривать фильм только в сопровождении одного из родителей'),
(5, 'NC-17', 'Лицам до 18 лет просмотр запрещён');

MERGE INTO genres (genre_id, name) KEY (genre_id) VALUES
(1, 'Комедия'),
(2, 'Драма'),
(3, 'Мультфильм'),
(4, 'Триллер'),
(5, 'Документальный'),
(6, 'Боевик');

MERGE INTO directors (director_id, name, surname) KEY (director_id) VALUES
(1, 'Иван', 'Иванов'),
(2, 'Петр', 'Петров');

MERGE INTO films (film_id, name, description, release_date, duration, mpa_rating_id) KEY (film_id) VALUES
(1, 'Film 1', 'Description 1', '2089-04-17', 120, 1),
(2, 'Film 2', 'Description 2', '1999-05-20', 100, 2),
(3, 'New film with director', 'Film with director', '1999-04-30', 120, 3);

MERGE INTO film_genres (film_id, genre_id) KEY (film_id, genre_id) VALUES
(1, 1),
(1, 3),
(2, 2),
(3, 1);

MERGE INTO film_directors (film_id, director_id) KEY (film_id, director_id) VALUES
(3, 1);

MERGE INTO users (user_id, email, login, name, birthday) KEY (user_id) VALUES
(1, 'user1@example.com', 'user1', 'User One', '1990-01-01'),
(2, 'user2@example.com', 'user2', 'User Two', '1991-02-02'),
(3, 'user3@example.com', 'user3', 'User Three', '1992-03-03');

MERGE INTO friendships (user_id, friend_id, status) KEY (user_id, friend_id) VALUES
(1, 2, 'CONFIRMED'),
(2, 1, 'CONFIRMED'),
(1, 3, 'CONFIRMED'),
(3, 1, 'CONFIRMED');

ALTER TABLE films ALTER COLUMN film_id RESTART WITH 4;
ALTER TABLE users ALTER COLUMN user_id RESTART WITH 4;
ALTER TABLE reviews ALTER COLUMN review_id RESTART WITH 1;
ALTER TABLE events ALTER COLUMN event_id RESTART WITH 1;