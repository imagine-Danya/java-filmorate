# java-filmorate
Template repository for Filmorate project.
![Основные сущности — users (пользователи) и films (фильмы) — связаны между собой через таблицы-связки likes (лайки) 
и friendships (дружба),что реализует отношения «многие-ко-многим» без дублирования данных. 
Фильмы дополнительно связаны со справочниками mpa_ratings (возрастные рейтинги) и genres (жанры) 
через таблицу film_genres, благодаря чему один фильм может иметь несколько жанров, 
а один жанр — принадлежать многим фильмам.](src/main/java/My%20First%20Board.jpg)

Примеры запросов:

ПОЛУЧЕНИЕ ВСЕХ ПОЛЬЗОВАТЕЛЕЙ:

SELECT user_id, email, login, name, birthday
FROM users
ORDER BY user_id;

ПОЛУЧЕНИЕ ВСЕХ ФИЛЬМОВ С ЖАНРАМИ И РЕЙТИНГОМ:

SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
m.name AS mpa_rating,
STRING_AGG(g.name, ', ') AS genres
FROM films f
LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id
LEFT JOIN film_genres fg ON f.film_id = fg.film_id
LEFT JOIN genres g ON fg.genre_id = g.genre_id
GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, m.name
ORDER BY f.film_id;

ПОЛУЧЕНИЕ ПОЛЬЗОВАТЕЛЯ ПО ID:

SELECT user_id, email, login, name, birthday
FROM users
WHERE user_id = 1;

ПОЛУЧЕНИЕ ФИЛЬМА ПО ID:

SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
m.name AS mpa_rating
FROM films f
LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id
WHERE f.film_id = 1;