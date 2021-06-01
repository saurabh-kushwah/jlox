package com.saurabhkushwah;

import static com.saurabhkushwah.TokenType.AND;
import static com.saurabhkushwah.TokenType.BANG;
import static com.saurabhkushwah.TokenType.BANG_EQUAL;
import static com.saurabhkushwah.TokenType.CLASS;
import static com.saurabhkushwah.TokenType.COMMA;
import static com.saurabhkushwah.TokenType.DOT;
import static com.saurabhkushwah.TokenType.ELSE;
import static com.saurabhkushwah.TokenType.EOF;
import static com.saurabhkushwah.TokenType.EQUAL;
import static com.saurabhkushwah.TokenType.EQUAL_EQUAL;
import static com.saurabhkushwah.TokenType.FALSE;
import static com.saurabhkushwah.TokenType.FOR;
import static com.saurabhkushwah.TokenType.FUN;
import static com.saurabhkushwah.TokenType.GREATER;
import static com.saurabhkushwah.TokenType.GREATER_EQUAL;
import static com.saurabhkushwah.TokenType.IDENTIFIER;
import static com.saurabhkushwah.TokenType.IF;
import static com.saurabhkushwah.TokenType.LEFT_BRACE;
import static com.saurabhkushwah.TokenType.LEFT_PAREN;
import static com.saurabhkushwah.TokenType.LESS;
import static com.saurabhkushwah.TokenType.LESS_EQUAL;
import static com.saurabhkushwah.TokenType.MINUS;
import static com.saurabhkushwah.TokenType.NIL;
import static com.saurabhkushwah.TokenType.NUMBER;
import static com.saurabhkushwah.TokenType.OR;
import static com.saurabhkushwah.TokenType.PLUS;
import static com.saurabhkushwah.TokenType.RETURN;
import static com.saurabhkushwah.TokenType.RIGHT_BRACE;
import static com.saurabhkushwah.TokenType.RIGHT_PAREN;
import static com.saurabhkushwah.TokenType.SEMICOLON;
import static com.saurabhkushwah.TokenType.SLASH;
import static com.saurabhkushwah.TokenType.STAR;
import static com.saurabhkushwah.TokenType.STRING;
import static com.saurabhkushwah.TokenType.SUPER;
import static com.saurabhkushwah.TokenType.THIS;
import static com.saurabhkushwah.TokenType.TRUE;
import static com.saurabhkushwah.TokenType.VAR;
import static com.saurabhkushwah.TokenType.WHILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {

  final static Map<String, TokenType> keywords = new HashMap<>();

  static {
    keywords.put("var", VAR);
    keywords.put("nil", NIL);

    keywords.put("if", IF);
    keywords.put("else", ELSE);

    keywords.put("for", FOR);
    keywords.put("while", WHILE);

    keywords.put("true", TRUE);
    keywords.put("false", FALSE);

    keywords.put("and", AND);
    keywords.put("or", OR);

    keywords.put("class", CLASS);
    keywords.put("this", THIS);
    keywords.put("super", SUPER);

    keywords.put("fun", FUN);
    keywords.put("return", RETURN);
  }

  final String source;
  final List<Token> tokens = new ArrayList<>();
  private int start; // start of the token
  private int current; // offset from start
  private int line = 1;

  public Scanner(String source) {
    this.source = source;
  }

  public List<Token> scanTokens() {
    while (!isAtEnd()) {
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private void scanToken() {
    char ch = advance();
    switch (ch) {
      case '(':
        addToken(LEFT_PAREN);
        break;
      case ')':
        addToken(RIGHT_PAREN);
        break;
      case '{':
        addToken(LEFT_BRACE);
        break;
      case '}':
        addToken(RIGHT_BRACE);
        break;
      case ',':
        addToken(COMMA);
        break;
      case '.':
        addToken(DOT);
        break;
      case '-':
        addToken(MINUS);
        break;
      case '+':
        addToken(PLUS);
        break;
      case '*':
        addToken(STAR);
        break;
      case ';':
        addToken(SEMICOLON);
        break;

      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;

      case '/':
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) {
            advance();
          }
        } else if(match('*')){
          while(!isAtEnd()){
            if(peek() == '\n'){
              line++;
            }else if(match('*') && match('/')){
              break;
            }
            advance();
          }
        }else {
          addToken(SLASH);
        }
        break;

      case ' ':
      case '\r':
      case '\t':
        break;
      case '\n':
        line++;
        break;

      case '"':
        string();
        break;

      default:

        if (isDigit(ch)) {
          number();
        } else if (isAlpha(ch)) {
          identifier();
        } else {
          Lox.error(line, "Unexpected character.");
        }
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) {
      advance();
    }

    String text = source.substring(start, current);
    addToken(keywords.getOrDefault(text, IDENTIFIER), text);
  }

  private boolean isAlphaNumeric(char ch) {
    return isAlpha(ch) || isDigit(ch);
  }

  private boolean isAlpha(char ch) {
    return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_';
  }

  private boolean isDigit(char ch) {
    return '0' <= ch && ch <= '9';
  }

  private void number() {
    while (isDigit(peek())) {
      advance();
    }

    if (peek() == '.' && isDigit(peekNext())) {
      advance();

      while (isDigit(peek())) {
        advance();
      }
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') {
        line++;
      }
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    advance(); // consume terminating "

    String str = source.substring(start + 1, current - 1);
    addToken(STRING, str);
  }

  private char advance() {
    return source.charAt(current++);
  }

  private char peek() {
    if (isAtEnd()) {
      return '\0';
    }
    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) {
      return '\0';
    }

    return source.charAt(current + 1);
  }

  private boolean match(char ch) {
    if (isAtEnd()) {
      return false;
    }
    if (source.charAt(current) != ch) {
      return false;
    }

    current++;
    return true;
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String lexeme = source.substring(start, current);
    tokens.add(new Token(type, lexeme, literal, line));
  }
}
