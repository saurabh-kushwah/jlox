package com.saurabhkushwah.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

  private static boolean hadError;
  private static boolean hadRuntimeError;

  private static final Interpreter interpreter = new Interpreter();

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.err.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    if (hadError) {
      System.exit(65);
    } else if (hadRuntimeError) {
      System.exit(70);
    }
  }

  private static void runPrompt() throws IOException {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

    while (true) {
      System.out.print("> ");
      String line = input.readLine();
      if (line == null) {
        continue;
      }
      run(line);
      hadError = false;
    }
  }

  private static void run(String str) {
    Scanner scan = new Scanner(str);
    List<Token> tokens = scan.scanTokens();

    Parser parser = new Parser(tokens);
    Expr expr = parser.parse();

    if (hadError) {
      return;
    }

    interpreter.interpret(expr);
//    System.out.println(new AstPrinter().print(expr));
  }

  public static void error(int line, String message) {
    report(line, "", message);
  }

  public static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  private static void report(int line, String where, String message) {
    System.err.printf("[Line %d] Error %s: %s\n", line, where, message);
    hadError = true;
  }

  public static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + "\n[Line " + error.token.line + "]");
    hadRuntimeError = true;
  }
}
