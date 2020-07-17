package ru.ilyafx.gammacases.database.factory;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import ru.ilyafx.gammacases.database.QueryResult;

import java.sql.*;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public abstract class AbstractQueryFactory {

    protected final HikariDataSource dataSource;

    protected QueryResult prepareGet0(String query, Consumer<PreparedStatement> consumer) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            consumer.accept(statement);
            try (
                    ResultSet set = statement.executeQuery()
            ) {
                return new QueryResult(set);
            }
        }
    }

    protected int prepareUpdate0(String query, Consumer<PreparedStatement> consumer) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            consumer.accept(statement);
            return statement.executeUpdate();
        }
    }

    protected int[] prepareBatch0(String query, List<Consumer<PreparedStatement>> consumers) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            for (Consumer<PreparedStatement> consumer : consumers) {
                consumer.accept(statement);
                statement.addBatch();
            }
            return statement.executeBatch();
        }
    }

    protected QueryResult unsafeGet0(String query) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet set = statement.executeQuery(query)
        ) {
            return new QueryResult(set);
        }
    }

    protected int unsafeUpdate0(String query) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()
        ) {
            return statement.executeUpdate(query);
        }
    }

    protected int[] unsafeBatch0(List<String> queries) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()
        ) {
            for (String query : queries)
                statement.addBatch(query);
            return statement.executeBatch();
        }
    }

}
