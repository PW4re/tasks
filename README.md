Кэширующий DNS-сервер
Выполнял Постовалов Егор, КН-203

Разбор пакетов производится без использования сторонних библиотек. 
В кэше пакеты хранятся в разобранном виде (только секции answer, authority и additional).
При необходимости отправки такого пакета клиенту осуществляется сборка в байтовый массив.
В отдельном потоке работает проверка ttl.
Сериализация сущности Storage тоже присутствует.

    Примеры запуска: java -jar Server.jar
