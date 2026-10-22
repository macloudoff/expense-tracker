# Expense Tracker

CLI для учёта расходов с хранением в CSV. Требуются Java 25 и Maven.

## Сборка и запуск

```sh
mvn package
./expense-tracker --help
```

Сборка запускает тесты и создаёт `target/expense-tracker.jar` с зависимостями.
Скрипт использует Java из `JAVA_HOME`, если эта переменная задана, иначе из `PATH`.
В Windows можно запускать JAR напрямую: `java -jar target/expense-tracker.jar list`.

Чтобы вызывать команду без `./`, добавьте каталог проекта в `PATH`:

```sh
export PATH="$PWD:$PATH"
expense-tracker --help
```

## Команды

```sh
expense-tracker add --description "Lunch" --amount 20
expense-tracker add --description "Dinner" --amount 10
expense-tracker list
expense-tracker summary
expense-tracker delete --id <UUID-из-list>
expense-tracker summary --month 8
```

ID — полный UUID: он выводится после добавления и в списке расходов.
Дата по умолчанию — сегодня в `AppConfig.TIME_ZONE`.
Дополнительные параметры добавления:

```sh
expense-tracker add --description "Lunch" --amount 20.50 --date 2020-08-06 --merchant "Cafe"
```

`summary` суммирует все записи. `summary --month 8` — только август текущего года
в часовом поясе приложения. Месяц должен быть от 1 до 12.
Суммы вычисляются через `BigDecimal`; знак `$` используется только при выводе,
модель валюту не хранит.

По умолчанию файл хранится в `data/expenses.csv` относительно рабочего каталога.
Для одинакового хранилища при запуске из разных каталогов задайте абсолютный путь:

```sh
export EXPENSE_TRACKER_DIR="$HOME/.expense-tracker"
```

Каталог создаётся автоматически. Несколько одновременных писателей не поддерживаются.
Коды завершения: `0` — успех, `1` — ошибка хранения, `2` — ошибка команды или данных.
