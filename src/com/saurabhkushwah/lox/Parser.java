package com.saurabhkushwah.lox;

import static com.saurabhkushwah.lox.TokenType.AND;
import static com.saurabhkushwah.lox.TokenType.BANG;
import static com.saurabhkushwah.lox.TokenType.BANG_EQUAL;
import static com.saurabhkushwah.lox.TokenType.COMMA;
import static com.saurabhkushwah.lox.TokenType.ELSE;
import static com.saurabhkushwah.lox.TokenType.EOF;
import static com.saurabhkushwah.lox.TokenType.EQUAL;
import static com.saurabhkushwah.lox.TokenType.EQUAL_EQUAL;
import static com.saurabhkushwah.lox.TokenType.FALSE;
import static com.saurabhkushwah.lox.TokenType.FOR;
import static com.saurabhkushwah.lox.TokenType.FUN;
import static com.saurabhkushwah.lox.TokenType.GREATER;
import static com.saurabhkushwah.lox.TokenType.GREATER_EQUAL;
import static com.saurabhkushwah.lox.TokenType.IDENTIFIER;
import static com.saurabhkushwah.lox.TokenType.IF;
import static com.saurabhkushwah.lox.TokenType.LEFT_BRACE;
import static com.saurabhkushwah.lox.TokenType.LEFT_PAREN;
import static com.saurabhkushwah.lox.TokenType.LESS;
import static com.saurabhkushwah.lox.TokenType.LESS_EQUAL;
import static com.saurabhkushwah.lox.TokenType.MINUS;
import static com.saurabhkushwah.lox.TokenType.NIL;
import static com.saurabhkushwah.lox.TokenType.NUMBER;
import static com.saurabhkushwah.lox.TokenType.OR;
import static com.saurabhkushwah.lox.TokenType.PLUS;
import static com.saurabhkushwah.lox.TokenType.PRINT;
import static com.saurabhkushwah.lox.TokenType.RETURN;
import static com.saurabhkushwah.lox.TokenType.RIGHT_BRACE;
import static com.saurabhkushwah.lox.TokenType.RIGHT_PAREN;
import static com.saurabhkushwah.lox.TokenType.SEMICOLON;
import static com.saurabhkushwah.lox.TokenType.SLASH;
import static com.saurabhkushwah.lox.TokenType.STAR;
import static com.saurabhkushwah.lox.TokenType.STRING;
import static com.saurabhkushwah.lox.TokenType.TRUE;
import static com.saurabhkushwah.lox.TokenType.VAR;
import static com.saurabhkushwah.lox.TokenType.WHILE;

import java.util.ArrayList;
import java.util.List;

/*
 * program        → declaration* EOF
 * declaration    → varDec
 *                | funDec
 *                | statement
 * varDec         → "var" IDENTIFIER ( "=" expression )? ";" ;
 * funDec         → "fun" function ;
 * function       → IDENTIFIER "(" parameter? ")" block ;
 * parameter      → IDENTIFIER ( "," IDENTIFIER )* ;
 * statement      → exprStmt
 *                | forStmt
 *                | ifStmt
 *                | printStmt
 *                | returnStmt
 *                | whileStmt
 *                | block ;
 * exprStmt       → expression ";" ;
 * forStmt        → "for" "(" ( varDec | exprStmt | ";" ) expression? ";" expression? ")" statement ;
 * ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
 * printStmt      → print expression ";" ;
 * returnStmt     → "return" expression? ";" ;
 * whileStmt      → "while" "(" expression ")" statement ;
 * block          → "{" declaration* "}" ;
 * expression     → assignment ;
 * assignment     → IDENTIFIER "=" assignment
 *                | logic_or ;
 * logic_or       → logic_and ( "or" logic_and )*
 * logic_and      → equality ( "and" equality )*
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term           → factor ( ( "-" | "+" ) factor )* ;
 * factor         → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary
 *                | call ;
 * call           → primary ( "(" argument? ")" )* ;
 * argument       → expression ( "," expression )*
 * primary        → NUMBER | STRING | "true" | "false" | "nil"
 *                | "(" expression ")"
 *                | IDENTIFIER ;
 */

/**
 * Language Notes
 * 1. Support operator chaining similar to python, js for all binary operator.
 * Ex. a == b == c == d, a / b / c / d, a >= b >= c >= d
 * <p>
 * 2. Support chaining of function calls
 * Ex. getCallback(1)(2)
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

      if (match(FUN)) {
        return funDeclaration("function");
      }

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt.Function funDeclaration(String type) {
    Token name = consume(IDENTIFIER, "Expect " + type + " name.");
    consume(LEFT_PAREN, "Expect '(' after " + type + " name.");

    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Cannot have more than 255 parameters");
        }
        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }

    consume(RIGHT_PAREN, "Expect ')' after " + type + " name.");

    consume(LEFT_BRACE, "Expect '{' before " + type + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
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

    if (match(IF)) {
      return ifStatement();
    }

    if (match(WHILE)) {
      return whileStatement();
    }

    if (match(FOR)) {
      return forStatement();
    }

    if (match(RETURN)) {
      return returnStatement();
    }

    return expressionStatement();
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr expr = null;

    if (!check(SEMICOLON)) {
      expr = expression();
    }

    consume(SEMICOLON, "Expect ';' after return value");
    return new Stmt.Return(keyword, expr);
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after for.");

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after condition.");

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after update.");

    Stmt body = statement();

    if (increment != null) {
      body = new Stmt.Block(List.of(body, new Stmt.Expression(increment)));
    }

    if (condition == null) {
      condition = new Expr.Literal(true);
    }

    body = new Stmt.While(condition, body);

    if (initializer != null) {
      body = new Stmt.Block(List.of(initializer, body));
    }

    return body;
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after while.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");

    Stmt body = statement();

    return new Stmt.While(condition, body);
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after if.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;

    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
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
    Expr expr = or();

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

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
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

    return call();
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr finishCall(Expr expr) {
    List<Expr> arguments = new ArrayList<>();

    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments");
        }
        arguments.add(expression());
      } while (match(COMMA));
    }

    Token paren = consume(RIGHT_PAREN, "expect ')'  after arguments.");
    return new Expr.Call(expr, paren, arguments);
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
