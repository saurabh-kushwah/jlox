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

/**
 * AstPrinter returns  Reverse Polish Notation(Postfix) representation of AST
 */
public class AstPrinter implements Expr.Visitor<String> {

  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitLiteralExpr(Literal expr) {
    if (expr.value == null) {
      return "nil";
    }
    return expr.value.toString();
  }

  @Override
  public String visitVariableExpr(Variable expr) {
    return null;
  }

  @Override
  public String visitAssignExpr(Assign expr) {
    return null;
  }

  @Override
  public String visitGroupingExpr(Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitCallExpr(Call expr) {
    return null;
  }

  @Override
  public String visitUnaryExpr(Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitBinaryExpr(Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitLogicalExpr(Logical expr) {
    return "";
  }

  @Override
  public String visitFunctionExpr(Function expr) {
    return null;
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder sb = new StringBuilder();

    sb.append("(");
    for (Expr expr : exprs) {
      sb.append(expr.accept(this)).append(" ");
    }

    sb.append(name);
    sb.append(")");
    return sb.toString();
  }
}
