package com.saurabhkushwah.lox;

import com.saurabhkushwah.lox.Expr.Assign;
import com.saurabhkushwah.lox.Expr.Binary;
import com.saurabhkushwah.lox.Expr.Call;
import com.saurabhkushwah.lox.Expr.Grouping;
import com.saurabhkushwah.lox.Expr.Literal;
import com.saurabhkushwah.lox.Expr.Logical;
import com.saurabhkushwah.lox.Expr.Unary;
import com.saurabhkushwah.lox.Expr.Variable;
import com.saurabhkushwah.lox.Stmt.Block;
import com.saurabhkushwah.lox.Stmt.Expression;
import com.saurabhkushwah.lox.Stmt.Function;
import com.saurabhkushwah.lox.Stmt.If;
import com.saurabhkushwah.lox.Stmt.Print;
import com.saurabhkushwah.lox.Stmt.Var;
import com.saurabhkushwah.lox.Stmt.While;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {

  final Environment globals = new Environment();
  final Map<Expr, Integer> locals = new HashMap<>();
  private Environment environment = globals;

  public void interpret(List<Stmt> statements) {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    try {
      for (Stmt stmt : statements) {
        executeStatement(stmt);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  // catches invalid use of keywords
  private void executeStatement(Stmt stmt) {
    try {
      execute(stmt);
    } catch (Break error) {
      Lox.runtimeError(
          new RuntimeError(error.keyword, "Cannot use 'break' outside for/while loop"));
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitVariableExpr(Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Variable expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    }

    return globals.get(name);
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitCallExpr(Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = expr.arguments.stream()
        .map(this::evaluate)
        .collect(Collectors.toList());

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable) callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren,
          String.format("Expect %d arguments but got %d.", function.arity(), arguments.size()));
    }

    return function.call(this, arguments);
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      case BANG:
        return !isTruthy(right);
    }
    return null;
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case PLUS:
        if (left instanceof String || right instanceof String) {
          return stringify(left) + stringify(right);
        } else if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        throw new RuntimeError(expr.operator,
            "Operands must be either 2 numbers or one of them must be string");
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        if ((double) right == 0) {
          throw new RuntimeError(expr.operator, "Division by zero");
        }
        return (double) left / (double) right;

      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case BANG_EQUAL:
        return !isEqual(left, right);
    }

    return null;
  }

  @Override
  public Object visitLogicalExpr(Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) {
        return left;
      }
    } else {
      if (!isTruthy(left)) {
        return left;
      }
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitFunctionExpr(Expr.Function expr) {
    return new LoxFunction(new Function(null, expr.parameters, expr.body),
        environment);
  }

  private String stringify(Object object) {
    if (object == null) {
      return "nil";
    }

    if (object instanceof String) {
      return (String) object;
    }

    if (object instanceof Double) {
      String str = object.toString();
      if (str.endsWith(".0")) {
        str = str.substring(0, str.length() - 2);
      }
      return str;
    }

    return object.toString();
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) {
      return;
    }
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isEqual(Object o1, Object o2) {
    if (o1 == null && o2 == null) {
      return true;
    } else if (o1 == null) {
      return false;
    }

    return o1.equals(o2);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) {
      return;
    }
    throw new RuntimeError(operator, "Operand must be a number");
  }

  private boolean isTruthy(Object object) {
    if (object == null) {
      return false;
    } else if (object instanceof Boolean) {
      return (boolean) object;
    }

    return true;
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Void visitExpressionStmt(Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Function stmt) {
    LoxFunction loxFunction = new LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, loxFunction);
    return null;
  }

  @Override
  public Void visitIfStmt(If stmt) {
    Object value = evaluate(stmt.condition);
    if (isTruthy(value)) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitBreakStmt(Stmt.Break stmt) {
    throw new Break(stmt.keyword);
  }

  @Override
  public Void visitPrintStmt(Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitWhileStmt(While stmt) {
    try {
      while (isTruthy(evaluate(stmt.condition))) {
        execute(stmt.body);
      }
    } catch (Break ignored) {
    }

    return null;
  }

  @Override
  public Object visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }

    throw new Return(stmt.keyword, value);
  }

  @Override
  public Void visitVarStmt(Var stmt) {
    Object value = null;

    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitBlockStmt(Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  public void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;

    try {
      this.environment = environment;
      for (Stmt stmt : statements) {
        execute(stmt);
      }
    } finally {
      this.environment = previous;
    }
  }

  // tells interpreter current scope - scope at variable defined
  public void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }
}
