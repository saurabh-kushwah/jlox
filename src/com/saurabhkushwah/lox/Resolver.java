package com.saurabhkushwah.lox;

import com.saurabhkushwah.lox.Expr.Assign;
import com.saurabhkushwah.lox.Expr.Binary;
import com.saurabhkushwah.lox.Expr.Call;
import com.saurabhkushwah.lox.Expr.Function;
import com.saurabhkushwah.lox.Expr.Grouping;
import com.saurabhkushwah.lox.Expr.Literal;
import com.saurabhkushwah.lox.Expr.Logical;
import com.saurabhkushwah.lox.Expr.Unary;
import com.saurabhkushwah.lox.Expr.Variable;
import com.saurabhkushwah.lox.Stmt.Block;
import com.saurabhkushwah.lox.Stmt.Expression;
import com.saurabhkushwah.lox.Stmt.If;
import com.saurabhkushwah.lox.Stmt.Print;
import com.saurabhkushwah.lox.Stmt.Var;
import com.saurabhkushwah.lox.Stmt.While;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Object> {

  private enum FunctionType {
    FUNCTION,
    NONE
  }

  private final Interpreter interpreter;

  // String, Boolean -> token, isDefined
  private final Stack<HashMap<String, Boolean>> scopes = new Stack<>();

  private FunctionType currentFunction = FunctionType.NONE;

  public Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return null;
  }

  @Override
  public Void visitVariableExpr(Variable expr) {
    // case var a = a;
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }

    // global variable, do nothing
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Object visitCallExpr(Call expr) {
    resolve(expr.callee);
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }
    return null;
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Object visitLogicalExpr(Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Object visitFunctionExpr(Function expr) {
    return null;
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  @Override
  public Object visitExpressionStmt(Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Object visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Object visitIfStmt(If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) {
      resolve(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitBreakStmt(Stmt.Break stmt) {
    return null;
  }

  @Override
  public Object visitPrintStmt(Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Object visitWhileStmt(While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Object visitReturnStmt(Stmt.Return stmt) {
    if(currentFunction == FunctionType.NONE){
      Lox.error(stmt.keyword, "Can't return from top-level code");
    }

    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Object visitVarStmt(Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Object visitBlockStmt(Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) {
      return;
    }

    Map<String, Boolean> scope = scopes.peek();

    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Already variable with the same name in this scope.");
    }

    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    // resolving only for local variables not global
    if (scopes.isEmpty()) {
      return;
    }

    Map<String, Boolean> scope = this.scopes.peek();
    scope.put(name.lexeme, true);
  }

  private void resolveFunction(Stmt.Function stmt, FunctionType functionType) {
    FunctionType enclosingType = functionType;
    currentFunction = functionType;

    beginScope();
    for (Token param : stmt.parameters) {
      declare(param);
      define(param);
    }
    resolve(stmt.body);
    endScope();
  }

  private void beginScope() {
    scopes.push(new HashMap<>());
  }

  public void resolve(List<Stmt> statements) {
    for (Stmt stmt : statements) {
      resolve(stmt);
    }
  }

  private void endScope() {
    scopes.pop();
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

}
