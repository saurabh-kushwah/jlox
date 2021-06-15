package com.saurabhkushwah.lox;

import static com.saurabhkushwah.lox.TokenType.BANG;
import static com.saurabhkushwah.lox.TokenType.BANG_EQUAL;
import static com.saurabhkushwah.lox.TokenType.EOF;
import static com.saurabhkushwah.lox.TokenType.EQUAL;
import static com.saurabhkushwah.lox.TokenType.EQUAL_EQUAL;
import static com.saurabhkushwah.lox.TokenType.FALSE;
import static com.saurabhkushwah.lox.TokenType.GREATER;
import static com.saurabhkushwah.lox.TokenType.GREATER_EQUAL;
import static com.saurabhkushwah.lox.TokenType.IDENTIFIER;
import static com.saurabhkushwah.lox.TokenType.LEFT_BRACE;
import static com.saurabhkushwah.lox.TokenType.LEFT_PAREN;
import static com.saurabhkushwah.lox.TokenType.LESS;
import static com.saurabhkushwah.lox.TokenType.LESS_EQUAL;
import static com.saurabhkushwah.lox.TokenType.MINUS;
import static com.saurabhkushwah.lox.TokenType.NIL;
import static com.saurabhkushwah.lox.TokenType.NUMBER;
import static com.saurabhkushwah.lox.TokenType.PLUS;
import static com.saurabhkushwah.lox.TokenType.PRINT;
import static com.saurabhkushwah.lox.TokenType.RIGHT_BRACE;
import static com.saurabhkushwah.lox.TokenType.RIGHT_PAREN;
import static com.saurabhkushwah.lox.TokenType.SEMICOLON;
import static com.saurabhkushwah.lox.TokenType.SLASH;
import static com.saurabhkushwah.lox.TokenType.STAR;
import static com.saurabhkushwah.lox.TokenType.STRING;
import static com.saurabhkushwah.lox.TokenType.TRUE;
import static com.saurabhkushwah.lox.TokenType.VAR;

import java.util.ArrayList;
import java.util.List;

/*
 * program        → declaration* EOF
 * declaration    → varDec
 *                | statement
 * varDec         → "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement      → exprStmt
 *                | printStmt ;
 *                | block ;
 * exprStmt       → expression ";" ;
 * printStmt      → print expression ";" ;
 * block          → "{" declaration* "}" ;
 * expression     → assignment ;
 * assignment     → IDENTIFIER "=" assignment
 *                | equality ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary
 *                | primary ;
 * primary        → NUMBER | STRING | "true" | "false" | "nil"
 *                | "(" expression ")"
 *                | IDENTIFIER ;
 */

/**
 * Language Notes
 * 1. Support operator chaining similar to python, js for all binary operator.
 * Ex. a == b == c == d, a / b / c / d, a >= b >= c >= d
 */
public class Parser {

  private final List<Token> tokens;
  private int current;
  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();

    try {
      while (!isAtEnd()) {
        statements.add(declaration());
      }
    } catch (ParseError error) {
      return null;
    }

    return statements;
  }

  private Stmt declaration() {
    try {
      if (match(VAR)) {
        return varDeclaration();
      }

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declarations");
    return new Stmt.Var(name, initializer);
  }

  private Stmt statement() {
    if (match(PRINT)) {
      return printStatement();
    }

    if (match(LEFT_BRACE)) {
      return new Stmt.Block(block());
    }

    return expressionStatement();
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' at the end of block.");
    return statements;
  }

  private Stmt printStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ; after value.");
    return new Stmt.Print(expr);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ; after expression.");
    return new Stmt.Expression(expr);
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    Expr expr = equality();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(PLUS, MINUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(STAR, SLASH)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    if (match(TRUE)) {
      return new Expr.Literal(true);
    }

    if (match(FALSE)) {
      return new Expr.Literal(false);
    }

    if (match(NIL)) {
      return new Expr.Literal(null);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression");
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    }

    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  /**
   * discards token that causes error until it encounters new statement
   * allows to prevent cascading error
   * now parse next statement
   * <p>
   * Reason
   * 1. To prevent parser crashing the interpreter
   * 2. Continue executing statement even if one statement has error like most scripting languages
   */
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) {
        return;
      }

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }
    return previous();
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    }
    return peek().type == type;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private static class ParseError extends RuntimeException {

  }
}
