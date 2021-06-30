package com.saurabhkushwah.lox;

import com.saurabhkushwah.lox.Expr.Assign;
import com.saurabhkushwah.lox.Expr.Binary;
import com.saurabhkushwah.lox.Expr.Grouping;
import com.saurabhkushwah.lox.Expr.Literal;
import com.saurabhkushwah.lox.Expr.Unary;
import com.saurabhkushwah.lox.Expr.Variable;
import com.saurabhkushwah.lox.Stmt.Block;
import com.saurabhkushwah.lox.Stmt.Expression;
import com.saurabhkushwah.lox.Stmt.If;
import com.saurabhkushwah.lox.Stmt.Print;
import com.saurabhkushwah.lox.Stmt.Var;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {

  private Environment environment = new Environment();

  public void interpret(List<Stmt> statements) {
    try {
      for (Stmt stmt : statements) {
        execute(stmt);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
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
    return environment.get(expr.name);
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
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

  private Object evaluate(Expr expr) {
    return expr.accept(this);
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

  @Override
  public Void visitExpressionStmt(Expression stmt) {
    evaluate(stmt.expression);
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
  public Void visitPrintStmt(Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
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

  private void executeBlock(List<Stmt> statements, Environment environment) {
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
}
