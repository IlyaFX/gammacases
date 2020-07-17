package ru.ilyafx.gammacases.database.factory;

import com.zaxxer.hikari.HikariDataSource;
import ru.ilyafx.gammacases.database.QueryResult;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class AsyncQueryFactory extends AbstractQueryFactory {

    private final Consumer<Throwable> onCatch;
    private final Executor executor;

    public AsyncQueryFactory(HikariDataSource dataSource, Consumer<Throwable> onCatch, Executor executor) {
        super(dataSource);
        this.onCatch = onCatch;
        this.executor = executor;
    }

    public CompletableFuture<QueryResult> prepareGet(String query, Consumer<PreparedStatement> consumer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return prepareGet0(query, consumer);
            } catch (SQLException exc) {
                onCatch.accept(exc);
                return null;
            }
        }, executor);
    }

    public CompletableFuture<Integer> prepareUpdate(String query, Consumer<PreparedStatement> consumer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return prepareUpdate0(query, consumer);
            } catch (SQLException exc) {
                onCatch.accept(exc);
                return 0;
            }
        }, executor);
    }

    public CompletableFuture<int[]> prepareBatch(String query, List<Consumer<PreparedStatement>> consumers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return prepareBatch0(query, consumers);
            } catch (SQLException exc) {
                onCatch.accept(exc);
                return new int[0];
            }
        }, executor);
    }

    public CompletableFuture<QueryResult> unsafeGet(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return unsafeGet0(query);
            } catch (SQLException exc) {
                onCatch.accept(exc);
                return null;
            }
        }, executor);
    }

    public CompletableFuture<Integer> unsafeUpdate(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return unsafeUpdate0(query);
            } catch (SQLException exc) {
                onCatch.accept(exc);
                return 0;
            }
        }, executor);
    }

    public CompletableFuture<int[]> unsafeBatch(List<String> query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return unsafeBatch0(query);
            } catch (SQLException exc) {
                onCatch.accept(exc);
                return new int[0];
            }
        }, executor);
    }

}
