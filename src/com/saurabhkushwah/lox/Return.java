package com.saurabhkushwah.lox;

public class Return extends RuntimeException {

  final Object value;
  final Token keyword;

  public Return(Token keyword, Object value) {
    super(null, null, false, false);
    this.keyword = keyword;
    this.value = value;
  }
}
