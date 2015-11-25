// expression test

//import konoha.hack.ByteCode;
//import konoha.hack.AST;
import konoha.syntax.ClassDeclaration;
class Klass {
  int a,b;
  String s;
  int add(){
    return this.a + this.b;
  }
  Klass(int a, int b){
    this.a = a;
    this.b = b;
  }
  Klass(){}
}
// 1. check eval (top level)

Klass c = new Klass(1,2);
c.s = "World";

assert(c.s == "World");
assert(c.add() == 3);

// 2. check in function
function test1(){
  Klass c = new Klass(10, 20);
  return c.add();
}

function test2(){
  Klass c = new Klass();
  c.s = "Hello";
  return c.s;
}

assert(test1() == 30);
assert(test2() == "Hello");
