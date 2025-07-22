# Сервис регистрации и авторизации

### Настройка
Для запуска требуется создать файл jwt-secret в корне проекта с токеном jwt.

### Эндпоинты
- POST `/register`
- POST `/login`
- POST `/refresh` - обновление accessToken по refreshToken
- POST `/logout` - добавление refreshToken в черный список для невозможности обновления accessToken

### База данных
Настройка подключения в application.properties, по дефолту выбран постгрес в качестве базы.

### Тесты

Запуск тестов с помощью 

```bash
./gradlew test
```

