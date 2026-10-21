package repository;

import model.Expense;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Хранилище расходов.
 *
 * <p>Изменения сохраняются только при вызове {@link #save(Expense)}.
 * Изменение переданного или полученного объекта Expense, а также списка
 * из findAll(), само по себе не изменяет содержимое хранилища.
 */
public interface ExpenseRepository {

    /**
     * Сохраняет расход: добавляет новый или заменяет существующий
     * с тем же идентификатором.
     *
     * @param expense сохраняемый расход
     * @throws NullPointerException если expense равен null
     */
    void save(Expense expense);

    /**
     * Находит расход по идентификатору.
     *
     * @param id идентификатор расхода
     * @return найденный расход или пустой Optional
     * @throws NullPointerException если id равен null
     */
    Optional<Expense> findById(UUID id);

    /**
     * Возвращает все расходы. Порядок элементов не определён.
     *
     * @return список расходов или пустой список
     */
    List<Expense> findAll();

    /**
     * Удаляет расход по идентификатору.
     *
     * @param id идентификатор расхода
     * @return true, если расход удалён; false, если он не найден
     * @throws NullPointerException если id равен null
     */
    boolean deleteById(UUID id);
}