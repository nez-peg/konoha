// Global,Local
// written by kimio kuramitsu

var i1 = 1;
//var i2 :int;
var i3 :int = 1;
var i4;
assert(i1 == 1);
//assert(i2 == 0);
assert(i3 == 1);
assert(i4 == null);

boolean TestFunc() {
 var i1 = 1;
 //var i2:int;
 var i3 :int = 1;
 var i4;
 assert(i1 == 1);
 //assert(i2 == 0);
 assert(i3 == 1);
 assert(i4 == null);
 return true;
}

assert(TestFunc());
