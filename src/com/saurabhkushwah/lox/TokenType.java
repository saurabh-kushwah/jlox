package com.saurabhkushwah.lox;

public enum TokenType {
  // single character token
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, DOT, PLUS, MINUS, STAR, SLASH, SEMICOLON,

  // one or two character token
  BANG, BANG_EQUAL,
  EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL,
  LESS, LESS_EQUAL,

  // literals
  IDENTIFIER, STRING, NUMBER,

  // keywords
  VAR, PRINT, NIL,
  AND, OR, TRUE, FALSE,
  IF, ELSE, FOR, WHILE, BREAK,
  FUN, RETURN, CLASS, SUPER, THIS,

  EOF
}
