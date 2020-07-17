package ru.ilyafx.gammacases.database.factory;

import com.zaxxer.hikari.HikariDataSource;
import ru.ilyafx.gammacases.database.QueryResult;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class SyncQueryFactory extends AbstractQueryFactory {

    private final Consumer<Throwable> onCatch;

    public SyncQueryFactory(HikariDataSource dataSource, Consumer<Throwable> onCatch) {
        super(dataSource);
        this.onCatch = onCatch;
    }

    public QueryResult prepareGet(String query, Consumer<PreparedStatement> consumer) {
        try {
            return prepareGet0(query, consumer);
        } catch (SQLException exc) {
            onCatch.accept(exc);
            return null;
        }
    }

    public int prepareUpdate(String query, Consumer<PreparedStatement> consumer) {
        try {
            return prepareUpdate0(query, consumer);
        } catch (SQLException exc) {
            onCatch.accept(exc);
            return 0;
        }
    }

    public int[] prepareBatch(String query, List<Consumer<PreparedStatement>> consumers) {
        try {
            return prepareBatch0(query, consumers);
        } catch (SQLException exc) {
            onCatch.accept(exc);
            return new int[0];
        }
    }

    public QueryResult unsafeGet(String query) {
        try {
            return unsafeGet0(query);
        } catch (SQLException exc) {
            onCatch.accept(exc);
            return null;
        }
    }

    public int unsafeUpdate(String query) {
        try {
            return unsafeUpdate0(query);
        } catch (SQLException exc) {
            onCatch.accept(exc);
            return 0;
        }
    }

    public int[] unsafeBatch(List<String> query) {
        try {
            return unsafeBatch0(query);
        } catch (SQLException exc) {
            onCatch.accept(exc);
            return new int[0];
        }
    }

}
