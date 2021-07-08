
## Resolving and Binding

### Static Scope
  - resolution of variable is at runtime, each time variable is accessed
  - but for closure based on other languages implementation may vary
  - before commit bafa445 lox closure behaviour is similar to JavaScript
  - lox tries to mimick behaviour of GO not JavaScript

```
var a = "global";
{
  fun showA() {
    print a;
  }

  showA();
  var a = "block";
  showA();
}
```

Behaviour in other languages
  1. JavaScript: "global", "block"
  2. Go:         "global", "global"
  3. Java:       "redeclaration error"
