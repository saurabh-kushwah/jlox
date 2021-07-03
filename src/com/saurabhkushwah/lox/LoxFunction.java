package com.saurabhkushwah.lox;

import com.saurabhkushwah.lox.Stmt.Function;
import java.util.List;

public class LoxFunction implements LoxCallable {

  private final Stmt.Function declaration;
  private final Environment closure;

  public LoxFunction(Function declaration, Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
  }

  @Override
  public int arity() {
    return declaration.parameters.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);

    for (int i = 0; i < arguments.size(); i++) {
      environment.define(declaration.parameters.get(i).lexeme, arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }

    return null;
  }

  @Override
  public String toString() {
    return String.format("<fn %s>", declaration.name.lexeme);
  }
}
