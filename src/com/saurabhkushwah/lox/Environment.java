package com.saurabhkushwah.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {

  private final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  public Environment() {
    enclosing = null;
  }

  public Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  // var similar to JS
  public void define(String name, Object value) {
    values.put(name, value);
  }

  public Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    if (enclosing != null) {
      return enclosing.get(name);
    }
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  public Object getAt(Integer distance, String lexeme) {
    return ancestor(distance).values.get(lexeme);
  }

  public void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  private Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = (environment != null) ? environment.enclosing : null;
    }

    return environment;
  }

  public void assignAt(Integer distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value);
  }

}
