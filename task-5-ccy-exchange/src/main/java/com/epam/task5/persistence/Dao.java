package com.epam.task5.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface Dao<T, ID> {

    boolean exists(ID id);

    Optional<T> get(ID id);

    List<T> getAll();

    T save(T t);

    Collection<T> saveAll(Collection<T> c);

    int delete(T t);

}
