package com.polo.boot.mybatis.plus.service;

import com.polo.boot.mybatis.plus.annotation.DataScope;
import com.polo.boot.mybatis.plus.annotation.DataScopeType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class DataScopeContext {
    private static final ThreadLocal<Deque<ScopeDefinition>> HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

    private DataScopeContext() {
    }

    public static void push(DataScope dataScope) {
        if (dataScope == null) {
            return;
        }
        HOLDER.get().push(ScopeDefinition.from(dataScope));
    }

    public static Optional<ScopeDefinition> current() {
        Deque<ScopeDefinition> stack = HOLDER.get();
        return stack.isEmpty() ? Optional.empty() : Optional.ofNullable(stack.peek());
    }

    public static List<ScopeDefinition> snapshot() {
        Deque<ScopeDefinition> stack = HOLDER.get();
        if (stack.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new ArrayList<>(stack));
    }

    public static void restore(List<ScopeDefinition> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            HOLDER.remove();
            return;
        }
        HOLDER.set(new ArrayDeque<>(snapshot));
    }

    public static void pop() {
        Deque<ScopeDefinition> stack = HOLDER.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            HOLDER.remove();
        }
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record ScopeDefinition(DataScopeType type,
                                  String deptColumn,
                                  String userColumn,
                                  String customCondition) {
        static ScopeDefinition from(DataScope dataScope) {
            return new ScopeDefinition(
                    dataScope.type(),
                    dataScope.deptColumn(),
                    dataScope.userColumn(),
                    dataScope.customCondition()
            );
        }
    }
}
