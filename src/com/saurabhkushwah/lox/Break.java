package com.saurabhkushwah.lox;

public class Break extends RuntimeException {

  final Token keyword;

  public Break(Token keyword) {
    super(null, null, false, false);
    this.keyword = keyword;
  }
}
